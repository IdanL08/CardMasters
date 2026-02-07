package com.example.cardmasters.utils;

import static androidx.appcompat.graphics.drawable.DrawableContainerCompat.Api21Impl.getResources;
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
import com.example.cardmasters.model.cards.FighterCard;

public class ImageUtils {
    public static int getImageForCard(Context context, String cardId) {
        String resourceName = "im_" + cardId;
        int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

        if (resId == 0) {
            return R.drawable.ic_launcher_foreground; // Fallback
        }
        return resId;
    }

    public static View createViewCard(LayoutInflater layoutInflater, LinearLayout handContainer, Card c, boolean turnSubmitted, Context context, GameActivity gameActivity){
        View cardView = layoutInflater.inflate(R.layout.item_fighter_lane, handContainer, false);

        // Find the views inside the inflated card
        ImageView art = cardView.findViewById(R.id.card_image);
        TextView atkText = cardView.findViewById(R.id.atk_text);
        TextView hpText = cardView.findViewById(R.id.hp_text);

        // Set dimensions for the hand (usually taller than lanes)
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(220, 300);
        lp.setMargins(10, 0, 10, 0);
        cardView.setLayoutParams(lp);

        if (c instanceof FighterCard) {
            FighterCard fc = (FighterCard) c;
            int resId = context.getResources().getIdentifier("im_" + fc.getId(), "drawable", getPackageName());
            art.setImageResource(resId != 0 ? resId : android.R.drawable.ic_menu_help);

            atkText.setText(String.valueOf(fc.getAtk()));
            hpText.setText(String.valueOf(fc.getHp()));
            atkText.setVisibility(View.VISIBLE);
            hpText.setVisibility(View.VISIBLE);
        }

        cardView.setOnLongClickListener(v -> {
            if (turnSubmitted) return false;
            gameActivity.setCurrentDraggingCard( (c instanceof FighterCard) ? (FighterCard) c : null;
            ClipData data = ClipData.newPlainText("", "");
            View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadow, v, 0);
            v.setVisibility(View.INVISIBLE);
            return true;
        });
        return cardView;
    }
}