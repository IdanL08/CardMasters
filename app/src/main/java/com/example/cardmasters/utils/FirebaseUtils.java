package com.example.cardmasters.utils;

import android.util.Log;

import com.example.cardmasters.model.dto.PlayedTurnDTO;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseUtils {

    private static final String TAG = "FirebaseUtils";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();

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
    public static void createMatch(
            String player1Id,
            String player2Id,
            OnSuccessListener<DocumentReference> onSuccess,
            OnFailureListener onFailure
    ) {
        Map<String, Object> matchData = new HashMap<>();
        matchData.put("player1Id", player1Id);
        matchData.put("player2Id", player2Id);
        matchData.put("turnNumber", 0);
        matchData.put("currentPlayer", player1Id);   // first turn
        matchData.put("winnerId", null);
        matchData.put("createdAt", System.currentTimeMillis());
        matchData.put("lastUpdated", System.currentTimeMillis());

        getMatchesCollection()
                .add(matchData)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
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
}
