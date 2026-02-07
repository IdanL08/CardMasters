package com.example.cardmasters.utils;


import static androidx.browser.customtabs.CustomTabsClient.getPackageName;

import android.content.ClipData;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.cardmasters.GameActivity;
import com.example.cardmasters.R;
import com.example.cardmasters.model.cards.Card;
import com.example.cardmasters.model.cards.EffectCard;
import com.example.cardmasters.model.cards.FighterCard;

public class UIUtils {
    public static int getImageForCard(Context context, String cardId) {
        String resourceName = "im_" + cardId;
        int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

        if (resId == 0) {
            return R.drawable.ic_launcher_foreground; // Fallback
        }
        return resId;
    }

    public static View createViewCard(LayoutInflater layoutInflater, LinearLayout container, Card c, Context context){
        View cardView = layoutInflater.inflate(R.layout.item_fighter_lane, container, false);

        // Find the views inside the inflated card
        ImageView art = cardView.findViewById(R.id.card_image);
        TextView atkText = cardView.findViewById(R.id.atk_text);
        TextView hpText = cardView.findViewById(R.id.hp_text);
        TextView costText = cardView.findViewById(R.id.cost_text);

        // Set dimensions for the hand (usually taller than lanes)
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(220, 300);
        lp.setMargins(10, 0, 10, 0);
        cardView.setLayoutParams(lp);

        if (c instanceof FighterCard) {
            FighterCard fc = (FighterCard) c;
            int resId = context.getResources().getIdentifier("im_" + fc.getId(), "drawable", context.getOpPackageName());
            art.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_help);

            atkText.setText(String.valueOf(fc.getAtk()));
            hpText.setText(String.valueOf(fc.getHp()));
            costText.setText(String.valueOf(fc.getCost()));
            atkText.setVisibility(View.VISIBLE);
            hpText.setVisibility(View.VISIBLE);
            costText.setVisibility(View.VISIBLE);
        }
        if (c instanceof EffectCard){
            EffectCard ec= (EffectCard) c;
            int resId = context.getResources().getIdentifier("im_" + ec.getId(), "drawable", context.getOpPackageName());
            art.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_help);
            costText.setText(String.valueOf(ec.getCost()));
            costText.setVisibility(View.VISIBLE);
        }


        return cardView;
    }

    public static void setDragDropListener(View cardView, GameActivity gameActivity, Card c){
        cardView.setOnLongClickListener(v -> {
            if (gameActivity.getTurnSubmitted()) return false;
            if (gameActivity.getMoney()<c.getCost()) return false;

            // 2. Set the card on the activity instance
            if (c instanceof FighterCard) {
                gameActivity.setCurrentDraggingCard( (FighterCard) c);
            } else if (c instanceof EffectCard){
                gameActivity.setCurrentDraggingCard((EffectCard) c);
            }

            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);

            // 3. Pass 'v' (the view) as local state so ACTION_DRAG_ENDED can hide/show it
            v.startDragAndDrop(data, shadow, v, 0);
            v.setVisibility(View.INVISIBLE);
            return true;
        });
    }

}