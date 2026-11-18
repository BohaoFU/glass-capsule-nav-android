package io.github.bohaofu.glasscapsulenav;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.PathParser;

/** A navigation item with a static badge and an independently animated glyph. */
public final class GlassNavTab extends FrameLayout {
    private final CharSequence labelText;
    private final Glyph glyph;
    private boolean glyphPressed;

    public GlassNavTab(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.GlassNavTab);
        labelText = attributes.getText(R.styleable.GlassNavTab_glassNavLabel);
        int icon = attributes.getInt(R.styleable.GlassNavTab_glassNavIcon, 0);
        Drawable image = attributes.getDrawable(R.styleable.GlassNavTab_glassNavDrawable);
        attributes.recycle();
        glyph = new Glyph(context, icon);
        glyph.setImageDrawable(image);
        setContentDescription(labelText);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // The optional XML child is the badge anchored to the glyph.
        View badge = getChildAt(0);
        if (badge != null) {
            removeView(badge);
        }

        LinearLayout column = new LinearLayout(getContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        column.setClipChildren(false);
        column.setClipToPadding(false);

        FrameLayout iconFrame = new FrameLayout(getContext());
        iconFrame.setClipChildren(false);
        iconFrame.setClipToPadding(false);
        iconFrame.addView(glyph, new LayoutParams(dp(20), dp(20), Gravity.CENTER));
        if (badge != null) {
            iconFrame.addView(badge);
        }
        column.addView(iconFrame, new LinearLayout.LayoutParams(dp(21), dp(21)));

        TextView label = new TextView(getContext());
        label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        label.setText(labelText);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        label.setTextColor(glyph.ink);
        label.setIncludeFontPadding(false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setEllipsize(TextUtils.TruncateAt.END);
        label.setAutoSizeTextTypeUniformWithConfiguration(8, 10, 1, TypedValue.COMPLEX_UNIT_SP);
        LinearLayout.LayoutParams labelParams =
                new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(3);
        labelParams.leftMargin = dp(4);
        labelParams.rightMargin = dp(4);
        column.addView(label, labelParams);

        setClipChildren(false);
        setClipToPadding(false);
        addView(column, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    void setHighlighted(boolean selected, boolean animate) {
        if (isSelected() == selected) {
            return;
        }
        setSelected(selected);
        glyph.select(selected, animate && ValueAnimator.areAnimatorsEnabled());
    }

    /** The image view used by this tab. Clearing its drawable restores the built-in glyph. */
    public ImageView getIconImageView() {
        return glyph;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        boolean pressed = isPressed();
        if (glyph == null || glyphPressed == pressed) {
            return;
        }
        glyphPressed = pressed;
        glyph.animate().cancel();
        float scale = pressed ? .9f : 1f;
        if (ValueAnimator.areAnimatorsEnabled()) {
            glyph.animate().scaleX(scale).scaleY(scale).setDuration(140).start();
        } else {
            glyph.setScaleX(scale);
            glyph.setScaleY(scale);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        glyph.animate().cancel();
        glyph.setScaleX(1f);
        glyph.setScaleY(1f);
        glyphPressed = false;
        glyph.finishSelection(isSelected());
        super.onDetachedFromWindow();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class Glyph extends ImageView {
        private static final float SIZE = 24f;
        private static final float SELECTED_SCALE = 1.08f;
        private static final PathInterpolator EASE_OUT = new PathInterpolator(0f, 0f, .58f, 1f);
        private static final PathInterpolator BOUNCE = new PathInterpolator(.18f, .7f, .36f, 1f);
        private static final PathInterpolator FILL_IN = new PathInterpolator(.24f, .7f, .28f, 1f);
        private static final PathInterpolator FILL_OUT = new PathInterpolator(.4f, 0f, .65f, 1f);

        private final int ink;
        private final int selectedInk;
        private final int selectedFill;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path outline = new Path();
        private final Path solid = new Path();
        private ValueAnimator animation;
        private boolean selected;
        private float animatedScale = 1f;
        private float fill;

        Glyph(Context context, int icon) {
            super(context);
            ink = context.getColor(R.color.glass_nav_ink);
            selectedInk = context.getColor(R.color.glass_nav_selected_ink);
            selectedFill = context.getColor(R.color.glass_nav_selected_fill);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            paint.setStrokeWidth(1.8f);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            buildIcon(icon);
        }

        // Lucide icon geometry. See assets/licenses/lucide.txt.
        private void buildIcon(int icon) {
            switch (icon) {
                case 1:
                    setBody("M2.992 16.342a2 2 0 0 1 .094 1.167l-1.065 3.29a1 1 0 0 0 1.236 1.168"
                            + "l3.413-.998a2 2 0 0 1 1.099.092 10 10 0 1 0-4.777-4.719");
                    break;
                case 2:
                    outline.addCircle(12, 12, 10, Path.Direction.CW);
                    outline.addPath(path("M8 12h8 M12 8v8"));
                    break;
                case 3:
                    setBody("M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673"
                            + "C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326");
                    outline.addPath(path("M10.268 21a2 2 0 0 0 3.464 0"));
                    break;
                case 4:
                    setBody("M20 21a8 8 0 0 0-16 0");
                    outline.addCircle(12, 8, 5, Path.Direction.CW);
                    solid.addCircle(12, 8, 5, Path.Direction.CW);
                    break;
                default:
                    setBody("M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0"
                            + "l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z");
                    outline.addPath(path("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8"));
                    solid.addRect(9, 12, 15, 21, Path.Direction.CW);
                    solid.setFillType(Path.FillType.EVEN_ODD);
                    break;
            }
        }

        private void setBody(String pathData) {
            Path body = path(pathData);
            outline.addPath(body);
            solid.addPath(body);
        }

        private static Path path(String pathData) {
            return PathParser.createPathFromPathData(pathData);
        }

        void select(boolean selected, boolean animate) {
            if (!animate || !isLaidOut()) {
                finishSelection(selected);
                return;
            }
            cancelAnimation();
            this.selected = selected;
            float startScale = animatedScale;
            float startFill = fill;
            animation = ValueAnimator.ofFloat(0f, 1f);
            animation.setInterpolator(null);
            animation.setDuration(selected ? 420 : 270);
            animation.addUpdateListener(animator -> {
                float time = (float) animator.getAnimatedValue();
                if (selected) {
                    if (time <= .16f) {
                        animatedScale = lerp(startScale, .92f, EASE_OUT.getInterpolation(time / .16f));
                    } else if (time <= .57f) {
                        animatedScale = lerp(.92f, 1.24f, BOUNCE.getInterpolation((time - .16f) / .41f));
                    } else {
                        animatedScale = lerp(1.24f, SELECTED_SCALE,
                                EASE_OUT.getInterpolation((time - .57f) / .43f));
                    }
                    fill = lerp(startFill, 1f, FILL_IN.getInterpolation(
                            clamp((time * 420f - 90f) / 240f)));
                } else {
                    animatedScale = time <= .46f
                            ? lerp(startScale, .88f, EASE_OUT.getInterpolation(time / .46f))
                            : lerp(.88f, 1f, EASE_OUT.getInterpolation((time - .46f) / .54f));
                    fill = lerp(startFill, 0f, FILL_OUT.getInterpolation(clamp(time * 270f / 165f)));
                }
                invalidate();
            });
            animation.start();
        }

        void finishSelection(boolean selected) {
            cancelAnimation();
            this.selected = selected;
            animatedScale = selected ? SELECTED_SCALE : 1f;
            fill = selected ? 1f : 0f;
            invalidate();
        }

        private void cancelAnimation() {
            if (animation != null) {
                animation.cancel();
                animation = null;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int save = canvas.save();
            canvas.scale(animatedScale, animatedScale, getWidth() / 2f, getHeight() / 2f);
            if (getDrawable() != null) {
                super.onDraw(canvas);
            } else {
                canvas.scale(getWidth() / SIZE, getHeight() / SIZE);
                paint.setColor(selected ? selectedInk : ink);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawPath(outline, paint);
                if (fill > 0f) {
                    canvas.clipRect(0f, SIZE * (1f - fill), SIZE, SIZE);
                    paint.setColor(selectedFill);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawPath(solid, paint);
                }
            }
            canvas.restoreToCount(save);
        }

        private static float clamp(float value) {
            return Math.max(0f, Math.min(1f, value));
        }

        private static float lerp(float start, float end, float time) {
            return start + (end - start) * time;
        }
    }
}
