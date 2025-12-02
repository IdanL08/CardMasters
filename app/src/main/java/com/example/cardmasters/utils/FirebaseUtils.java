package com.example.cardmasters.utils;


import android.util.Log;

import com.example.cardmasters.model.dto.PlayedTurnDTO;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class FirebaseUtils {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // -----------------------------
    // MATCH REFERENCES
    // -----------------------------
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
    //  CREATE A NEW MATCH
    // --------------------------------------------------------
    public static void createMatch(String player1Id, String player2Id,
                                   OnSuccessListener<DocumentReference> onSuccess,
                                   OnFailureListener onFailure) {

        Map<String, Object> matchData = new HashMap<>();
        matchData.put("player1Id", player1Id);
        matchData.put("player2Id", player2Id);
        matchData.put("createdAt", System.currentTimeMillis());

        getMatchesCollection()
                .add(matchData)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // --------------------------------------------------------
    //  SUBMIT A PLAYER TURN
    // --------------------------------------------------------
    public static void submitTurn(String matchId, PlayedTurnDTO turn, Consumer<Boolean> callback) {//consumer זה סוג של שליח של המידע הבוליאני
        getTurnsCollection(matchId)
                .add(turn.toMap())
                .addOnSuccessListener(documentReference -> {
                    // Firestore succeeded → callback true
                    callback.accept(true);
                })
                .addOnFailureListener(e -> {
                    // Firestore failed → callback false
                    callback.accept(false);
                });
    }

    // --------------------------------------------------------
    //  LISTEN FOR NEW TURNS
    // --------------------------------------------------------
    public static ListenerRegistration listenForTurns(
            String matchId,
            TurnListener callback
    ) {
        return getTurnsCollection(matchId)
                .orderBy("submittedAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e("FirebaseUtils", "Turn listener error: ", error);
                        return;
                    }

                    if (snapshots == null || snapshots.getDocuments().isEmpty()) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            String turnId = change.getDocument().getId();
                            Map<String, Object> data = change.getDocument().getData();
                            callback.onTurnReceived(turnId, data);
                        }
                    }
                });
    }

    public interface TurnListener {
        void onTurnReceived(String turnId, Map<String, Object> turnData);
    }





    // --------------------------------------------------------
    //  GET A MATCH ONCE
    // --------------------------------------------------------
    public static Task<DocumentSnapshot> getMatch(String matchId) {
        return getMatchDocument(matchId).get();
    }
}
