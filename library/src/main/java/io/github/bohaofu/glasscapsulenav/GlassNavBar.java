package io.github.bohaofu.glasscapsulenav;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.PathInterpolator;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.BlurView;
import eightbitlab.com.blurview.RenderScriptBlur;

/** Draws the shared glass indicator between the blurred surface and the tab row. */
public final class GlassNavBar extends BlurView {
    private static final PathInterpolator DEPART = new PathInterpolator(.35f, .05f, .28f, 1f);
    private static final PathInterpolator TRAVEL = new PathInterpolator(.20f, .65f, .33f, 1f);
    private static final PathInterpolator SETTLE = new PathInterpolator(.22f, .61f, .36f, 1f);
    private static final PathInterpolator GLINT = new PathInterpolator(.42f, 0f, .58f, 1f);
    private static final float REST_GLINT_ALPHA = .48f;

    private final Drawable indicator;
    private final Paint glintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path capsule = new Path();
    private final RectF capsuleBounds = new RectF();
    private final float density;
    private ViewGroup tabs;
    private GlassNavTab selectedTab;
    private ValueAnimator movement;
    private float targetCenterX;
    private int targetWidth;
    private int targetHeight;
    private float centerX;
    private float heightScale = 1f;
    private float highlightX;
    private float highlightAlpha = REST_GLINT_ALPHA;

