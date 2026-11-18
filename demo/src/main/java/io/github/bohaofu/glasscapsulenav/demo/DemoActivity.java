package io.github.bohaofu.glasscapsulenav.demo;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.bohaofu.glasscapsulenav.GlassNavBar;
import io.github.bohaofu.glasscapsulenav.GlassNavTab;

public final class DemoActivity extends Activity {
    private static final int[] DESTINATIONS = {
            R.id.tab_home, R.id.tab_messages, R.id.tab_notifications, R.id.tab_profile
    };
    private static final long PLAYBACK_STEP_MS = 700;
    private static final int PLAYBACK_IDLE = -1;

    private final PalettePanel[] panels = new PalettePanel[3];
    private final Runnable playbackStep = this::advancePlayback;
    private GlassNavBar bar;
    private LinearLayout content;
    private TextView currentTab;
    private Button playbackButton;
    private int destination;
    private int playbackStepsRemaining = PLAYBACK_IDLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(false);
        setContentView(R.layout.activity_demo);

        View root = findViewById(R.id.demo_root);
        ViewGroup backdrop = findViewById(R.id.backdrop);
        content = findViewById(R.id.demo_content);
        currentTab = findViewById(R.id.current_tab);
        playbackButton = findViewById(R.id.play_animation);
        bar = findViewById(R.id.glass_bar);
        bar.setBackdrop(backdrop, null);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            Insets system = insets.getInsets(WindowInsets.Type.systemBars());
            int bottom = Math.max(dp(24), system.bottom);
            backdrop.setPadding(0, system.top, 0, 0);
            content.setPadding(dp(22), dp(24), dp(22), bottom + dp(76));
            FrameLayout.LayoutParams barParams = (FrameLayout.LayoutParams) bar.getLayoutParams();
            if (barParams.bottomMargin != bottom) {
                barParams.bottomMargin = bottom;
                bar.setLayoutParams(barParams);
            }
            return insets;
        });

        for (int i = 0; i < panels.length; i++) {
            panels[i] = addPanel(i);
        }
        for (int i = 0; i < DESTINATIONS.length; i++) {
            int destinationIndex = i;
            findViewById(DESTINATIONS[i]).setOnClickListener(view -> {
                stopPlayback();
                showTab(destinationIndex, true);
            });
        }
        playbackButton.setOnClickListener(view -> {
            if (playbackStepsRemaining == PLAYBACK_IDLE) {
                startPlayback();
            } else {
                stopPlayback();
            }
        });

        showTab(savedInstanceState == null ? 0 : savedInstanceState.getInt("destination"), false);
    }

    @Override
    protected void onStop() {
        stopPlayback();
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("destination", destination);
    }

    private void startPlayback() {
        playbackStepsRemaining = DESTINATIONS.length;
        playbackButton.setText(R.string.stop_animation);
        playbackButton.postDelayed(playbackStep, 200);
    }

    private void advancePlayback() {
        if (playbackStepsRemaining <= 0) {
            stopPlayback();
            return;
        }
        showTab((destination + 1) % DESTINATIONS.length, true);
        playbackStepsRemaining--;
        playbackButton.postDelayed(playbackStep, PLAYBACK_STEP_MS);
    }

    private void stopPlayback() {
        playbackButton.removeCallbacks(playbackStep);
        playbackStepsRemaining = PLAYBACK_IDLE;
        playbackButton.setText(R.string.play_animation);
    }

    private PalettePanel addPanel(int palette) {
        PalettePanel panel = new PalettePanel(this);
        panel.setPalette(palette);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(242));
        params.topMargin = dp(16);
        content.addView(panel, params);
        return panel;
    }

    private void showTab(int index, boolean animated) {
        destination = index;
        GlassNavTab tab = findViewById(DESTINATIONS[index]);
        currentTab.setText(tab.getContentDescription());
        bar.selectTab(tab, animated);
        for (int i = 0; i < panels.length; i++) {
            panels[i].setPalette(index + i);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
