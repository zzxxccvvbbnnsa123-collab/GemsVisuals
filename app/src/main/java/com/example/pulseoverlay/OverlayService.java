package com.example.pulseoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class OverlayService extends Service {

    private WindowManager wm;
    private View floatingButton;
    private View menuView;
    private LinearLayout content;
    private TextView[] tabViews;
    private WindowManager.LayoutParams buttonParams;
    private int currentTab = 0;

    private final int PURPLE = Color.rgb(135, 105, 255);
    private final int BG = Color.rgb(13, 14, 21);
    private final int CARD = Color.rgb(28, 29, 41);
    private final int MUTED = Color.rgb(151, 153, 174);

    private final String[][] options = {
        {
            "Particles",
            "Animations",
            "Crosshair (demo)"
        },
        {
            "FPS Counter (demo)",
            "Target HUD (demo)",
            "Armor Display (demo)"
        },
        {
            "Menu effects",
            "Compact mode"
        }
    };

    private final Map<String, Boolean> enabled = new HashMap<>();

    private int dp(float value) {
        return (int) (
            value * getResources().getDisplayMetrics().density + 0.5f
        );
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView makeText(
        String value,
        int size,
        int color
    ) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL);
        return text;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        createNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Allow display over other apps first",
                Toast.LENGTH_LONG
            ).show();

            stopSelf();
            return;
        }

        createFloatingButton();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    private int windowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createNotification() {
        String channelId = "gemsvisuals_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                "GemsVisuals",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE
                );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        builder
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("GemsVisuals")
            .setContentText("Overlay is running")
            .setOngoing(true);

        startForeground(101, builder.build());
    }

    private void createFloatingButton() {
        TextView button = makeText("G", 23, Color.WHITE);

        button.setGravity(Gravity.CENTER);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackground(background(PURPLE, 30));
        button.setElevation(dp(8));

        buttonParams = new WindowManager.LayoutParams(
            dp(54),
            dp(54),
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        );

        buttonParams.gravity = Gravity.TOP | Gravity.START;
        buttonParams.x = dp(16);
        buttonParams.y = dp(160);

        button.setOnClickListener(v -> toggleMenu());

        try {
            wm.addView(button, buttonParams);
            floatingButton = button;
        } catch (RuntimeException e) {
            stopSelf();
        }
    }

    private void toggleMenu() {
        if (menuView == null) {
            showMenu();
        } else {
            hideMenu();
        }
    }

    private void showMenu() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(14));
        root.setBackground(background(BG, 20));
        root.setElevation(dp(12));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = makeText("GemsVisuals", 20, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(
            title,
            new LinearLayout.LayoutParams(0, dp(42), 1)
        );

        TextView close = makeText("✕", 20, MUTED);
        close.setGravity(Gravity.CENTER);
        close.setOnClickListener(v -> hideMenu());

        header.addView(close, new LinearLayout.LayoutParams(
            dp(42),
            dp(42)
        ));

        root.addView(header);

        TextView subtitle = makeText(
            "Overlay prototype",
            12,
            MUTED
        );

        root.addView(subtitle);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(12), 0, dp(12));

        String[] names = {"Visuals", "HUD", "Utilities"};
        tabViews = new TextView[names.length];

        for (int i = 0; i < names.length; i++) {
            final int index = i;

            TextView tab = makeText(names[i], 12, MUTED);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(4), dp(10), dp(4), dp(10));

            LinearLayout.LayoutParams tabParams =
                new LinearLayout.LayoutParams(0, dp(42), 1);

            tabParams.setMargins(dp(2), 0, dp(2), 0);
            tabs.addView(tab, tabParams);

            tabViews[i] = tab;

            tab.setOnClickListener(v -> {
                currentTab = index;
                updateTabs();
                refreshContent();
            });
        }

        root.addView(tabs);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        root.addView(
            content,
            new LinearLayout.LayoutParams(
                -1,
                0,
                1
            )
        );

        TextView note = makeText(
            "HUD and game features are visual demos.",
            10,
            MUTED
        );

        note.setPadding(0, dp(10), 0, 0);
        root.addView(note);

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                dp(310),
                dp(410),
                windowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.CENTER;

        try {
            wm.addView(root, params);
            menuView = root;

            updateTabs();
            refreshContent();
        } catch (RuntimeException e) {
            menuView = null;

            Toast.makeText(
                this,
                "Unable to open overlay menu",
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void updateTabs() {
        if (tabViews == null) {
            return;
        }

        for (int i = 0; i < tabViews.length; i++) {
            if (i == currentTab) {
                tabViews[i].setTextColor(Color.WHITE);
                tabViews[i].setBackground(background(PURPLE, 12));
            } else {
                tabViews[i].setTextColor(MUTED);
                tabViews[i].setBackground(background(CARD, 12));
            }
        }
    }

    private void refreshContent() {
        if (content == null) {
            return;
        }

        content.removeAllViews();

        for (String option : options[currentTab]) {
            addOption(option);
        }

        if (currentTab == 2) {
            TextView stop = makeText(
                "Stop overlay",
                14,
                Color.WHITE
            );

            stop.setGravity(Gravity.CENTER);
            stop.setBackground(background(
                Color.rgb(130, 45, 65),
                12
            ));

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(46));

            params.setMargins(0, dp(8), 0, 0);

            content.addView(stop, params);
            stop.setOnClickListener(v -> stopSelf());
        }
    }

    private void addOption(String name) {
        if (!enabled.containsKey(name)) {
            enabled.put(name, false);
        }

        boolean state = enabled.get(name);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), 0, dp(12), 0);
        row.setBackground(background(CARD, 12));

        LinearLayout.LayoutParams rowParams =
            new LinearLayout.LayoutParams(-1, dp(48));

        rowParams.setMargins(0, 0, 0, dp(8));

        TextView label = makeText(name, 13, Color.WHITE);

        row.addView(
            label,
            new LinearLayout.LayoutParams(0, -1, 1)
        );

        TextView value = makeText(
            state ? "ON" : "OFF",
            12,
            state ? PURPLE : MUTED
        );

        value.setTypeface(null, Typeface.BOLD);
        row.addView(value);

        row.setOnClickListener(v -> {
            boolean next = !enabled.get(name);
            enabled.put(name, next);

            value.setText(next ? "ON" : "OFF");
            value.setTextColor(next ? PURPLE : MUTED);
        });

        content.addView(row, rowParams);
    }

    private void hideMenu() {
        if (menuView != null) {
            try {
                wm.removeView(menuView);
            } catch (RuntimeException ignored) {
                // View may already have been removed.
            }

            menuView = null;
            content = null;
            tabViews = null;
        }
    }

    @Override
    public void onDestroy() {
        hideMenu();

        if (floatingButton != null) {
            try {
                wm.removeView(floatingButton);
            } catch (RuntimeException ignored) {
                // View may already have been removed.
            }

            floatingButton = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
