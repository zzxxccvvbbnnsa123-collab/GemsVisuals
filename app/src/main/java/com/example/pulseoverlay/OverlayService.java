package com.example.pulseoverlay;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class OverlayService extends Service {

    private WindowManager wm;
    private View floatingButton, menuView;
    private WindowManager.LayoutParams buttonParams;
    private boolean menuOpen = false;

    private final int PURPLE = Color.rgb(135, 105, 255);
    private final int BG = Color.rgb(13, 14, 21);
    private final int CARD = Color.rgb(28, 29, 41);
    private final int MUTED = Color.rgb(151, 153, 174);

    private final Map<String, Boolean> enabled = new HashMap<>();

    private LinearLayout content;
    private TextView[] tabViews = new TextView[3];
    private int currentTab = 0;

    private int dp(float v) {
        return (int) (v * getResources()
            .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        createNotification();
        createFloatingButton();
    }

    private int windowType() {
        if (Build.VERSION.SDK_INT >= 26) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createNotification() {
        String channelId = "gemsvisuals_channel";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                "GemsVisuals",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                (NotificationManager) getSystemService(
                    NOTIFICATION_SERVICE
                );

            manager.createNotificationChannel(channel);
        }

        Notification.Builder builder =
            Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, channelId)
                : new Notification.Builder(this);

        builder.setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("GemsVisuals")
            .setContentText("Floating menu is running")
            .setOngoing(true);

        startForeground(101, builder.build());
    }

    private void createFloatingButton() {
        TextView button = text("G", 23, Color.WHITE);

        button.setGravity(Gravity.CENTER);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackground(bg(PURPLE, 30));
        button.setElevation(dp(8));

        buttonParams = new WindowManager.LayoutParams(
            dp(54),
            dp(54),
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        buttonParams.gravity = Gravity.TOP | Gravity.LEFT;
        button
