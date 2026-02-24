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
        String playerId = UserPrefsUtils.getEmail(this);
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
        if (matchId == null) {
            Log.d("MATCH_DEBUG", "listenForOpponent: matchId is null, exiting.");
            return;
        }

        Log.d("MATCH_DEBUG", "Starting listener for matchId: " + matchId);

        matchStatusListener = FirebaseUtils.listenForMatchStatus(matchId, new FirebaseUtils.OnMatchStatusChangeListener() {
            @Override
            public void onStatusChange(Map<String, Object> matchData) {
                if (matchData == null) {
                    Log.d("MATCH_DEBUG", "onStatusChange: matchData is null");
                    return;
                }
                String status = (String) matchData.get("status");
                Log.d("MATCH_DEBUG", "Status updated to: " + status);

                if ("READY".equals(status) || "ACTIVE".equals(status)) {
                    launchGame();
                }
            }

            @Override
            public void onMatchDeleted() {
                Log.d("MATCH_DEBUG", "onMatchDeleted: THE MATCH WAS DELETED FROM FIRESTORE");
                Toast.makeText(MatchmakingActivity.this, "Enemy player left, you win!", Toast.LENGTH_LONG).show();
                handleDelayedExit();
            }

            @Override
            public void onError(Exception e) {
                Log.e("MATCH_DEBUG", "Firestore error: " + e.getMessage());
            }
        });
    }

    private void handleDelayedExit() {
        // 3. Stop listening immediately so we don't get multiple callbacks
        if (matchStatusListener != null) {
            matchStatusListener.remove();
            matchStatusListener = null; // Prevent memory leaks/double calls
        }

        // 4. Wait 3 seconds then return to main menu
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // Use your existing navigation method
            returnToMain();
        }, 3000);
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