    public GlassNavBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        indicator = context.getDrawable(R.drawable.bg_capsule_nav_item_active).mutate();
        int glint = context.getColor(R.color.glass_nav_indicator_glint);
        glintPaint.setShader(new RadialGradient(0f, 0f, 1f, glint, glint & 0x00FFFFFF,
                Shader.TileMode.CLAMP));
        int shadow = context.getColor(R.color.glass_nav_shadow);
        shadowPaint.setColor((shadow & 0x00FFFFFF) | 0x0A000000);
        shadowPaint.setShadowLayer(3f * density, 0f, 1.5f * density, shadow);
        // Keep the shadow visible even when the glass itself is translucent on a white page.
        setOutlineAmbientShadowColor(shadow | 0xFF000000);
        setOutlineSpotShadowColor(shadow | 0xFF000000);
        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getHeight() / 2f);
                outline.setAlpha(1f);
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        tabs = findViewById(R.id.glass_nav_tabs);
    }

    /**
     * Samples the views drawn behind this bar. Put the backdrop and this bar in separate
     * branches of the same window. The optional drawable fills transparent backdrop pixels.
     */
    public void setBackdrop(@NonNull ViewGroup backdrop, @Nullable Drawable clearDrawable) {
        BlurAlgorithm blur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? new ClampRenderEffectBlur(getContext())
                : new RenderScriptBlur(getContext());
        setupWith(backdrop, blur)
                .setFrameClearDrawable(clearDrawable)
                .setBlurRadius(Math.min(25f, 15f * density / blur.scaleFactor()))
                .setOverlayColor(Color.TRANSPARENT)
                .setBlurAutoUpdate(true);
    }

    /** Moves the shared capsule to a tab in this bar. Pass null to clear the selection. */
    public void selectTab(@Nullable GlassNavTab tab, boolean animated) {
        if (selectedTab == tab) {
            return;
        }
        if (selectedTab != null) {
            selectedTab.setHighlighted(false, animated);
        }
        selectedTab = tab;
        if (selectedTab != null) {
            selectedTab.setHighlighted(true, animated);
            moveIndicator(animated && ValueAnimator.areAnimatorsEnabled());
        } else {
            cancelMovement();
            targetWidth = 0;
            targetHeight = 0;
            invalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (selectedTab == null || selectedTab.getWidth() <= 0) {
            return;
        }
        if (targetCenterX != tabCenter() || targetWidth != pillWidth() || targetHeight != tabs.getHeight()) {
            moveIndicator(false);
        }
    }

    private float tabCenter() {
        return tabs.getLeft() + selectedTab.getLeft() + selectedTab.getWidth() / 2f;
    }

    private int pillWidth() {
        return Math.max(1, tabs.getWidth() / tabs.getChildCount() + Math.round(2f * density));
    }

    private void moveIndicator(boolean animated) {
        if (selectedTab.getWidth() <= 0 || tabs.getHeight() <= 0) {
            return;
        }

        boolean hadIndicator = targetWidth > 0;
        cancelMovement();
        targetCenterX = tabCenter();
        targetWidth = pillWidth();
        targetHeight = tabs.getHeight();
        indicator.setBounds(0, 0, targetWidth, targetHeight);
        capsuleBounds.set(0, 0, targetWidth, targetHeight);
        float radius = targetHeight / 2f;
        capsule.reset();
        capsule.addRoundRect(capsuleBounds, radius, radius, Path.Direction.CW);
        if (!animated || !hadIndicator) {
            snapToTarget();
            return;
        }

        float startCenter = centerX;
        float startHeightScale = heightScale;
        float startHighlightX = highlightX;
        float startHighlightAlpha = highlightAlpha;
        float distance = targetCenterX - startCenter;
        float direction = Math.signum(distance);
        float travelCenter = startCenter + distance * .42f;
        float overshootCenter = targetCenterX + direction * 3f * density;
        movement = ValueAnimator.ofFloat(0f, 1f);
        movement.setDuration(470);
        movement.setInterpolator(null);
        movement.addUpdateListener(animator -> {
            float time = (float) animator.getAnimatedValue();
            if (time <= .35f) {
                float t = DEPART.getInterpolation(time / .35f);
                centerX = lerp(startCenter, travelCenter, t);
                heightScale = lerp(startHeightScale, .91f, t);
            } else if (time <= .77f) {
                float t = TRAVEL.getInterpolation((time - .35f) / .42f);
                centerX = lerp(travelCenter, overshootCenter, t);
                heightScale = lerp(.91f, 1.03f, t);
            } else {
                float t = SETTLE.getInterpolation((time - .77f) / .23f);
                centerX = lerp(overshootCenter, targetCenterX, t);
                heightScale = lerp(1.03f, 1f, t);
            }
            float glintPeak = direction * .14f;
            if (time <= .4f) {
                float t = GLINT.getInterpolation(time / .4f);
                highlightX = lerp(startHighlightX, glintPeak, t);
                highlightAlpha = lerp(startHighlightAlpha, .95f, t);
            } else {
                float t = GLINT.getInterpolation((time - .4f) / .6f);
                highlightX = lerp(glintPeak, 0f, t);
                highlightAlpha = lerp(.95f, REST_GLINT_ALPHA, t);
            }
            invalidate();
        });
        movement.start();
    }

    private void snapToTarget() {
        centerX = targetCenterX;
        heightScale = 1f;
        highlightX = 0f;
        highlightAlpha = REST_GLINT_ALPHA;
        invalidate();
    }

    private void cancelMovement() {
        if (movement != null) {
            movement.cancel();
            movement = null;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (targetWidth > 0 && targetHeight > 0) {
            int save = canvas.save();
            canvas.translate(centerX - targetWidth / 2f,
                    tabs.getTop() + (targetHeight - targetHeight * heightScale) / 2f);
            canvas.scale(1f, heightScale);
            canvas.drawPath(capsule, shadowPaint);
            indicator.draw(canvas);
            canvas.clipPath(capsule);
            canvas.translate(targetWidth * (.34f + highlightX), 0);
            canvas.scale(targetWidth * .65f, targetHeight * .65f);
            glintPaint.setAlpha(Math.round(highlightAlpha * 255f));
            canvas.drawCircle(0f, 0f, 1f, glintPaint);
            canvas.restoreToCount(save);
        }
        super.dispatchDraw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        cancelMovement();
        snapToTarget();
        super.onDetachedFromWindow();
    }

    private static float lerp(float start, float end, float time) {
        return start + (end - start) * time;
    }
}
