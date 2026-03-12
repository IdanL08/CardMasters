package com.example.cardmasters.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.cardmasters.GameActivity;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
    public static void createPendingMatch(
            String player1Id,
            OnSuccessListener<DocumentReference> onSuccess,
            OnFailureListener onFailure
    ) {
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("player1Id", player1Id);
        matchData.put("player2Id", null);
        matchData.put("status", "PENDING");
        matchData.put("turnNumber", 0);
        matchData.put("startingPlayer", null);
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
        searchForMatchRecursive(playerId, listener, 0);
    }

    // --- 2. RECURSIVE SEARCH LOGIC ---
    private static void searchForMatchRecursive(String playerId, MatchmakingListener listener, int attempt) {
        if (attempt >= MAX_MATCHMAKING_RETRIES) {
            Log.e(TAG, "Matchmaking timed out after " + MAX_MATCHMAKING_RETRIES + " attempts.");
            listener.onFailure(new Exception("Matchmaking timed out. No available matches found."));
            return;
        }

        Log.d(TAG, "Searching for match, attempt #" + (attempt + 1));

        Query pendingMatchesQuery = getMatchesCollection()
                .whereEqualTo("status", "PENDING")
                .whereEqualTo("player2Id", null)
                .whereNotEqualTo("player1Id", playerId)
                .limit(1);

        pendingMatchesQuery.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();

                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    DocumentReference matchRef = querySnapshot.getDocuments().get(0).getReference();
                    attemptJoinMatch(matchRef, playerId, listener, attempt);
                } else {
                    createPendingMatch(playerId,
                            docRef -> listener.onMatchCreated(docRef.getId()),
                            listener::onFailure
                    );
                }
            } else {
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

            if (!matchSnapshot.exists()) {
                throw new FirebaseFirestoreException("Match deleted", FirebaseFirestoreException.Code.NOT_FOUND);
            }

            if (matchSnapshot.getString("player2Id") == null && "PENDING".equals(matchSnapshot.getString("status"))) {
                String player1Id = matchSnapshot.getString("player1Id");
                String startingPlayerId = new Random().nextBoolean() ? player1Id : joiningPlayerId;

                transaction.update(matchRef, "player2Id", joiningPlayerId);
                transaction.update(matchRef, "status", "READY");
                transaction.update(matchRef, "lastUpdated", System.currentTimeMillis());
                transaction.update(matchRef, "startingPlayer", startingPlayerId);

                return matchRef.getId();
            } else {
                throw new FirebaseFirestoreException("Match claimed", FirebaseFirestoreException.Code.ABORTED);
            }
        }).addOnSuccessListener(matchId -> {
            Log.d(TAG, "Successfully joined match: " + matchId);
            listener.onMatchFound(matchId);

        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException &&
                    (((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.ABORTED ||
                            ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.NOT_FOUND)) {

                Log.w(TAG, "Match was claimed or deleted. Retrying search...");
                searchForMatchRecursive(joiningPlayerId, listener, currentAttempt + 1);
            } else {
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

    public static void checkIfIStart(OnStartingPlayerResult callback,String matchId) {
        if (matchId == null || matchId.isEmpty()) {
            Log.e(TAG, "checkIfIStart: matchId is null! Cannot check starter.");
            return;
        }

        String myEmail = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;

        getMatchDocument(matchId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String starter = snapshot.getString("startingPlayer");
                boolean amIStarter = myEmail != null && myEmail.equals(starter);
                callback.onResult(amIStarter);
            } else {
                Log.e(TAG, "Match document does not exist.");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch match for starter check", e);
        });
    }

    public static void finishMatch(String matchId, String winnerId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "COMPLETED");
        updates.put("winnerId", winnerId);
        updates.put("lastUpdated", System.currentTimeMillis());

        getMatchesCollection().document(matchId)
                .update(updates)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public interface OnStartingPlayerResult {
        void onResult(boolean isStartingPlayer);
    }

    // --- מחיקה עיוורת (רגילה) ---
    public static void deleteMatch(String matchId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        getMatchesCollection().document(matchId)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // --- מחיקה חכמה לטיימר (מונע חדרי רפאים!) ---
    public static void abortPendingMatchSafely(String matchId, Runnable onDeleted, Runnable onAlreadyJoined) {
        DocumentReference matchRef = getMatchDocument(matchId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(matchRef);

            // בודק שהחדר עדיין על PENDING (כלומר, השחקן השני עדיין לא תפס אותו)
            if (snapshot.exists() && "PENDING".equals(snapshot.getString("status"))) {
                transaction.delete(matchRef);
                return true; // אישור שהחדר נמחק
            }
            return false; // מישהו בדיוק הצטרף! אנחנו לא מוחקים.

        }).addOnSuccessListener(deleted -> {
            if (Boolean.TRUE.equals(deleted)) {
                if (onDeleted != null) onDeleted.run();
            } else {
                if (onAlreadyJoined != null) onAlreadyJoined.run();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Transaction failed in abortPendingMatchSafely", e);
            if (onDeleted != null) onDeleted.run(); // במקרה של שגיאה עדיף למחוק/לחפש מחדש
        });
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
                            listener.onStatusChange(snapshot.getData());
                        } else {
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

    public static Task<DocumentSnapshot> getMatch(String matchId) {
        return getMatchDocument(matchId).get();
    }

    /*public static void updateMatchAfterTurn(
            String matchId,
            String nextPlayerId
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentPlayer", nextPlayerId);
        updates.put("turnNumber", FieldValue.increment(1));
        updates.put("lastUpdated", System.currentTimeMillis());

        getMatchDocument(matchId).update(updates)
                .addOnFailureListener(e -> Log.e(TAG, "Failed updating match: ", e));
    }*/

    public static void declareWinner(String matchId, String winnerId) {
        getMatchDocument(matchId)
                .update("winnerId", winnerId)
                .addOnSuccessListener(a -> Log.d(TAG, "Winner set: " + winnerId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to set winner", e));
    }

    public static void deleteMatch(String matchId) {
        getTurnsCollection(matchId)
                .get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        doc.getReference().delete();
                    }
                    getMatchDocument(matchId).delete();
                });
    }

    /*public interface UsernameCallback {
        void onUsernameLoaded(String username);
        void onError(Exception e);
    }*/

    /*public static void getUsername(UsernameCallback callback) {
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
    }*/

    public static FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

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
                    UserPrefsUtils.saveUsername(context, newUsername);
                    if (onSuccess != null) onSuccess.run();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update username", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Username update error", e);
                });
    }

    public static void updatePlayerRating( long pointsDelta) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user= getCurrentUser();

        if (user==null) return;

        db.collection("users").document(user.getUid())
                .update("rating", FieldValue.increment(pointsDelta))
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Rating updated by " + pointsDelta);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error updating rating", e);
                });
    }

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

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.reauthenticate(credential).addOnSuccessListener(aVoid -> {
            String uid = user.getUid();

            db.collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener(unused -> {
                        user.delete().addOnSuccessListener(aVoid1 -> {
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