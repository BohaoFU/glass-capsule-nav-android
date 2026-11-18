package io.github.bohaofu.glasscapsulenav;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import eightbitlab.com.blurview.BlurAlgorithm;
import eightbitlab.com.blurview.RenderScriptBlur;

/**
 * RenderEffect-based blur tuned for capsule glass effect.
 * Uses CLAMP edges to avoid mirrored artifacts.
 */
@RequiresApi(api = Build.VERSION_CODES.S)
final class ClampRenderEffectBlur implements BlurAlgorithm {

    private static final float SCALE_FACTOR = 4f;

    private final RenderNode node = new RenderNode("CapsuleBlurNode");
    private final Context appContext;
    private final ColorMatrixColorFilter saturation;
    private int height;
    private int width;
    private float lastBlurRadius = Float.NaN;
    @Nullable
    private BlurAlgorithm fallbackAlgorithm;

    ClampRenderEffectBlur(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(1.45f);
        saturation = new ColorMatrixColorFilter(matrix);
    }

    @Override
    public Bitmap blur(@NonNull Bitmap bitmap, float blurRadius) {
        if (blurRadius != lastBlurRadius) {
            lastBlurRadius = blurRadius;
            RenderEffect blur = RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP);
            node.setRenderEffect(RenderEffect.createColorFilterEffect(saturation, blur));
        }
        if (bitmap.getHeight() != height || bitmap.getWidth() != width) {
            height = bitmap.getHeight();
            width = bitmap.getWidth();
            node.setPosition(0, 0, width, height);
        }
        Canvas canvas = node.beginRecording();
        canvas.drawBitmap(bitmap, 0, 0, null);
        node.endRecording();
        return bitmap;
    }

    @Override
    public void destroy() {
        node.discardDisplayList();
        if (fallbackAlgorithm != null) {
            fallbackAlgorithm.destroy();
        }
    }

    @Override
    public boolean canModifyBitmap() {
        return true;
    }

    @NonNull
    @Override
    public Bitmap.Config getSupportedBitmapConfig() {
        return Bitmap.Config.ARGB_8888;
    }

    @Override
    public float scaleFactor() {
        return SCALE_FACTOR;
    }

    @Override
    public void render(@NonNull Canvas canvas, @NonNull Bitmap bitmap) {
        if (canvas.isHardwareAccelerated()) {
            canvas.drawRenderNode(node);
            return;
        }
        if (fallbackAlgorithm == null) {
            fallbackAlgorithm = new RenderScriptBlur(appContext);
        }
        fallbackAlgorithm.blur(bitmap, lastBlurRadius);
        fallbackAlgorithm.render(canvas, bitmap);
    }
}
