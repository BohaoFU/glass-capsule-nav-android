package io.github.bohaofu.glasscapsulenav.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import io.github.bohaofu.glasscapsulenav.GlassNavBar;
import io.github.bohaofu.glasscapsulenav.GlassNavTab;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
            GlassNavTab home = root.findViewById(R.id.tab_home);
            assertEquals(4, tabs.getChildCount());
            for (int i = 0; i < tabs.getChildCount(); i++) {
                assertTrue(Math.abs(tabs.getChildAt(i).getWidth() - home.getWidth()) <= 1);
            }

            GlassNavTab profile = root.findViewById(R.id.tab_profile);
            bar.selectTab(profile, false);
            expected = drawBar(bar);

            bar.selectTab(home, false);
            bar.selectTab(root.findViewById(R.id.tab_messages), true);
            bar.selectTab(root.findViewById(R.id.tab_notifications), true);
            bar.selectTab(profile, true);
            assertTrue(profile.isSelected());
            assertFalse(home.isSelected());
        });

        SystemClock.sleep(650);
        instrumentation.runOnMainSync(() -> assertTrue(expected.sameAs(drawBar(bar))));
    }

    @Test
    public void autoplayVisitsFourTabsAndManualSelectionStopsIt() throws InterruptedException {
        List<String> transitions = new CopyOnWriteArrayList<>();
        CountDownLatch lap = new CountDownLatch(4);
        try (ActivityScenario<DemoActivity> scenario = ActivityScenario.launch(DemoActivity.class)) {
            scenario.onActivity(activity -> {
                TextView title = activity.findViewById(R.id.current_tab);
                title.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence text, int start, int before, int count) {
                        transitions.add(text.toString());
                        lap.countDown();
                    }

                    @Override
                    public void afterTextChanged(Editable text) {}
                });
                Button play = activity.findViewById(R.id.play_animation);
                play.performClick();
                assertEquals(activity.getString(R.string.stop_animation), play.getText().toString());
            });
            assertTrue(lap.await(8, TimeUnit.SECONDS));
            assertEquals(List.of("Messages", "Notifications", "Profile", "Home"), transitions);

            scenario.onActivity(activity -> {
                Button play = activity.findViewById(R.id.play_animation);
                activity.findViewById(R.id.tab_notifications).performClick();
                assertEquals(activity.getString(R.string.play_animation), play.getText().toString());
            });
            SystemClock.sleep(800);
            scenario.onActivity(activity -> assertTrue(activity.findViewById(R.id.tab_notifications).isSelected()));
        }
    }

    @Test
    public void tabTitleAndCustomImageLoadFromXml() {
        try (ActivityScenario<DemoActivity> scenario = ActivityScenario.launch(DemoActivity.class)) {
            scenario.onActivity(activity -> {
                GlassNavTab tab = activity.findViewById(R.id.tab_profile);
                assertEquals(activity.getString(R.string.profile), tab.getContentDescription().toString());
                assertNotNull(tab.getIconImageView().getDrawable());
            });
        }
    }

    private static Bitmap drawBar(View bar) {
        Bitmap image = Bitmap.createBitmap(bar.getWidth(), bar.getHeight(), Bitmap.Config.ARGB_8888);
        bar.draw(new Canvas(image));
        return image;
    }
}
