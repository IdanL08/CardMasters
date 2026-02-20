package com.example.cardmasters.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.example.cardmasters.R;

public class AnimatedChestView extends View {

    private Bitmap spriteSheet;

    private int frameCount = 6; // update to match your sprite sheet
    private int currentFrame = 0;
    private int frameWidth, frameHeight;

    private long lastFrameTime = 0;
    private int frameDuration = 80;

    private int animationDirection = 1;
    private boolean isAnimating = false;

    private enum ChestState {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    private ChestState chestState = ChestState.CLOSED;

    public AnimatedChestView(Context context, AttributeSet attrs) {
        super(context, attrs);

        spriteSheet = BitmapFactory.decodeResource(getResources(), R.drawable.chests);//(4)

        frameWidth = spriteSheet.getWidth() / frameCount;
        frameHeight = spriteSheet.getHeight();
    }

    // =========================
    // OPEN
    // =========================
    public void openChest() {

        // If already open, do nothing
        if (chestState == ChestState.OPEN) return;

        animationDirection = 1;
        chestState = ChestState.OPENING;
        isAnimating = true;
        lastFrameTime = System.currentTimeMillis();
        invalidate();
    }

    // =========================
    // CLOSE
    // =========================
    public void closeChest() {

        // If already closed, do nothing
        if (chestState == ChestState.CLOSED) return;

        animationDirection = -1;
        chestState = ChestState.CLOSING;
        isAnimating = true;
        lastFrameTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // ---- Scale properly (no squish) ----
        float scale = Math.max(
                (float) getWidth() / frameWidth,
                (float) getHeight() / frameHeight
        );

        int drawWidth = (int) (frameWidth * scale);
        int drawHeight = (int) (frameHeight * scale);

        int left = (getWidth() - drawWidth) / 2;
        int top = (getHeight() - drawHeight) / 2;

        Rect src = new Rect(
                currentFrame * frameWidth,
                0,
                currentFrame * frameWidth + frameWidth,
                frameHeight
        );

        Rect dst = new Rect(left, top, left + drawWidth, top + drawHeight);

        canvas.drawBitmap(spriteSheet, src, dst, null);

        // ---- Animation Update ----
        if (isAnimating) {

            long now = System.currentTimeMillis();

            if (now - lastFrameTime > frameDuration) {

                lastFrameTime = now;
                currentFrame += animationDirection;

                // Finished Opening
                if (currentFrame >= frameCount - 1) {
                    currentFrame = frameCount - 1;
                    isAnimating = false;
                    chestState = ChestState.OPEN;
                }

                // Finished Closing
                if (currentFrame <= 0) {
                    currentFrame = 0;
                    isAnimating = false;
                    chestState = ChestState.CLOSED;
                }
            }

            invalidate();
        }
    }
}
