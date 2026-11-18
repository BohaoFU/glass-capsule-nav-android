package io.github.bohaofu.glasscapsulenav.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;

final class PalettePanel extends View {
    private static final int[][] PALETTES = {
            {0xFFFBE7CB, 0xFFC2D9CC, 0xFF385C6C, 0xFF9ABDB9},
            {0xFFDBE5FA, 0xFF5E70A0, 0xFFC97676, 0xFFF5C2A7},
            {0xFFF3DBC1, 0xFFD98B68, 0xFF6A4749, 0xFFE9B287},
            {0xFFCFDFEB, 0xFF698BAA, 0xFF536A59, 0xFFC0DAAE}
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path boundsPath = new Path();
    private final Path curve = new Path();
    private final RectF bounds = new RectF();
    private final float cornerRadius;
    private Shader background;
    private Shader ribbon;
    private int palette;

    PalettePanel(Context context) {
        super(context);
        cornerRadius = 22f * getResources().getDisplayMetrics().density;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void setPalette(int index) {
        palette = index % PALETTES.length;
        updateShaders(getWidth(), getHeight());
        invalidate();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        bounds.set(0, 0, width, height);
        boundsPath.reset();
        boundsPath.addRoundRect(bounds, cornerRadius, cornerRadius, Path.Direction.CW);

        curve.reset();
        curve.moveTo(0, height * .65f);
        curve.cubicTo(width * .33f, height * .23f, width * .47f, height * 1.08f, width, height * .42f);
        curve.lineTo(width, height);
        curve.lineTo(0, height);
        curve.close();

        updateShaders(width, height);
    }

    private void updateShaders(int width, int height) {
        if (width == 0 || height == 0) {
            return;
        }
        int[] colors = PALETTES[palette];
        background = new LinearGradient(0, 0, width, height, colors[0], colors[1], Shader.TileMode.CLAMP);
        ribbon = new LinearGradient(width * .14f, height, width * .72f, height * .35f,
                colors[2], colors[3], Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(boundsPath);
        paint.setShader(background);
        canvas.drawRect(bounds, paint);
        paint.setShader(ribbon);
        canvas.drawPath(curve, paint);
        canvas.restoreToCount(save);
    }
}
