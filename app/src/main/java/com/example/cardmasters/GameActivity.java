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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private TextView txtMatchId, txtOpponentHero, txtMyHero, txtMoney, txtTimer;
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

    // --- טיימרים וסנכרון ---
    private android.os.CountDownTimer turnTimer;
    private android.os.CountDownTimer graceTimer;
    private static final long TURN_DURATION = 25000;
    private static final long GRACE_PERIOD = 10000;

    private final Map<Integer, PlayedTurnDTO> opponentTurnsMap = new HashMap<>();
    private boolean isAnimating = false;

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
            public void handleOnBackPressed() { showQuitConfirmationDialog(); }
        });
    }

    private void initUI() {
        txtMatchId = findViewById(R.id.txtMatchId);
        txtOpponentHero = findViewById(R.id.txtOpponentHero);
        txtMyHero = findViewById(R.id.txtMyHero);
        txtMoney = findViewById(R.id.txtMoney);
        txtTimer = findViewById(R.id.txtTimer);
        playerLaneContainer = findViewById(R.id.player_lane_container);
        enemyLaneContainer = findViewById(R.id.enemy_lane_container);
        handContainer = findViewById(R.id.hand_container);
        btnSendTurn = findViewById(R.id.btnSendTurn);
        btnBack = findViewById(R.id.btn_back_to_main);

        txtMatchId.setText("Match ID: " + matchId);

        // עיצוב כפתור היציאה
        android.graphics.drawable.GradientDrawable gdQuit = new android.graphics.drawable.GradientDrawable();
        gdQuit.setColor(android.graphics.Color.parseColor("#C0392B"));
        gdQuit.setCornerRadius(24f);
        btnBack.setBackground(gdQuit);
        btnBack.setTextColor(android.graphics.Color.WHITE);

        // עיצוב השעון
        android.graphics.drawable.GradientDrawable timerBg = new android.graphics.drawable.GradientDrawable();
        timerBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        timerBg.setColor(android.graphics.Color.parseColor("#34495E"));
        timerBg.setStroke(2, android.graphics.Color.parseColor("#7F8C8D"));
        txtTimer.setBackground(timerBg);

        enableEndTurnWithDelay();
        btnSendTurn.setOnClickListener(v -> submitTurn());
        btnBack.setOnClickListener(v -> showQuitConfirmationDialog());
    }

    /**
     * פונקציה שנועלת את הכפתור ופותחת אותו רק אחרי 2 שניות של חסד
     */
    private void enableEndTurnWithDelay() {
        setEndTurnEnabled(false); // נועלים מיד
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // פותחים רק אם המשחק עדיין פעיל והתור לא הוגש אוטומטית בינתיים
            if (playerHero.getHealth() > 0 && enemyHero.getHealth() > 0 && !turnSubmitted) {
                setEndTurnEnabled(true);
            }
        }, 2000); // השהיה של 2000 מילישניות (2 שניות)
    }

    private void setEndTurnEnabled(boolean isEnabled) {
        btnSendTurn.setEnabled(isEnabled);
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setCornerRadius(24f);

        if (isEnabled) {
            gd.setColor(android.graphics.Color.parseColor("#D4AF37"));
            gd.setStroke(4, android.graphics.Color.parseColor("#B8860B"));
            btnSendTurn.setTextColor(android.graphics.Color.BLACK);
        } else {
            gd.setColor(android.graphics.Color.parseColor("#2C3E50"));
            gd.setStroke(4, android.graphics.Color.parseColor("#1A252F"));
            btnSendTurn.setTextColor(android.graphics.Color.parseColor("#7F8C8D"));
        }
        btnSendTurn.setBackground(gd);
    }

    private void showQuitConfirmationDialog() {
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Retreat from Battle?")
                .setMessage("Are you sure you want to quit? You will lose the match and rating points!")
                .setPositiveButton("Retreat", (d, which) -> quitGame())
                .setNegativeButton("Cancel", null)
                .show();

        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#E74C3C"));
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.parseColor("#BDC3C7"));
    }

    // ==========================================
    // מנגנון הטיימרים
    // ==========================================

    private void startTurnTimer() {//מופעל בתחילת המשחק ובתור חדש בסיום הקרב
        cancelTimers();
        txtTimer.setTextColor(android.graphics.Color.WHITE);

        turnTimer = new android.os.CountDownTimer(TURN_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                txtTimer.setText(String.valueOf(secondsLeft));

                if (secondsLeft <= 5) {
                    txtTimer.setTextColor(android.graphics.Color.parseColor("#E74C3C"));
                }
            }

            @Override
            public void onFinish() {
                txtTimer.setText("0");
                if (!turnSubmitted) {
                    submitTurn();
                }
                startGraceTimer();
            }
        }.start();
    }

    private void startGraceTimer() {//למען דיליי רשת
        graceTimer = new android.os.CountDownTimer(GRACE_PERIOD, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                txtTimer.setText("...");
            }

            @Override
            public void onFinish() {
                if (!isAnimating) {
                    declareAutoWin();
                }
            }
        }.start();
    }

    private void cancelTimers() {
        if (turnTimer != null) turnTimer.cancel();
        if (graceTimer != null) graceTimer.cancel();
        txtTimer.setText("25");
    }

    private void declareAutoWin() {
        if (playerHero.getHealth() <= 0) return;
        cancelTimers();
        setEndTurnEnabled(false);
        showFloatingText("OPPONENT TIMED OUT!\nVICTORY!", true);
        FirebaseUtils.updatePlayerRating(100);
        new Handler(Looper.getMainLooper()).postDelayed(this::quitGame, 4000);
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
        startTurnTimer();
    }

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

    private void showFloatingText(String message, boolean isBanner) {
        ViewGroup root = findViewById(android.R.id.content);
        if (root == null) return;

        TextView floatingText = new TextView(this);
        floatingText.setText(message);
        floatingText.setTextColor(android.graphics.Color.parseColor("#FFD700"));
        floatingText.setTextSize(isBanner ? 50f : 28f);
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
            floatingText.setScaleX(3f);
            floatingText.setScaleY(3f);
            floatingText.setAlpha(0f);
            floatingText.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(600)
                    .setInterpolator(new OvershootInterpolator())
                    .start();
        } else {
            floatingText.setTranslationY(100f);
            floatingText.setAlpha(0f);
            floatingText.animate()
                    .translationY(-50f).alpha(1f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> {
                        floatingText.animate()
                                .translationY(-150f).alpha(0f)
                                .setStartDelay(1200)
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

                        // הגנה מגרירה אחרי סיום תור
                        if (currentDraggingCard == null || turnSubmitted) return false;

                        if (currentDraggingCard instanceof FighterCard) {
                            FighterCard fc = (FighterCard) currentDraggingCard;

                            if (playerLanes.get(laneIdx) == null) {
                                playerLanes.set(laneIdx, fc);
                                playerHand.remove(fc);
                                money -= fc.getCost();
                                txtMoney.setText("MONEY: " + money);

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
                                showFloatingText("JACKPOT!\n+" + cardsToDraw + " CARDS", false);
                            } else {
                                ec.applyEffect(playerLanes.get(laneIdx));
                            }

                            playerHand.remove(ec);
                            money -= ec.getCost();
                            txtMoney.setText("MONEY: " + money);

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

        turnSubmitted = true;
        setEndTurnEnabled(false);

        PlayedTurnDTO turnDto = new PlayedTurnDTO(UserPrefsUtils.getEmail(this), currentTurnNumber, new ArrayList<>(actionSequence));

        FirebaseUtils.submitTurn(matchId, turnDto, success -> {
            runOnUiThread(() -> {
                if (success) {
                    tryStartBattle();
                } else {
                    turnSubmitted = false;
                    setEndTurnEnabled(true);
                    Toast.makeText(this, "Failed to send turn. Network issue?", Toast.LENGTH_SHORT).show();
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

                if (incomingTurnNum < currentTurnNumber) return;

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
                    opponentTurnsMap.put(incomingTurnNum, enemyTurn);
                    tryStartBattle();
                });

            } catch (Exception e) { Log.e(TAG, "Parsing Error: " + e.getMessage()); }
        });
    }

    private void tryStartBattle() {
        if (isAnimating) return;

        if (turnSubmitted && opponentTurnsMap.containsKey(currentTurnNumber)) {
            isAnimating = true;
            cancelTimers(); // עוצרים את הטיימרים במהלך הקרב
            PlayedTurnDTO enemyTurn = opponentTurnsMap.get(currentTurnNumber);
            processBattle(enemyTurn);
        }
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

    // התיקון לאנימציות שלא עוצרות את הלוגיקה (Barrage)
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
            new Handler(Looper.getMainLooper()).postDelayed(() -> processActionStep(actions, index + 1), 300);

        } else if (Objects.equals(c.getType(), "EFFECT")) {
            CardDatabaseHelper dbHelper = new CardDatabaseHelper(this);
            EffectCard ec = (EffectCard)dbHelper.getCardById(c.getCardId());

            if (!"slot_machine".equals(c.getCardId())) {
                ec.applyEffect(enemyLanes.get(lane));
            }
            refreshLaneUI();

            showEffectAnimation(c, lane);
            new Handler(Looper.getMainLooper()).postDelayed(() -> processActionStep(actions, index + 1), 300);
        }
    }

    private void showEffectAnimation(CardDTO card, int laneId) {
        ViewGroup laneView = (ViewGroup) enemyLaneContainer.getChildAt(laneId);
        if (laneView == null) return;

        laneView.setClipChildren(false); laneView.setClipToPadding(false);
        ((ViewGroup)laneView.getParent()).setClipChildren(false); ((ViewGroup)laneView.getParent()).setClipToPadding(false);

        ImageView effectView = new ImageView(this);
        effectView.setImageResource(UIUtils.getImageForCard(this, card.getCardId()));

        int width = (int) (140 * getResources().getDisplayMetrics().density);
        int height = (int) (190 * getResources().getDisplayMetrics().density);

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
                            .setStartDelay(300)
                            .setDuration(300)
                            .withEndAction(() -> laneView.removeView(effectView));
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
                showFloatingText("THE HOUSE ALWAYS WINS!\n+" + mrHouseCards + " CARDS", false);
            }

            currentTurnNumber++;
            money = currentTurnNumber;
            turnSubmitted = false;
            actionSequence.clear();

            setEndTurnEnabled(true);
            checkWinCondition();
            txtMoney.setText("MONEY: " + money);

            isAnimating = false;

            // מפעילים את הטיימר מחדש רק אם המשחק עדיין לא נגמר
            if (playerHero.getHealth() > 0 && enemyHero.getHealth() > 0) {
                startTurnTimer();
            }
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
            cancelTimers(); // מוודא שהטיימרים מפסיקים כשהמשחק נגמר
            boolean win = playerHero.getHealth() > enemyHero.getHealth();
            long rating = (win) ? 100 : -100;
            String result = (win) ? "VICTORY!" : "DEFEAT!";

            setEndTurnEnabled(false);

            showFloatingText(result, true);

            FirebaseUtils.updatePlayerRating(rating);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> quitGame(), 4000);
        }
    }

    private void quitGame() {
        cancelTimers();
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
                        cancelTimers();
                        showFloatingText("ENEMY RETREATED!\nVICTORY!", true);

                        if (matchDeleteListener != null) matchDeleteListener.remove();
                        setEndTurnEnabled(false);
                        btnBack.setEnabled(false);
                        FirebaseUtils.updatePlayerRating(100);
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> quitGame(), 4000);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimers();
        if (turnListener != null) turnListener.remove();
        if (matchDeleteListener != null) matchDeleteListener.remove();
    }

    public boolean getTurnSubmitted() {
        return turnSubmitted;
    }
    public int getMoney()
    {
        return money;
    }
}