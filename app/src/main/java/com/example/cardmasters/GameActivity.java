package com.example.cardmasters;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.model.dto.CardDTO;
import com.example.cardmasters.model.dto.PlayedActionDTO;
import com.example.cardmasters.model.dto.PlayedTurnDTO;
import com.example.cardmasters.utils.FirebaseUtils;
import com.example.cardmasters.utils.UserPrefsUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class GameActivity extends AppCompatActivity implements FirebaseUtils.MatchmakingListener {

    private static final String TAG = "GameActivity";

    private String matchId = null; // Changed to null, will be set by matchmaking

    private int currentTurnNumber = 1;

    private ListenerRegistration turnListener;

    // UI elements
    private TextView txtLog;
    private TextView txtMatchId;
    private Button sendTurnBtn;
    private Button listenBtn;
    private Button btnBack;
    private Button searchMatchBtn;

    private ProgressDialog progressDialog;
    private ListenerRegistration matchStatusListener; // New member to store the listener


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        init();
    }


    private void updateMatchStatusUI(String id) {
        this.matchId = id;
        txtMatchId.setText(String.format("Match ID: %s", id));
        searchMatchBtn.setEnabled(false); // Can't search if you're in a match

        // Only enable these if the match is READY
        if (id != null && !id.isEmpty()) {
            listenBtn.setEnabled(true);
            sendTurnBtn.setEnabled(true);
            // Optionally start listening immediately if matched
            startListening();
        } else {
            listenBtn.setEnabled(false);
            sendTurnBtn.setEnabled(false);
        }
    }


    // -----------------------------------------------------
    // MATCHMAKING BUTTON ACTION
    // -----------------------------------------------------
    private void searchForGame() {
        if (matchId != null) {
            Toast.makeText(this, "Already in match: " + matchId, Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = ProgressDialog.show(this, "Searching for Match",
                "Loading... Looking for an opponent or creating a new game.",
                true, true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {

                        Toast.makeText(GameActivity.this, "Match search canceled.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        // Use the username as the player ID
        String playerId = UserPrefsUtils.getUsername(this);
        FirebaseUtils.searchForMatch(playerId, this);
    }

    // -----------------------------------------------------
    // MatchmakingListener Implementation
    // -----------------------------------------------------


    // Update the matchmaking callbacks to start the new listener
    @Override
    public void onMatchFound(String matchId) {
        if (progressDialog != null) progressDialog.dismiss();
        Log.d(TAG, "Match Found and Joined: " + matchId);
        Toast.makeText(this, "Match Found! ID: " + matchId, Toast.LENGTH_LONG).show();
        updateMatchStatusUI(matchId);
        startMatchListeners(); // <<< START LISTENING HERE
    }

    @Override
    public void onMatchCreated(String matchId) {
        if (progressDialog != null) progressDialog.dismiss();
        Log.d(TAG, "New Match Created and Pending: " + matchId);
        Toast.makeText(this, "Match Created. Waiting for Opponent...", Toast.LENGTH_LONG).show();
        updateMatchStatusUI(matchId);
        startMatchListeners(); // <<< START LISTENING HERE
    }


    @Override
    public void onFailure(Exception e) {
        if (progressDialog != null) progressDialog.dismiss();

        Log.e(TAG, "Matchmaking FAILED: " + e.getMessage(), e);
        Toast.makeText(this, "Matchmaking failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        // Re-enable the search button after failure
        searchMatchBtn.setEnabled(true);
    }


    // -----------------------------------------------------
    // SEND A TEST TURN
    // -----------------------------------------------------
    private void sendTestTurn() {
        if (matchId == null) {
            Toast.makeText(this, "Cannot send turn. No active match ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ... (rest of sendTestTurn logic remains the same)
        // ...

        // ==== 1. Create a dummy card DTO ====
        CardDTO dummyCard = new CardDTO();
        dummyCard.setCardId("card_001");
        dummyCard.setType("Test Fighter");

        dummyCard.setBaseAttack(3);
        dummyCard.setBaseHp(5);

        // ==== 2. Wrap in PlayedActionDTO ====
        PlayedActionDTO action = new PlayedActionDTO("0", dummyCard); // lane 0

        List<PlayedActionDTO> actions = new ArrayList<>();
        actions.add(action);

        // ==== 3. Create PlayedTurnDTO ====
        PlayedTurnDTO turn = new PlayedTurnDTO(
                UserPrefsUtils.getUsername(GameActivity.this),           // real username
                currentTurnNumber,
                actions
        );

        // ==== 4. Send to Firebase ====
        FirebaseUtils.submitTurn(matchId, turn, success -> {
            if (success) {
                Log.d(TAG, "Turn sent successfully!");
                currentTurnNumber++;
            } else {
                Log.e(TAG, "FAILED to send turn");
                Toast.makeText(GameActivity.this, "Failed to send turn.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // -----------------------------------------------------
    // LISTEN FOR INCOMING TURNS
    // -----------------------------------------------------
    private void startListening() {
        if (matchId == null) {
            Toast.makeText(this, "Cannot start listening. No active match ID.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (turnListener != null) return;

        turnListener = FirebaseUtils.listenForTurns(
                matchId,
                (turnId, data) -> runOnUiThread(() -> {
                    String logEntry = "TURN RECEIVED:\n"
                            + "ID: " + turnId + "\n"
                            + "DATA: " + data + "\n\n";

                    txtLog.append(logEntry);

                    // Auto scroll (Standard Android way to scroll TextViews)
                    final int scrollAmount = txtLog.getLayout().getLineTop(txtLog.getLineCount()) - txtLog.getHeight();
                    txtLog.scrollTo(0, scrollAmount > 0 ? scrollAmount : 0);

                    Log.d(TAG, logEntry);
                })
        );

        Log.d(TAG, "Listening for turns on match: " + matchId);
        Toast.makeText(this, "Listening started for match: " + matchId, Toast.LENGTH_SHORT).show();
    }

    private void init(){
        searchMatchBtn = findViewById(R.id.btnSearchMatch); // New Button
        sendTurnBtn    = findViewById(R.id.btnSendTurn);
        listenBtn      = findViewById(R.id.btnStartListening);
        txtLog         = findViewById(R.id.txtLog);
        btnBack        = findViewById(R.id.btn_back_to_main);
        txtMatchId     = findViewById(R.id.txtMatchId); // New TextView

        btnBack.setOnClickListener(v -> finish());

        // ---------------------------
        // MATCHMAKING BUTTON
        // ---------------------------
        searchMatchBtn.setOnClickListener(v -> searchForGame());

        // ---------------------------
        // SEND TURN BUTTON (Initial state is disabled)
        // ---------------------------
        sendTurnBtn.setOnClickListener(v -> sendTestTurn());
        sendTurnBtn.setEnabled(false);

        // ---------------------------
        // LISTEN FOR INCOMING TURNS (Initial state is disabled)
        // ---------------------------
        listenBtn.setOnClickListener(v -> startListening());
        listenBtn.setEnabled(false);
    }


    private void startMatchListeners() {
        if (matchId == null || matchStatusListener != null) return;

        // 1. Start listening for turns (existing logic)
        startListening();

        // 2. Start listening for match status/deletion (new logic)
        matchStatusListener = FirebaseUtils.listenForMatchStatus(
                matchId,
                new FirebaseUtils.OnMatchStatusChangeListener() {
                    @Override
                    public void onStatusChange(Map<String, Object> matchData) {
                        // Check if the match has been completed (e.g., status == "COMPLETED")
                        String status = (String) matchData.get("status");
                        String winnerId = (String) matchData.get("winnerId");

                        if ("COMPLETED".equals(status)) {
                            handleMatchCompletion(winnerId);
                        }
                    }

                    @Override
                    public void onMatchDeleted() {
                        // CRITICAL: Match document is gone.
                        handleMatchDeletion();
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Match Status Listener Failed.", e);
                    }
                });
    }

    private void handleMatchCompletion(String winnerId) {
        stopAllListeners();
        String message;
        String currentUserId = UserPrefsUtils.getUsername(this);

        if (currentUserId.equals(winnerId)) {
            message = "CONGRATULATIONS! YOU WON THE MATCH!";
        } else if (winnerId != null) {
            message = "GAME OVER. You lost.";
        } else {
            message = "GAME ENDED IN A DRAW.";
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Prompt user to return to the main menu or a results screen
        // For now, we will wait for deletion, but this sets the final UI state.
    }

    private void handleMatchDeletion() {
        stopAllListeners();

        // Use runOnUiThread because this callback comes from a background thread
        runOnUiThread(() -> {
            Toast.makeText(GameActivity.this, "Match has been finalized and removed.", Toast.LENGTH_LONG).show();
            // Return to the previous screen (Main Menu)
            finish();
        });
    }

    private void stopAllListeners() {
        if (turnListener != null) {
            turnListener.remove();
            turnListener = null;
        }
        if (matchStatusListener != null) {
            matchStatusListener.remove();
            matchStatusListener = null;
        }
    }



    // Clean up listeners in onDestroy
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAllListeners();

    }
}