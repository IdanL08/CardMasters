package com.example.cardmasters;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cardmasters.model.Hero;
import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.model.cards.EffectCard;
import com.example.cardmasters.model.cards.FighterCard;
import com.example.cardmasters.model.dto.CardDTO;
import com.example.cardmasters.model.dto.PlayedActionDTO;
import com.example.cardmasters.model.dto.PlayedTurnDTO;
import com.example.cardmasters.utils.BattlefieldUtils;
import com.example.cardmasters.utils.CardDatabaseHelper;
import com.example.cardmasters.utils.FirebaseUtils;
import com.example.cardmasters.utils.UIUtils;
import com.example.cardmasters.utils.UserPrefsUtils;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    // UI Components
    private TextView txtMatchId, txtOpponentHero, txtMyHero, txtMoney;
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
    private int money=currentTurnNumber;
    private boolean turnSubmitted = false;
    private PlayedTurnDTO pendingEnemyTurn = null; // Holds opponent's move if it arrives early
    private final List<PlayedActionDTO> actionSequence = new ArrayList<>();
    private ListenerRegistration turnListener;
    private Card currentDraggingCard = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }

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
        txtMoney= findViewById(R.id.txtMoney);
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
        //log("Battle sequence initialized. Round 1 Start!");
    }

    public boolean getTurnSubmitted() {
        return turnSubmitted;
    }

    private void loadPlayerHand() {
        playerHand = cardDbHelper.getActiveDeck();
        handContainer.removeAllViews();

        for (Card c : playerHand) {
            // Inflate the complex layout instead of a simple ImageView
            View cardView = UIUtils.createViewCard(getLayoutInflater(),  handContainer,  c,   this);
            UIUtils.setDragDropListener(cardView,this,c);

            handContainer.addView(cardView);
        }
    }

    private void setupDragAndDrop() {
        for (int i = 0; i < BattlefieldUtils.NUM_LANES; i++) {
            final int laneIdx = i;
            View laneView = playerLaneContainer.getChildAt(i);

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
                        if(currentDraggingCard == null) return false;
                        if(currentDraggingCard instanceof FighterCard){
                            FighterCard fc = (FighterCard) currentDraggingCard;

                        if ( playerLanes.get(laneIdx) == null) {
                            playerLanes.set(laneIdx, fc);
                            money-=fc.getCost();
                            txtMoney.setText("YOUR Money: "+String.valueOf(money));
                            CardDTO dto = new CardDTO(
                                    fc.getId(),
                                    "FIGHTER",
                                    fc.getHp(),
                                    fc.getAtk(),
                                    new ArrayList<>()
                            );
                            actionSequence.add(new PlayedActionDTO(String.valueOf(laneIdx), dto));

                            //log("Placed " + currentDraggingCard.getName() + " in Lane " + laneIdx);
                            refreshLaneUI();
                            return true;
                        }
                        }
                        else if(currentDraggingCard instanceof EffectCard){
                            EffectCard ec = (EffectCard) currentDraggingCard;
                            ec.applyEffect(playerLanes.get(laneIdx));
                            money-=ec.getCost();
                            txtMoney.setText("YOUR Money: "+String.valueOf(money));
                            CardDTO dto = new CardDTO(
                                    ec.getId(),
                                    "EFFECT"

                            );
                            actionSequence.add(new PlayedActionDTO(String.valueOf(laneIdx), dto));
                            //log("Placed " + currentDraggingCard.getName() + " in Lane " + laneIdx);
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

    public void setCurrentDraggingCard(Card currentDraggingCard) {
        this.currentDraggingCard = currentDraggingCard;
    }

    private void submitTurn() {
        if (turnSubmitted) return;

        PlayedTurnDTO turnDto = new PlayedTurnDTO(
                UserPrefsUtils.getEmail(this),
                currentTurnNumber,
                new ArrayList<>(actionSequence)
        );

        btnSendTurn.setEnabled(false);
        //log("Sending turn " + currentTurnNumber + "...");

        FirebaseUtils.submitTurn(matchId, turnDto, success -> {
            runOnUiThread(() -> {
                if (success) {
                    turnSubmitted = true;
                    //log("Turn sent. Waiting for opponent...");

                    // SYNC CHECK: If opponent already moved, start battle
                    if (pendingEnemyTurn != null && pendingEnemyTurn.getTurnNumber() == currentTurnNumber) {
                        //log("Opponent was waiting! Commencing battle...");
                        processBattle(pendingEnemyTurn);
                    }
                } else {
                    turnSubmitted = false;
                    btnSendTurn.setEnabled(true);
                    //log("Send failed! Try again.");
                }
            });
        });
    }

    private void startTurnListener() {
        turnListener = FirebaseUtils.listenForTurns(matchId, (turnId, turnData) -> {
            if (turnData == null) return;

            String sender = (String) turnData.get("playerId");
            if (sender != null && sender.equals(UserPrefsUtils.getEmail(this))) return;

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
                Log.d(TAG, "parsing");
                if (actionsMapList != null) {
                    Log.d(TAG, "actionsMapList: "+actionsMapList);
                    for (Map<String, Object> actionMap : actionsMapList) {
                        Log.d(TAG, "actionMap: "+actionMap);
                        PlayedActionDTO actionDto = new PlayedActionDTO();
                        actionDto.setLaneId((String) actionMap.get("laneId"));
                        Map<String, Object> cardMap = (Map<String, Object>) actionMap.get("card");
                        if (cardMap != null) {
                            Log.d(TAG, "cardMap: "+cardMap);
                            CardDTO cardDto = new CardDTO();
                            cardDto.setCardId((String) cardMap.get("cardId"));
                            cardDto.setCardType((String) cardMap.get("type"));
                            Log.d(TAG, "cardMap.get(\"type\"): "+cardMap.get("type"));
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



                    if (turnSubmitted) {
                        //log("Both ready! Commencing battle...");
                        processBattle(pendingEnemyTurn);
                    } else {
                        //log("Waiting for your move...");
                    }
                });

            } catch (Exception e) {
                //log("Parsing Error: " + e.getMessage());
            }
        });
    }

    private void processBattle(PlayedTurnDTO enemyTurn)/** (2)**/ {
        if (enemyTurn.getActions() == null || enemyTurn.getActions().isEmpty()) {
            finalizeBattle(); // No actions, go straight to math
            return;
        }

        List<PlayedActionDTO> actions = enemyTurn.getActions();
        processActionStep(actions, 0);
    }

    private void processActionStep(List<PlayedActionDTO> actions, int index) /** (2)**/{//פעולה רקורסיבית
        // Base case: All actions shown, now run the math and UI refresh
        if (index >= actions.size()) {
            finalizeBattle();
            return;
        }

        PlayedActionDTO action = actions.get(index);
        int lane = Integer.parseInt(action.getLaneId());
        CardDTO c = action.getCard();

        if (Objects.equals(c.getType(), "FIGHTER")) {
            // Update data and refresh UI immediately
            enemyLanes.set(lane, new FighterCard(c.getCardId(), "Enemy", c.getHp(), c.getAtk(), new ArrayList<>()));
            refreshLaneUI();

            // Wait 200ms then move to next action
            new Handler(Looper.getMainLooper()).postDelayed(() -> processActionStep(actions, index + 1), 200);

        } else if (Objects.equals(c.getType(), "EFFECT")) {
            // Trigger the fancy animation
            showEffectAnimation(c, lane, () -> {
                // This callback runs AFTER the animation finishes
                EffectCard ec = new EffectCard(c.getCardId());
                ec.applyEffect(enemyLanes.get(lane));
                refreshLaneUI();
                processActionStep(actions, index + 1);
            });
        }
    }

    private void showEffectAnimation(CardDTO card, int laneId, Runnable onComplete)/** (2)**/ {
        // 1. Get the Lane (The actual container for that specific slot)
        ViewGroup laneView = (ViewGroup) enemyLaneContainer.getChildAt(laneId);

        if (laneView == null) {
            onComplete.run();
            return;
        }

        // 2. CRITICAL: Disable clipping so the card can "float" outside the lane boundaries
        laneView.setClipChildren(false);
        laneView.setClipToPadding(false);
        ((ViewGroup)laneView.getParent()).setClipChildren(false);
        ((ViewGroup)laneView.getParent()).setClipToPadding(false);

        // 3. Create the Card View
        ImageView effectView = new ImageView(this);
        effectView.setImageResource(UIUtils.getImageForCard(this, card.getCardId()));

        // Size it relative to the lane or fixed DP
        int width = (int) (110 * getResources().getDisplayMetrics().density);
        int height = (int) (150 * getResources().getDisplayMetrics().density);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = Gravity.CENTER; // This handles RTL/LTR automatically!
        effectView.setLayoutParams(params);

        // 4. Reset for animation
        effectView.setScaleX(0f);
        effectView.setScaleY(0f);
        effectView.setAlpha(0f);
        effectView.setElevation(10f); // Make sure it sits "above" other cards

        laneView.addView(effectView);

        // 5. Animation
        effectView.animate()
                .scaleX(1.4f).scaleY(1.4f).alpha(1f)
                .setDuration(450)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> {
                    effectView.animate()
                            .alpha(0f)
                            .scaleX(0.8f).scaleY(0.8f)
                            .setStartDelay(600)
                            .setDuration(300)
                            .withEndAction(() -> {
                                laneView.removeView(effectView);
                                onComplete.run();
                            });
                });
    }

    private void finalizeBattle() {
        // Calculate the actual math results
        BattlefieldUtils.fieldBattle(playerLanes, enemyLanes, playerHero, enemyHero);

        runOnUiThread(() -> {
            refreshLaneUI();
            updateHeroUI();

            currentTurnNumber++;
            money = currentTurnNumber;
            turnSubmitted = false;
            pendingEnemyTurn = null;
            actionSequence.clear();

            btnSendTurn.setEnabled(true);
            checkWinCondition();
            txtMoney.setText("YOUR Money: " + money);
        });
    }

    private void refreshLaneUI() {
        for (int i = 0; i < BattlefieldUtils.NUM_LANES; i++) {
            // Find the parent layouts for the lanes
            View pLaneView = playerLaneContainer.getChildAt(i);
            View eLaneView = enemyLaneContainer.getChildAt(i);

            updateLaneUI(pLaneView, playerLanes.get(i));
            updateLaneUI(eLaneView, enemyLanes.get(i));
        }
    }

    private void updateLaneUI(View laneView, FighterCard card) {
        ImageView art = laneView.findViewById(R.id.card_image);
        TextView atkText = laneView.findViewById(R.id.atk_text);
        TextView hpText = laneView.findViewById(R.id.hp_text);

        if (card != null) {
            int resId = getResources().getIdentifier("im_" + card.getId(), "drawable", getPackageName());
            art.setImageResource(resId);
            atkText.setText(String.valueOf(card.getAtk()));
            hpText.setText(String.valueOf(card.getHp()));

            atkText.setVisibility(View.VISIBLE);
            hpText.setVisibility(View.VISIBLE);
        } else {
            art.setImageResource(android.R.color.transparent);
            atkText.setVisibility(View.INVISIBLE);
            hpText.setVisibility(View.INVISIBLE);
        }
    }

    private void updateHeroUI() {
        txtMyHero.setText("YOUR HERO: " + playerHero.getHealth() + " HP");
        txtOpponentHero.setText("ENEMY HERO: " + enemyHero.getHealth() + " HP");
    }

    private void checkWinCondition() {
        if (playerHero.getHealth() <= 0 || enemyHero.getHealth() <= 0) {
            String result = (playerHero.getHealth() > 0) ? "VICTORY!" : "DEFEAT!";
            //("GAME OVER: " + result);
            btnSendTurn.setEnabled(false);
            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
        }
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

    public int getMoney() {
        return money;
    }
}