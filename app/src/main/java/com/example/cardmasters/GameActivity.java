package com.example.cardmasters;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.cardmasters.model.dto.CardDTO;
import com.example.cardmasters.model.dto.PlayedActionDTO;
import com.example.cardmasters.model.dto.PlayedTurnDTO;
import com.example.cardmasters.utils.FirebaseUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;


public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private String matchId = "test_match"; // Temporary while testing

    private int currentTurnNumber = 1;

    private ListenerRegistration turnListener;
    private TextView txtLog;
    private Button sendTurnBtn ;
    private Button listenBtn ;

    private Button btnBack;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        init();
    }


    // -----------------------------------------------------
    // SEND A TEST TURN
    // -----------------------------------------------------
    private void sendTestTurn() {

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
        FirebaseUtils.getUsername(new FirebaseUtils.UsernameCallback() {
            @Override
            public void onUsernameLoaded(String username) {

                PlayedTurnDTO turn = new PlayedTurnDTO(
                        username,           // real username
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
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("Game", "Failed to load username", e);
            }
        });



    }


    // -----------------------------------------------------
    // LISTEN FOR INCOMING TURNS
    // -----------------------------------------------------
    private void startListening() {
        if (turnListener != null) return;

        turnListener = FirebaseUtils.listenForTurns(
                matchId,
                (turnId, data) -> runOnUiThread(() -> {
                    String logEntry = "TURN RECEIVED:\n"
                            + "ID: " + turnId + "\n"
                            + "DATA: " + data + "\n\n";

                    txtLog.append(logEntry);

                    // Auto scroll
                    int scrollAmount = txtLog.getLayout().getLineTop(txtLog.getLineCount()) - txtLog.getHeight();
                    if (scrollAmount > 0)
                        txtLog.scrollTo(0, scrollAmount);
                    else
                        txtLog.scrollTo(0, 0);

                    Log.d(TAG, logEntry);
                })
        );

        Log.d(TAG, "Listening for turns...");
    }

    private void init(){
        sendTurnBtn = findViewById(R.id.btnSendTurn);
        listenBtn   = findViewById(R.id.btnStartListening);
        txtLog    = findViewById(R.id.txtLog);
        btnBack = findViewById(R.id.btn_back_to_main);
        btnBack.setOnClickListener(v -> {finish();});


        // ---------------------------
        // SEND TURN BUTTON
        // ---------------------------
        sendTurnBtn.setOnClickListener(v -> sendTestTurn());

        // ---------------------------
        // LISTEN FOR INCOMING TURNS
        // ---------------------------
        listenBtn.setOnClickListener(v -> startListening());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (turnListener != null) {
            turnListener.remove();
            turnListener = null;
        }
    }
}
