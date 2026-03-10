package com.example.cardmasters;

import android.content.Intent;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private TextView txtMatchId, txtOpponentHero, txtMyHero, txtMoney;
    private LinearLayout playerLaneContainer, enemyLaneContainer, handContainer;
    private Button btnSendTurn, btnBack;
    private ListenerRegistration matchDeleteListener;

    private String matchId;
    private CardDatabaseHelper cardDbHelper;
    private Hero playerHero, enemyHero;
    private List<FighterCard> playerLanes, enemyLanes;
    private List<Card> playerHand;
    private List<Card> playerDeck;

    private int currentTurnNumber = 1;
    private int money = currentTurnNumber;
    private boolean turnSubmitted = false;
    private PlayedTurnDTO pendingEnemyTurn = null;
    private final List<PlayedActionDTO> actionSequence = new ArrayList<>();
    private ListenerRegistration turnListener;
    private Card currentDraggingCard = null;
    private Random rand = new Random();

    private boolean isStartingPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        matchId = getIntent().getStringExtra("MATCH_ID");
        if (matchId == null) {
            Log.e(TAG, "Match Error: No ID found");
            finish();
            return;
        }

        isStartingPlayer = getIntent().getBooleanExtra("IS_STARTING_PLAYER", false);

        initUI();
        initGameState();
        setupDragAndDrop();
        startTurnListener();
        startWatchingForLeavers();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { quitGame(); }
        });
    }

    private void initUI() {
        txtMatchId = findViewById(R.id.txtMatchId);
        txtOpponentHero = findViewById(R.id.txtOpponentHero);
        txtMyHero = findViewById(R.id.txtMyHero);
        txtMoney = findViewById(R.id.txtMoney);
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

        playerDeck = cardDbHelper.getActiveDeck();
        playerHand = new ArrayList<>();

        drawCards(BattlefieldUtils.NUM_STARTING_CARDS);
        updateHeroUI();
    }

    public boolean getTurnSubmitted() { return turnSubmitted; }

    private void drawCards(int amount) {
        if (playerDeck == null || playerDeck.isEmpty()) return;

        for (int i = 0; i < amount; i++) {
            int randomIndex = rand.nextInt(playerDeck.size());
            playerHand.add(playerDeck.get(randomIndex).cloneCard());
        }
        refreshHandUI();
    }

    private void refreshHandUI() {
        handContainer.removeAllViews();
        for (Card c : playerHand) {
            View cardView = UIUtils.createViewCard(getLayoutInflater(), handContainer, c, this);
            UIUtils.setDragDropListener(cardView, this, c);
            handContainer.addView(cardView);
        }
    }

    // ========================================================
    // אנימציית טקסט מרחף במקום טואסטים (משתלב מדהים במשחק!)
    // ========================================================
    private void showFloatingText(String message, boolean isBanner) {
        ViewGroup root = findViewById(android.R.id.content);
        if (root == null) return;

        TextView floatingText = new TextView(this);
        floatingText.setText(message);
        // צבע זהב סטייל Fallout NV עם צללית כבדה
        floatingText.setTextColor(android.graphics.Color.parseColor("#FFD700"));
        floatingText.setTextSize(isBanner ? 50f : 28f); // באנר סיום ענק, הודעה רגילה קטנה יותר
        floatingText.setTypeface(null, android.graphics.Typeface.BOLD);
        floatingText.setGravity(Gravity.CENTER);
        floatingText.setShadowLayer(8f, 4f, 4f, android.graphics.Color.BLACK);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        floatingText.setLayoutParams(params);

        root.addView(floatingText);

        if (isBanner) {
            // אנימציית באנר סיום משחק (נוחת מלמעלה בבום)
            floatingText.setScaleX(3f);
            floatingText.setScaleY(3f);
            floatingText.setAlpha(0f);
            floatingText.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        } else {
            // אנימציית טקסט אירוע רגיל (מרחף ונעלם)
            floatingText.setTranslationY(100f);
            floatingText.setAlpha(0f);
            floatingText.animate()
                    .translationY(-50f).alpha(1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> {
                        floatingText.animate()
                                .translationY(-150f).alpha(0f)
                                .setStartDelay(1200) // נשאר על המסך שנייה וקצת
                                .setDuration(400)
                                .withEndAction(() -> root.removeView(floatingText))
                                .start();
                    }).start();
        }
    }

    private void animateCardPulse(View cardView) {
        if (cardView == null) return;
        cardView.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .setDuration(150)
                .withEndAction(() -> {
                    cardView.animate()
                            .scaleX(1.0f).scaleY(1.0f)
                            .setDuration(150)
                            .start();
                }).start();
    }

    private void setupDragAndDrop() {
        for (int i = 0; i < BattlefieldUtils.NUM_LANES; i++) {
            final int laneIdx = i;
            View laneView = playerLaneContainer.getChildAt(i);

            laneView.setOnDragListener((v, event) -> {
                switch (event.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED: return true;
                    case DragEvent.ACTION_DRAG_ENTERED:
                        v.setBackgroundColor(0x44FFFFFF);
                        return true;
                    case DragEvent.ACTION_DRAG_EXITED:
                        v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        return true;
                    case DragEvent.ACTION_DROP:
                        v.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                        if (currentDraggingCard == null) return false;

                        if (currentDraggingCard instanceof FighterCard) {
                            FighterCard fc = (FighterCard) currentDraggingCard;

                            if (playerLanes.get(laneIdx) == null) {
                                playerLanes.set(laneIdx, fc);
                                playerHand.remove(fc);
                                money -= fc.getCost();
                                txtMoney.setText("YOUR Money: " + money);

                                CardDTO dto = new CardDTO(fc.getId(), "FIGHTER", fc.getHp(), fc.getAtk(), new ArrayList<>());
                                actionSequence.add(new PlayedActionDTO(String.valueOf(laneIdx), dto));

                                refreshLaneUI();
                                return true;
                            }
                        } else if (currentDraggingCard instanceof EffectCard) {
                            EffectCard ec = (EffectCard) currentDraggingCard;

                            if ("slot_machine".equals(ec.getId())) {
                                int cardsToDraw = ec.getEffectPayload().getValue();
                                drawCards(cardsToDraw);
                                // --- מפעילים את הפונקציה היפה שלנו במקום Toast ---
                                showFloatingText("JACKPOT!\n+" + cardsToDraw + " CARDS", false);
                            } else {
                                ec.applyEffect(playerLanes.get(laneIdx));
                            }

                            playerHand.remove(ec);
                            money -= ec.getCost();
                            txtMoney.setText("YOUR Money: " + money);

                            CardDTO dto = new CardDTO(ec.getId(), "EFFECT");
                            actionSequence.add(new PlayedActionDTO(String.valueOf(laneIdx), dto));

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

    public void setCurrentDraggingCard(Card currentDraggingCard) { this.currentDraggingCard = currentDraggingCard; }

    private void submitTurn() {
        if (turnSubmitted) return;

        PlayedTurnDTO turnDto = new PlayedTurnDTO(UserPrefsUtils.getEmail(this), currentTurnNumber, new ArrayList<>(actionSequence));
        btnSendTurn.setEnabled(false);

        FirebaseUtils.submitTurn(matchId, turnDto, success -> {
            runOnUiThread(() -> {
                if (success) {
                    turnSubmitted = true;
                    if (pendingEnemyTurn != null && pendingEnemyTurn.getTurnNumber() == currentTurnNumber) {
                        processBattle(pendingEnemyTurn);
                    }
                } else {
                    turnSubmitted = false;
                    btnSendTurn.setEnabled(true);
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
                if (incomingTurnNum != currentTurnNumber) return;

                PlayedTurnDTO enemyTurn = new PlayedTurnDTO();
                enemyTurn.setPlayerId(sender);
                enemyTurn.setTurnNumber(incomingTurnNum);

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
                            cardDto.setCardType((String) cardMap.get("type"));
                            cardDto.setHp(((Number) cardMap.getOrDefault("hp", 0)).intValue());
                            cardDto.setAtk(((Number) cardMap.getOrDefault("atk", 0)).intValue());
                            actionDto.setCard(cardDto);
                        }
                        actionDtoList.add(actionDto);
                    }
                }
                enemyTurn.setActions(actionDtoList);

                runOnUiThread(() -> {
                    pendingEnemyTurn = enemyTurn;
                    if (turnSubmitted) processBattle(pendingEnemyTurn);
                });

            } catch (Exception e) { Log.e(TAG, "Parsing Error: " + e.getMessage()); }
        });
    }

    private void processBattle(PlayedTurnDTO enemyTurn) {
        if (enemyTurn.getActions() == null || enemyTurn.getActions().isEmpty()) {
            finalizeBattle();
        } else {
            List<PlayedActionDTO> actions = enemyTurn.getActions();
            processActionStep(actions, 0);
        }
        drawCards(1);
    }

    private void processActionStep(List<PlayedActionDTO> actions, int index) {
        if (index >= actions.size()) {
            finalizeBattle();
            return;
        }

        PlayedActionDTO action = actions.get(index);
        int lane = Integer.parseInt(action.getLaneId());
        CardDTO c = action.getCard();

        if (Objects.equals(c.getType(), "FIGHTER")) {
            enemyLanes.set(lane, new FighterCard(c.getCardId(), "Enemy", c.getHp(), c.getAtk(), new ArrayList<>()));
            refreshLaneUI();
            new Handler(Looper.getMainLooper()).postDelayed(() -> processActionStep(actions, index + 1), 200);

        } else if (Objects.equals(c.getType(), "EFFECT")) {
            showEffectAnimation(c, lane, () -> {
                CardDatabaseHelper dbHelper = new CardDatabaseHelper(this);
                EffectCard ec = (EffectCard)dbHelper.getCardById(c.getCardId());

                if (!"slot_machine".equals(c.getCardId())) {
                    ec.applyEffect(enemyLanes.get(lane));
                }

                refreshLaneUI();
                processActionStep(actions, index + 1);
            });
        }
    }

    private void showEffectAnimation(CardDTO card, int laneId, Runnable onComplete) {
        ViewGroup laneView = (ViewGroup) enemyLaneContainer.getChildAt(laneId);
        if (laneView == null) { onComplete.run(); return; }

        laneView.setClipChildren(false); laneView.setClipToPadding(false);
        ((ViewGroup)laneView.getParent()).setClipChildren(false); ((ViewGroup)laneView.getParent()).setClipToPadding(false);

        ImageView effectView = new ImageView(this);
        effectView.setImageResource(UIUtils.getImageForCard(this, card.getCardId()));

        int width = (int) (110 * getResources().getDisplayMetrics().density);
        int height = (int) (150 * getResources().getDisplayMetrics().density);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = Gravity.CENTER;
        effectView.setLayoutParams(params);

        effectView.setScaleX(0f); effectView.setScaleY(0f); effectView.setAlpha(0f); effectView.setElevation(10f);

        laneView.addView(effectView);

        effectView.animate()
                .scaleX(1.4f).scaleY(1.4f).alpha(1f)
                .setDuration(450)
                .setInterpolator(new OvershootInterpolator())
                .withEndAction(() -> {
                    effectView.animate()
                            .alpha(0f).scaleX(0.8f).scaleY(0.8f)
                            .setStartDelay(600).setDuration(300)
                            .withEndAction(() -> { laneView.removeView(effectView); onComplete.run(); });
                });
    }

    private void finalizeBattle() {
        BattlefieldUtils.BattleEventsListener drawListener = (hero, amount) -> {
            if (hero == playerHero) {
                runOnUiThread(() -> drawCards(amount));
            }
        };

        if(isStartingPlayer) {
            BattlefieldUtils.fieldBattle(playerLanes, enemyLanes, playerHero, enemyHero, drawListener);
        } else {
            BattlefieldUtils.fieldBattle(enemyLanes, playerLanes, enemyHero, playerHero, drawListener);
        }

        runOnUiThread(() -> {
            refreshLaneUI();
            updateHeroUI();

            int mrHouseCards = 0;
            for (int i = 0; i < BattlefieldUtils.NUM_LANES; i++) {
                FighterCard fc = playerLanes.get(i);
                if (fc != null && "mr_house".equals(fc.getId()) && !fc.isDead()) {
                    mrHouseCards++;
                    View laneView = playerLaneContainer.getChildAt(i);
                    animateCardPulse(laneView);
                }
            }
            if (mrHouseCards > 0) {
                drawCards(mrHouseCards);
                // --- מפעילים את הפונקציה היפה שלנו במקום Toast ---
                showFloatingText("THE HOUSE ALWAYS WINS!\n+" + mrHouseCards + " CARDS", false);
            }

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
            atkText.setVisibility(View.VISIBLE); hpText.setVisibility(View.VISIBLE);
        } else {
            art.setImageResource(android.R.color.transparent);
            atkText.setVisibility(View.INVISIBLE); hpText.setVisibility(View.INVISIBLE);
        }
    }

    private void updateHeroUI() {
        txtMyHero.setText("YOUR HERO: " + playerHero.getHealth() + " HP");
        txtOpponentHero.setText("ENEMY HERO: " + enemyHero.getHealth() + " HP");
    }

    private void checkWinCondition() {
        if (playerHero.getHealth() <= 0 || enemyHero.getHealth() <= 0) {
            boolean win = playerHero.getHealth() > enemyHero.getHealth();
            long rating = (win) ? 100 : -100;
            String result = (win) ? "VICTORY!" : "DEFEAT!";

            btnSendTurn.setEnabled(false);

            // --- באנר סיום משחק בולט במקום Toast ---
            showFloatingText(result, true);

            FirebaseUtils.updatePlayerRating(rating);
            // הגדלתי טיפה את ההמתנה כדי שיראו את האנימציה בכיף לפני שזה יוצא
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> quitGame(), 4000);
        }
    }

    private void quitGame() {
        if (turnListener != null) turnListener.remove();
        FirebaseUtils.deleteMatch(matchId);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (matchDeleteListener != null) matchDeleteListener.remove();
        startActivity(intent);
        finish();
    }

    private void startWatchingForLeavers() {
        if (matchId == null) return;
        matchDeleteListener = FirebaseFirestore.getInstance()
                .collection("matches").document(matchId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && !snapshot.exists()) {

                        // --- באנר סיום משחק במקום Toast ---
                        showFloatingText("ENEMY RETREATED!\nVICTORY!", true);

                        if (matchDeleteListener != null) matchDeleteListener.remove();
                        btnSendTurn.setEnabled(false); btnBack.setEnabled(false);
                        FirebaseUtils.updatePlayerRating(100);
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> quitGame(), 4000);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (turnListener != null) turnListener.remove();
        if (matchDeleteListener != null) matchDeleteListener.remove();
    }

    public int getMoney() { return money; }
}