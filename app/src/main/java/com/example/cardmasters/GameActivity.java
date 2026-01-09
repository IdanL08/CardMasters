package com.example.cardmasters;

import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.model.Hero;
import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.model.cards.FighterCard;
import com.example.cardmasters.model.dto.CardDTO;
import com.example.cardmasters.model.dto.PlayedActionDTO;
import com.example.cardmasters.model.dto.PlayedTurnDTO;
import com.example.cardmasters.utils.BattlefieldUtils;
import com.example.cardmasters.utils.CardDatabaseHelper;
import com.example.cardmasters.utils.FirebaseUtils;
import com.example.cardmasters.utils.UserPrefsUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    // UI Components
    private TextView txtMatchId, txtOpponentHero, txtMyHero, txtLog;
    private LinearLayout playerLaneContainer, enemyLaneContainer, handContainer;
    private Button btnSendTurn, btnBack;

    // Game State
    private String matchId;
    private CardDatabaseHelper cardDbHelper;
    private Hero playerHero, enemyHero;
    private List<FighterCard> playerLanes, enemyLanes;
    private List<Card> playerHand;

    // Turn Tracking & Synchronization
    private int currentTurnNumber = 1;
    private boolean turnSubmitted = false;
    private PlayedTurnDTO pendingEnemyTurn = null; // Holds opponent's move if it arrives early
    private final List<PlayedActionDTO> actionSequence = new ArrayList<>();
    private ListenerRegistration turnListener;
    private FighterCard currentDraggingCard = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        matchId = getIntent().getStringExtra("MATCH_ID");
        if (matchId == null) {
            Toast.makeText(this, "Match Error: No ID found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initUI();
        initGameState();
        setupDragAndDrop();
        startTurnListener();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                quitGame();
            }
        });
    }

    private void initUI() {
        txtMatchId = findViewById(R.id.txtMatchId);
        txtOpponentHero = findViewById(R.id.txtOpponentHero);
        txtMyHero = findViewById(R.id.txtMyHero);
        txtLog = findViewById(R.id.txtLog);
        playerLaneContainer = findViewById(R.id.player_lane_container);
        enemyLaneContainer = findViewById(R.id.enemy_lane_container);
        handContainer = findViewById(R.id.hand_container);
        btnSendTurn = findViewById(R.id.btnSendTurn);
        btnBack = findViewById(R.id.btn_back_to_main);

        txtMatchId.setText("Match ID: " + matchId);
        btnSendTurn.setOnClickListener(v -> submitTurn());
        btnBack.setOnClickListener(v -> quitGame());
    }

    private void initGameState() {
        cardDbHelper = new CardDatabaseHelper(this);
        playerHero = new Hero(UserPrefsUtils.getUsername(this), 30);
        enemyHero = new Hero("Opponent", 30);

        playerLanes = new ArrayList<>();
        enemyLanes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            playerLanes.add(null);
            enemyLanes.add(null);
        }

        loadPlayerHand();
        updateHeroUI();
        log("Battle sequence initialized. Round 1 Start!");
    }

    private void loadPlayerHand() {
        playerHand = cardDbHelper.getActiveDeck();
        handContainer.removeAllViews();

        for (Card c : playerHand) {
            ImageView cardImg = new ImageView(this);
            int resId = getResources().getIdentifier("im_" + c.getId(), "drawable", getPackageName());
            cardImg.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_help);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(220, 300);
            lp.setMargins(10, 0, 10, 0);
            cardImg.setLayoutParams(lp);

            cardImg.setOnLongClickListener(v -> {
                if (turnSubmitted) return false;
                currentDraggingCard = (c instanceof FighterCard) ? (FighterCard) c : null;
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
                v.startDragAndDrop(data, shadow, v, 0);
                v.setVisibility(View.INVISIBLE);
                return true;
            });
            handContainer.addView(cardImg);
        }
    }

    private void setupDragAndDrop() {
        for (int i = 0; i < 5; i++) {
            final int laneIdx = i;
            ImageView laneView = (ImageView) playerLaneContainer.getChildAt(i);

            laneView.setOnDragListener((v, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        v.setBackgroundColor(0x44FFFFFF);
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        return true;
                    case DragEvent.ACTION_DROP:
                        v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        if (currentDraggingCard != null && playerLanes.get(laneIdx) == null) {
                            playerLanes.set(laneIdx, currentDraggingCard);
                            CardDTO dto = new CardDTO(
                                    currentDraggingCard.getId(),
                                    "FIGHTER",
                                    currentDraggingCard.getHp(),
                                    currentDraggingCard.getAtk(),
                                    new ArrayList<>()
                            );
                            actionSequence.add(new PlayedActionDTO(String.valueOf(laneIdx), dto));
                            log("Placed " + currentDraggingCard.getName() + " in Lane " + laneIdx);
                            refreshLaneUI();
                            return true;
                        }
                        return false;
                    case DragEvent.ACTION_DRAG_ENDED:
                        View draggedView = (View) event.getLocalState();
                        if (draggedView != null) {
                            if (!event.getResult()) draggedView.setVisibility(View.VISIBLE);
                            else handContainer.removeView(draggedView);
                        }
                        currentDraggingCard = null;
                        return true;
                }
                return true;
            });
        }
    }

    private void submitTurn() {
        if (turnSubmitted) return;

        PlayedTurnDTO turnDto = new PlayedTurnDTO(
                UserPrefsUtils.getUsername(this),
                currentTurnNumber,
                new ArrayList<>(actionSequence)
        );

        btnSendTurn.setEnabled(false);
        log("Sending turn " + currentTurnNumber + "...");

        FirebaseUtils.submitTurn(matchId, turnDto, success -> {
            runOnUiThread(() -> {
                if (success) {
                    turnSubmitted = true;
                    log("Turn sent. Waiting for opponent...");

                    // SYNC CHECK: If opponent already moved, start battle
                    if (pendingEnemyTurn != null && pendingEnemyTurn.getTurnNumber() == currentTurnNumber) {
                        log("Opponent was waiting! Commencing battle...");
                        processBattle(pendingEnemyTurn);
                    }
                } else {
                    turnSubmitted = false;
                    btnSendTurn.setEnabled(true);
                    log("Send failed! Try again.");
                }
            });
        });
    }

    private void startTurnListener() {
        turnListener = FirebaseUtils.listenForTurns(matchId, (turnId, turnData) -> {
            if (turnData == null) return;

            String sender = (String) turnData.get("playerId");
            if (sender != null && sender.equals(UserPrefsUtils.getUsername(this))) return;

            try {
                Object tNumObj = turnData.get("turnNumber");
                if (tNumObj == null) return;

                int incomingTurnNum = ((Number) tNumObj).intValue();

                // Only handle turns for the current round
                if (incomingTurnNum != currentTurnNumber) return;

                PlayedTurnDTO enemyTurn = new PlayedTurnDTO();
                enemyTurn.setPlayerId(sender);
                enemyTurn.setTurnNumber(incomingTurnNum);

                // Parsing Logic
                List<Map<String, Object>> actionsMapList = (List<Map<String, Object>>) turnData.get("actions");
                List<PlayedActionDTO> actionDtoList = new ArrayList<>();
                if (actionsMapList != null) {
                    for (Map<String, Object> actionMap : actionsMapList) {
                        PlayedActionDTO actionDto = new PlayedActionDTO();
                        actionDto.setLaneId((String) actionMap.get("laneId"));
                        Map<String, Object> cardMap = (Map<String, Object>) actionMap.get("card");
                        if (cardMap != null) {
                            CardDTO cardDto = new CardDTO();
                            cardDto.setCardId((String) cardMap.get("cardId"));
                            cardDto.setHp(((Number) cardMap.getOrDefault("hp", 0)).intValue());
                            cardDto.setAtk(((Number) cardMap.getOrDefault("atk", 0)).intValue());
                            actionDto.setCard(cardDto);
                        }
                        actionDtoList.add(actionDto);
                    }
                }
                enemyTurn.setActions(actionDtoList);

                // SYNC CHECK: Store data and check if we have submitted our turn
                runOnUiThread(() -> {
                    pendingEnemyTurn = enemyTurn;
                    log("Opponent's turn " + incomingTurnNum + " received.");

                    if (turnSubmitted) {
                        log("Both ready! Commencing battle...");
                        processBattle(pendingEnemyTurn);
                    } else {
                        log("Waiting for your move...");
                    }
                });

            } catch (Exception e) {
                log("Parsing Error: " + e.getMessage());
            }
        });
    }

    private void processBattle(PlayedTurnDTO enemyTurn) {
        // 1. Setup enemy lanes
        if (enemyTurn.getActions() != null) {
            for (PlayedActionDTO action : enemyTurn.getActions()) {
                if (action != null && action.getLaneId() != null && action.getCard() != null) {
                    int lane = Integer.parseInt(action.getLaneId());
                    CardDTO c = action.getCard();
                    enemyLanes.set(lane, new FighterCard(c.getCardId(), "Enemy", c.getHp(), c.getAtk(), new ArrayList<>()));
                }
            }
        }

        // 2. Resolve Battle Logic
        BattlefieldUtils.fieldBattle(playerLanes, enemyLanes, playerHero, enemyHero);

        // 3. UI Update & Reset Synchronization Gates
        runOnUiThread(() -> {
            refreshLaneUI();
            updateHeroUI();

            // Increment Turn and Reset
            currentTurnNumber++;
            turnSubmitted = false;
            pendingEnemyTurn = null;
            actionSequence.clear();

            btnSendTurn.setEnabled(true);
            log("--- Round " + currentTurnNumber + " Start ---");
            checkWinCondition();
        });
    }

    private void refreshLaneUI() {
        for (int i = 0; i < 5; i++) {
            ImageView pLane = (ImageView) playerLaneContainer.getChildAt(i);
            FighterCard pC = playerLanes.get(i);
            if (pC != null && pC.getHp() > 0) {
                int resId = getResources().getIdentifier("im_" + pC.getId(), "drawable", getPackageName());
                pLane.setImageResource(resId);
            } else {
                pLane.setImageResource(android.R.color.transparent);
                playerLanes.set(i, null);
            }

            ImageView eLane = (ImageView) enemyLaneContainer.getChildAt(i);
            FighterCard eC = enemyLanes.get(i);
            if (eC != null && eC.getHp() > 0) {
                int resId = getResources().getIdentifier("im_" + eC.getId(), "drawable", getPackageName());
                eLane.setImageResource(resId);
            } else {
                eLane.setImageResource(android.R.color.transparent);
                enemyLanes.set(i, null);
            }
        }
    }

    private void updateHeroUI() {
        txtMyHero.setText("YOUR HERO: " + playerHero.getHealth() + " HP");
        txtOpponentHero.setText("ENEMY HERO: " + enemyHero.getHealth() + " HP");
    }

    private void checkWinCondition() {
        if (playerHero.getHealth() <= 0 || enemyHero.getHealth() <= 0) {
            String result = (playerHero.getHealth() > 0) ? "VICTORY!" : "DEFEAT!";
            log("GAME OVER: " + result);
            btnSendTurn.setEnabled(false);
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        }
    }

    private void log(String msg) {
        txtLog.append("\n> " + msg);
        // Optional: Add auto-scroll for the log here
    }

    private void quitGame() {
        if (turnListener != null) turnListener.remove();
        FirebaseUtils.deleteMatch(matchId);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (turnListener != null) turnListener.remove();
    }
}