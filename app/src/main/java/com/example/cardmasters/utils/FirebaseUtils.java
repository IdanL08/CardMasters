package com.example.cardmasters.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.cardmasters.model.dto.PlayedTurnDTO;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseUtils {

    private static final String TAG = "FirebaseUtils";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    private static final int MAX_MATCHMAKING_RETRIES = 5;

    // --------------------------------------------------------
    //   MATCH REFERENCES
    // --------------------------------------------------------
    private static CollectionReference getMatchesCollection() {
        return db.collection("matches");
    }

    private static DocumentReference getMatchDocument(String matchId) {
        return getMatchesCollection().document(matchId);
    }

    private static CollectionReference getTurnsCollection(String matchId) {
        return getMatchDocument(matchId).collection("turns");
    }


    // --------------------------------------------------------
    //   1) CREATE MATCH  (Start of the game loop)
    // --------------------------------------------------------
    // Called once when two players join a match.
    // Creates a match document with metadata.
    public static void createPendingMatch(
            String player1Id,
            OnSuccessListener<DocumentReference> onSuccess,
            OnFailureListener onFailure
    ) {
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("player1Id", player1Id);
        matchData.put("player2Id", null);           // Player 2 is unknown/pending
        matchData.put("status", "PENDING");         // New Status Field
        matchData.put("turnNumber", 0);
        matchData.put("currentPlayer", player1Id);
        matchData.put("winnerId", null);
        matchData.put("createdAt", System.currentTimeMillis());
        matchData.put("lastUpdated", System.currentTimeMillis());

        getMatchesCollection()
                .add(matchData)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }


    // --- 1. PUBLIC ENTRY POINT ---
    public static void searchForMatch(String playerId, MatchmakingListener listener) {
        // Start the search process with 0 retries
        searchForMatchRecursive(playerId, listener, 0);
    }

    // --- 2. RECURSIVE SEARCH LOGIC ---
    private static void searchForMatchRecursive(String playerId, MatchmakingListener listener, int attempt) {
        // Stop if we've tried too many times
        if (attempt >= MAX_MATCHMAKING_RETRIES) {
            Log.e(TAG, "Matchmaking timed out after " + MAX_MATCHMAKING_RETRIES + " attempts.");
            listener.onFailure(new Exception("Matchmaking timed out. No available matches found."));
            return;
        }

        Log.d(TAG, "Searching for match, attempt #" + (attempt + 1));

        // Query for a match that is PENDING, has no player 2, and wasn't created by us
        Query pendingMatchesQuery = getMatchesCollection()
                .whereEqualTo("status", "PENDING")
                .whereEqualTo("player2Id", null)
                .whereNotEqualTo("player1Id", playerId)
                .limit(1);

        pendingMatchesQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    // Found a potential match, let's try to join it.
                    DocumentReference matchRef = querySnapshot.getDocuments().get(0).getReference();
                    attemptJoinMatch(matchRef, playerId, listener, attempt);
                } else {
                    // No pending matches found. We will create our own (terminates recursion).
                    createPendingMatch(playerId,
                            docRef -> listener.onMatchCreated(docRef.getId()),
                            listener::onFailure
                    );
                }
            } else {
                // General query failure (e.g., network error)
                listener.onFailure(task.getException());
            }
        });
    }

    // --- 3. TRANSACTIONAL JOIN LOGIC ---
    public static void attemptJoinMatch(
            DocumentReference matchRef,
            String joiningPlayerId,
            MatchmakingListener listener,
            int currentAttempt) {

        db.runTransaction(transaction -> {
            DocumentSnapshot matchSnapshot = transaction.get(matchRef);

            // Check if the match is still available to be joined
            if (matchSnapshot.exists()
                    && matchSnapshot.getString("player2Id") == null
                    && "PENDING".equals(matchSnapshot.getString("status"))) {

                // It's available! Claim the spot.
                transaction.update(matchRef, "player2Id", joiningPlayerId);
                transaction.update(matchRef, "status", "READY");
                transaction.update(matchRef, "lastUpdated", System.currentTimeMillis());

                // Return the match ID on success
                return matchRef.getId();

            } else {
                // The match was taken by someone else or is no longer pending.
                // We throw an ABORTED code exception to signal failure/retry.
                throw new FirebaseFirestoreException("Match claimed", FirebaseFirestoreException.Code.ABORTED);
            }
        }).addOnSuccessListener(matchId -> {
            // Success! The transaction completed and we joined the match.
            Log.d(TAG, "Successfully joined match: " + matchId);
            listener.onMatchFound(matchId);

        }).addOnFailureListener(e -> {
            // --- FAILURE HANDLER ---

            // Check if the failure was because the transaction was aborted (our desired retry case)
            if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.ABORTED) {

                // This means the match was claimed by someone else (race condition).
                Log.w(TAG, "Match was claimed by another player. Retrying search...");

                // The correct retry logic: search again from the start with incremented attempt count.
                searchForMatchRecursive(joiningPlayerId, listener, currentAttempt + 1);

            } else {
                // This was a different, more serious error (network, permissions, etc.).
                Log.e(TAG, "Transaction failed with unexpected error.", e);
                listener.onFailure(e);
            }
        });
    }

    public interface MatchmakingListener {
        void onMatchFound(String matchId);
        void onMatchCreated(String matchId);
        void onFailure(Exception e);
    }

    public static void finishMatch(String matchId, String winnerId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "COMPLETED");
        updates.put("winnerId", winnerId); // Null if a draw
        updates.put("lastUpdated", System.currentTimeMillis());

        getMatchesCollection().document(matchId)
                .update(updates)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public static void deleteMatch(String matchId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        getMatchesCollection().document(matchId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public static ListenerRegistration listenForMatchStatus(
            String matchId,
            OnMatchStatusChangeListener listener
    ) {
        return getMatchesCollection().document(matchId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Match status listener error.", error);
                        listener.onError(error);
                        return;
                    }

                    if (snapshot != null) {
                        if (snapshot.exists()) {
                            // Match still exists (e.g., status changed to COMPLETED)
                            listener.onStatusChange(snapshot.getData());
                        } else {
                            // Match was deleted by the server or the winning client
                            listener.onMatchDeleted();
                        }
                    }
                });
    }


    public interface OnMatchStatusChangeListener {
        void onStatusChange(Map<String, Object> matchData);
        void onMatchDeleted();
        void onError(Exception e);
    }


    // --------------------------------------------------------
    //   2) SUBMIT TURN  (Player performs an action)
    // --------------------------------------------------------
    // This adds a turn document inside   /matches/{id}/turns/
    // DOES NOT CHANGE currentPlayer or turnNumber — that’s part of the loop logic.
    public static void submitTurn(
            String matchId,
            PlayedTurnDTO turn,
            Consumer<Boolean> callback
    ) {
        getTurnsCollection(matchId)
                .add(turn.toMap())
                .addOnSuccessListener(doc -> callback.accept(true))
                .addOnFailureListener(e -> callback.accept(false));
    }


    // --------------------------------------------------------
    //   3) LISTEN FOR NEW TURNS (Core of turn-based gameplay)
    // --------------------------------------------------------
    // Called when the game is running.
    // Every time /turns/ gets a new document → callback fires.
    //
    // This is how you know:
    //   - the opponent played
    //   - turnNumber should increase
    //   - UI should update
    public static ListenerRegistration listenForTurns(
            String matchId,
            TurnListener callback
    ) {
        return getTurnsCollection(matchId)
                .orderBy("submittedAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Log.e(TAG, "Turn listener error: ", error);
                        return;
                    }

                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            String turnId = change.getDocument().getId();
                            Map<String, Object> turnData = change.getDocument().getData();
                            callback.onTurnReceived(turnId, turnData);
                        }
                    }
                });
    }

    public interface TurnListener {
        void onTurnReceived(String turnId, Map<String, Object> turnData);
    }


    // --------------------------------------------------------
    //   4) GET MATCH (Read current match state one-time)
    // --------------------------------------------------------
    public static Task<DocumentSnapshot> getMatch(String matchId) {
        return getMatchDocument(matchId).get();
    }


    // --------------------------------------------------------
    //   5) UPDATE MATCH AFTER A TURN
    // --------------------------------------------------------
    // This is what advances the game loop:
    //   - turnNumber++
    //   - currentPlayer switches
    //   - lastUpdated refreshes (for timeouts)
    //
    // This must be called AFTER submitTurn().
    public static void updateMatchAfterTurn(
            String matchId,
            String nextPlayerId
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentPlayer", nextPlayerId);
        updates.put("turnNumber", FieldValue.increment(1));// increment by 1 מגדיל את מספר התור בתיאום עם הפיירבייס
        updates.put("lastUpdated", System.currentTimeMillis());

        getMatchDocument(matchId).update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Failed updating match: ", e));
    }


    // --------------------------------------------------------
    //   6) SET WINNER (End of the game loop)
    // --------------------------------------------------------
    public static void declareWinner(String matchId, String winnerId) {
        getMatchDocument(matchId)
                .update("winnerId", winnerId)
                .addOnSuccessListener(a -> Log.d(TAG, "Winner set: " + winnerId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to set winner", e));
    }


    // --------------------------------------------------------
    //   7) DELETE MATCH + ALL TURNS (Cleanup)
    // --------------------------------------------------------
    // Call this after match ends.
    public static void deleteMatch(String matchId) {

        // delete turns collection first
        getTurnsCollection(matchId)
                .get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // delete match
                    getMatchDocument(matchId).delete();
                });
    }
    public interface UsernameCallback {
        void onUsernameLoaded(String username);
        void onError(Exception e);
    }

    // Fetch username from Firestore
    public static void getUsername(UsernameCallback callback) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            callback.onError(new Exception("User not logged in"));
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String username = snapshot.getString("username");
                        callback.onUsernameLoaded(username);
                    } else {
                        callback.onError(new Exception("User doc not found"));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    // -------------------------------------------------------------
    // ★ Update username in Firestore + locally
    // -------------------------------------------------------------
    public static void updateUsername(Context context, String newUsername, Runnable onSuccess) {

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(user.getUid())
                .update("username", newUsername)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Username updated", Toast.LENGTH_SHORT).show();

                    // Save locally
                    UserPrefsUtils.saveUsername(context, newUsername);

                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update username", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Username update error", e);
                });
    }


    // -------------------------------------------------------------
    // ★ Re-authenticate + delete Firestore + delete Auth + clear prefs
    // -------------------------------------------------------------
    public static void reauthAndDelete(Context context, Runnable onSuccess) {

        String email = UserPrefsUtils.getEmail(context);
        String password = UserPrefsUtils.getPassword(context);

        if (email == null || password == null) {
            Toast.makeText(context, "Missing saved credentials", Toast.LENGTH_SHORT).show();
            UserPrefsUtils.clear(context);
            if (onSuccess != null) onSuccess.run();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(context, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        // יוצר מין כרטיס כניסה בעזרת עצמים של הפיירבייס
        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential).addOnSuccessListener(aVoid -> {

            String uid = user.getUid();

            // 1) delete from Firestore
            db.collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener(unused -> {

                        // 2) delete Auth user
                        user.delete().addOnSuccessListener(aVoid1 -> {

                            // 3) clear shared prefs
                            UserPrefsUtils.clear(context);

                            Toast.makeText(context, "User deleted successfully", Toast.LENGTH_SHORT).show();

                            if (onSuccess != null) onSuccess.run();

                        }).addOnFailureListener(e -> {
                            Toast.makeText(context, "Failed to delete Auth user", Toast.LENGTH_SHORT).show();
                        });

                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to delete Firestore user", Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Re-authentication failed", Toast.LENGTH_SHORT).show();
        });
    }

}


