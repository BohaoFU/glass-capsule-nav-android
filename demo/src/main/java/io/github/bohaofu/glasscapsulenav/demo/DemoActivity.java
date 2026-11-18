package io.github.bohaofu.glasscapsulenav.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.bohaofu.glasscapsulenav.GlassNavBar;
import io.github.bohaofu.glasscapsulenav.GlassNavTab;

public final class DemoActivity extends Activity {
    private static final int[] DESTINATIONS = {
            R.id.tab_home, R.id.tab_messages, R.id.tab_notifications, R.id.tab_profile
    };

    private final PalettePanel[] panels = new PalettePanel[3];
    private GlassNavBar bar;
    private LinearLayout content;
    private TextView currentTab;
    private int destination;
    private int addedPanels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setDecorFitsSystemWindows(false);
        setContentView(R.layout.activity_demo);

        View root = findViewById(R.id.demo_root);
        ViewGroup backdrop = findViewById(R.id.backdrop);
        content = findViewById(R.id.demo_content);
        currentTab = findViewById(R.id.current_tab);
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
            findViewById(DESTINATIONS[i]).setOnClickListener(view -> showTab(destinationIndex, true));
        }
        findViewById(R.id.tab_add).setOnClickListener(view ->
                new AlertDialog.Builder(this)
                        .setMessage(R.string.add_color_panel)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.add, (dialog, which) -> {
                            addPanel(destination + panels.length + addedPanels++);
                            backdrop.post(() -> backdrop.scrollTo(0, content.getHeight()));
                        })
                        .show());

        showTab(savedInstanceState == null ? 0 : savedInstanceState.getInt("destination"), false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("destination", destination);
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
