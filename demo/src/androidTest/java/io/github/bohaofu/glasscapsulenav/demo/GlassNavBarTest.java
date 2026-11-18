package io.github.bohaofu.glasscapsulenav.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import io.github.bohaofu.glasscapsulenav.GlassNavBar;
import io.github.bohaofu.glasscapsulenav.GlassNavTab;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class GlassNavBarTest {
    private GlassNavBar bar;
    private Bitmap expected;

    @Test
    public void equalSlotsAndRapidSwitchesSettleAtTheLastTab() {
        var instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(instrumentation.getTargetContext(), R.style.Theme_GlassCapsuleDemo);
            View root = LayoutInflater.from(context).inflate(R.layout.activity_demo, null);
            float density = context.getResources().getDisplayMetrics().density;
            int width = Math.round(320 * density);
            int height = Math.round(640 * density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, height);

            bar = root.findViewById(R.id.glass_bar);
            ViewGroup tabs = (ViewGroup) bar.getChildAt(0);
            GlassNavTab add = root.findViewById(R.id.tab_add);
            assertEquals(5, tabs.getChildCount());
            for (int i = 0; i < tabs.getChildCount(); i++) {
                assertTrue(Math.abs(tabs.getChildAt(i).getWidth() - add.getWidth()) <= 1);
            }
            assertTrue(Math.abs(add.getLeft() + add.getRight() - tabs.getWidth()) <= 2);

            GlassNavTab home = root.findViewById(R.id.tab_home);
            GlassNavTab profile = root.findViewById(R.id.tab_profile);
            bar.selectTab(profile, false);
            expected = drawBar(bar);

            bar.selectTab(home, false);
            bar.selectTab(root.findViewById(R.id.tab_messages), true);
            bar.selectTab(root.findViewById(R.id.tab_notifications), true);
            bar.selectTab(profile, true);
            assertTrue(profile.isSelected());
            assertTrue(!home.isSelected() && !add.isSelected());
        });

        SystemClock.sleep(650);
        instrumentation.runOnMainSync(() -> assertTrue(expected.sameAs(drawBar(bar))));
    }

    private static Bitmap drawBar(View bar) {
        Bitmap image = Bitmap.createBitmap(bar.getWidth(), bar.getHeight(), Bitmap.Config.ARGB_8888);
        bar.draw(new Canvas(image));
        return image;
    }
}
