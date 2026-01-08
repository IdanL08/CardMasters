package com.example.cardmasters.utils;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MatchmakingUtils {
    private static final String COL = "matches";

    public static void createMatch(String uid, FirebaseFirestore db, Runnable onCreated) {
        Map<String, Object> m = new HashMap<>();
        m.put("player1", uid);
        m.put("player2", null);
        m.put("waiting", true);
        db.collection(COL).add(m).addOnSuccessListener(r -> onCreated.run());
    }

    public static void joinMatch(String matchId, String uid, FirebaseFirestore db, Runnable onJoined) {
        db.collection(COL).document(matchId)
                .update("player2", uid, "waiting", false)
                .addOnSuccessListener(v -> onJoined.run());
    }
}
