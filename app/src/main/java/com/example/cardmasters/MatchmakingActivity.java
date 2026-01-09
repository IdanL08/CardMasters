package com.example.cardmasters;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.utils.FirebaseUtils;
import com.example.cardmasters.utils.UserPrefsUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;

public class MatchmakingActivity extends AppCompatActivity implements FirebaseUtils.MatchmakingListener {

    private static final String TAG = "MatchmakingActivity";

    private String matchId = null;
    private boolean isTransitioning = false;

    private TextView txtStatus, txtMatchId;
    private Button btnCancel;
    private ListenerRegistration matchStatusListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking);

        txtStatus = findViewById(R.id.txtStatus);
        txtMatchId = findViewById(R.id.txtMatchId);
        btnCancel = findViewById(R.id.btnCancel);

        // --- ANDROIDX BACK NAVIGATION DISPATCHER ---
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExit();
            }
        });

        btnCancel.setOnClickListener(v -> handleExit());

        // Auto-start matchmaking
        startMatchmaking();
    }

    private void startMatchmaking() {
        String playerId = UserPrefsUtils.getUsername(this);
        txtStatus.setText("Searching for opponent...");
        FirebaseUtils.searchForMatch(playerId, this);
    }

    @Override
    public void onMatchFound(String matchId) {
        this.matchId = matchId;
        txtMatchId.setText("Match ID: " + matchId);
        txtStatus.setText("Connecting...");
        listenForOpponent();
    }

    @Override
    public void onMatchCreated(String matchId) {
        this.matchId = matchId;
        txtMatchId.setText("Match ID: " + matchId);
        txtStatus.setText("Waiting for opponent...");
        listenForOpponent();
    }

    private void listenForOpponent() {
        if (matchId == null) return;

        matchStatusListener = FirebaseUtils.listenForMatchStatus(matchId, new FirebaseUtils.OnMatchStatusChangeListener() {
            @Override
            public void onStatusChange(Map<String, Object> matchData) {
                String status = (String) matchData.get("status");
                // Switch to GameActivity if another player joins
                if ("READY".equals(status) || "ACTIVE".equals(status)) {
                    launchGame();
                }
            }

            @Override
            public void onMatchDeleted() {
                Toast.makeText(MatchmakingActivity.this, "Match was closed.", Toast.LENGTH_SHORT).show();
                handleExit();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Firestore error: " + e.getMessage());
            }
        });
    }

    private void launchGame() {
        isTransitioning = true;
        if (matchStatusListener != null) matchStatusListener.remove();

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("MATCH_ID", matchId);
        startActivity(intent);
        finish();
    }

    private void handleExit() {
        // Stop listening to avoid callbacks during destruction
        if (matchStatusListener != null) matchStatusListener.remove();

        if (matchId != null && !isTransitioning) {
            // Cleanup the match in Firebase before leaving
            FirebaseUtils.deleteMatch(matchId,
                    unused -> returnToMain(),
                    e -> returnToMain());
        } else {
            returnToMain();
        }
    }

    private void returnToMain() {
        // Since MainActivity was finished, we must create a new Intent to go back
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onFailure(Exception e) {
        Toast.makeText(this, "Matchmaking Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        returnToMain();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (matchStatusListener != null) matchStatusListener.remove();

        // Final safety: If the user swiped the app away, try to delete the match
        if (!isTransitioning && matchId != null) {
            FirebaseUtils.deleteMatch(matchId);
        }
    }
}