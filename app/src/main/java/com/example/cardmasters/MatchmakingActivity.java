package com.example.cardmasters;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Random;

public class MatchmakingActivity extends AppCompatActivity implements FirebaseUtils.MatchmakingListener {

    private static final String TAG = "MatchmakingActivity";

    private String matchId = null;
    private boolean isTransitioning = false;

    private TextView txtStatus, txtMatchId;
    private Button btnCancel;
    private ListenerRegistration matchStatusListener;

    // --- מנגנון ה-Retry לחילוץ מחדרים ריקים ---
    private Handler retryHandler = new Handler(Looper.getMainLooper()); // TODO לרשום כהרחבה בתיק פרוייקט
    private Runnable retryRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking);

        txtStatus = findViewById(R.id.txtStatus);
        txtMatchId = findViewById(R.id.txtMatchId);
        btnCancel = findViewById(R.id.btnCancel);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExit();
            }
        });

        btnCancel.setOnClickListener(v -> handleExit());

        startMatchmaking();
    }

    private void startMatchmaking() {
        String playerId = UserPrefsUtils.getEmail(this);
        txtStatus.setText("Searching for opponent...");
        FirebaseUtils.searchForMatch(playerId, this);
    }

    @Override
    public void onMatchFound(String matchId) {
        // מצאנו משחק! נוודא שאין איזה טיימר מחיקה שרץ ברקע
        cancelRetryTimer();

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

        // --- הוספת Jitter: מתחילים טיימר אקראי בין 4 ל-8 שניות ---
        int randomDelay = 4000 + new Random().nextInt(4000);

        retryRunnable = () -> {
            Log.d(TAG, "No one joined after " + (randomDelay/1000) + "s. Verifying and retrying...");

            // מסירים את ההאזנה לפני המחיקה כדי שלא נקפיץ לעצמנו את onMatchDeleted
            if (matchStatusListener != null) matchStatusListener.remove();

            // שימוש במחיקה החכמה החדשה שלנו!
            FirebaseUtils.abortPendingMatchSafely(this.matchId,
                    () -> {
                        // הסטטוס היה PENDING והחדר נמחק, אז נחפש מחדש
                        this.matchId = null;
                        startMatchmaking();
                    },
                    () -> {
                        // אם זה הופעל, סימן שמישהו בדיוק נכנס והסטטוס השתנה ל-READY!
                        // אנחנו מבטלים את המחיקה ומפעילים מחדש את ההאזנה לסטטוס כדי שניכנס למשחק.
                        Log.d(TAG, "Match was claimed just in time! Aborting delete.");
                        listenForOpponent();
                    }
            );
        };

        //  זה מה שבאמת מפעיל את הטיימר:
        retryHandler.postDelayed(retryRunnable, randomDelay);
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
                if (matchData == null) return;

                String status = (String) matchData.get("status");
                Log.d("MATCH_DEBUG", "Status updated to: " + status);

                if ("READY".equals(status) || "ACTIVE".equals(status)) {
                    // מישהו נכנס! מבטלים מיד את טיימר ההשמדה העצמית
                    cancelRetryTimer();
                    launchGame();
                }
            }

            @Override
            public void onMatchDeleted() {
                // בגלל שאנחנו מסירים את ההאזנה לפני המחיקה היזומה שלנו,
                // הפונקציה הזו תיקרא רק אם *מישהו אחר* מחק את המשחק (למשל ברח או שהייתה שגיאה)
                Log.d("MATCH_DEBUG", "onMatchDeleted: THE MATCH WAS DELETED FROM FIRESTORE");

                if (matchId != null) {
                    Toast.makeText(MatchmakingActivity.this, "Enemy player left or match cancelled.", Toast.LENGTH_LONG).show();
                    handleDelayedExit();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e("MATCH_DEBUG", "Firestore error: " + e.getMessage());
            }
        });
    }

    private void cancelRetryTimer() {
        if (retryHandler != null && retryRunnable != null) {
            retryHandler.removeCallbacks(retryRunnable);
        }
    }

    private void handleDelayedExit() {
        cancelRetryTimer();
        if (matchStatusListener != null) {
            matchStatusListener.remove();
            matchStatusListener = null;
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::returnToMain, 3000);
    }

    private void launchGame() {
        isTransitioning = true;
        cancelRetryTimer();
        if (matchStatusListener != null) matchStatusListener.remove();

        FirebaseUtils.checkIfIStart(isStarter -> {
            Intent intent = new Intent(MatchmakingActivity.this, GameActivity.class);
            intent.putExtra("MATCH_ID", matchId);
            intent.putExtra("IS_STARTING_PLAYER", isStarter);

            startActivity(intent);
            finish();
        }, matchId);
    }

    private void handleExit() {
        cancelRetryTimer();
        if (matchStatusListener != null) matchStatusListener.remove();

        if (matchId != null && !isTransitioning) {
            FirebaseUtils.deleteMatch(matchId,
                    unused -> returnToMain(),
                    e -> returnToMain());
        } else {
            returnToMain();
        }
    }

    private void returnToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onFailure(Exception e) {
        cancelRetryTimer();
        Toast.makeText(this, "Matchmaking Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        returnToMain();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelRetryTimer();
        if (matchStatusListener != null) matchStatusListener.remove();

        if (!isTransitioning && matchId != null) {
            FirebaseUtils.deleteMatch(matchId);
        }
    }
}