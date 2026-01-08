package com.example.cardmasters.utils;

import android.content.Context;
import com.example.cardmasters.R;

public class ImageUtils {
    public static int getImageForCard(Context context, String cardId) {
        String resourceName = "im_" + cardId;
        int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());

        if (resId == 0) {
            return R.drawable.ic_launcher_foreground; // Fallback
        }
        return resId;
    }
}