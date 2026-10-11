package com.example.pulseoverlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

public class OverlayService extends Service {

    private WindowManager wm;
    private WindowManager.LayoutParams buttonParams;
    private View floatingButton;
    private FrameLayout menu;
    private LinearLayout content;
    private TextView[] tabs;

    private SharedPreferences prefs;

    private int selectedTab = 0;
    private boolean closing = false;

    private final int PURPLE = Color.rgb(135, 105, 255);
    private final int BLUE = Color.rgb(65, 155, 255);
    private final int BG = Color.rgb(15, 16, 25);
    private final int CARD = Color.rgb(30, 31, 44);
    private final int MUTED = Color.rgb(155, 157, 177);

    private final String[] tabNames = {
            "Visuals", "HUD", "Utilities"
    };

    private final String[][] items = {
            {
                    "Particles",
                    "Trails (preview)",
                    "Custom Hand (preview)",
                    "Hitbox Customizer (preview)",
                    "Target ESP (preview)"
            },
            {
                    "FPS Counter (demo)",
                    "Target HUD (preview)",
                    "Armor Display (preview)"
            },
            {
                    "Menu effects",
                    "Compact mode",
                    "Swap Totem (not connected)"
            }
    };

    private int dp(float value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        prefs = getSharedPreferences(
                "gemsvisuals_settings",
                MODE_PRIVATE
        );

        createNotification();

        if (Build.VERSION.SDK_INT >= 23
                && !Settings.canDrawOverlays(this)) {
            Toast.makeText(
                    this,
                    "Разреши отображение поверх других приложений",
                    Toast.LENGTH_LONG
            ).show();
            stopSelf();
            return;
        }

        createButton();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        return START_NOT_STICKY;
    }

    private int getWindowType() {
        if (Build.VERSION.SDK_INT >= 26) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createNotification() {
        String channelId = "gemsvisuals";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "GemsVisuals",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= 26) {
            builder = new Notification.Builder(this, channelId);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.ic_menu_view);
        builder.setContentTitle("GemsVisuals");
        builder.setContentText("Overlay is running");
        builder.setOngoing(true);

        startForeground(101, builder.build());
    }

    private void createButton() {
        TextView button = text("G", 23, Color.WHITE);

        button.setGravity(Gravity.CENTER);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackground(shape(PURPLE, 28));
        button.setElevation(dp(6));

        buttonParams = new WindowManager.LayoutParams(
                dp(54),
                dp(54),
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        buttonParams.gravity = Gravity.TOP | Gravity.START;
        buttonParams.x = dp(16);
        buttonParams.y = dp(150);

        button.setOnTouchListener(new View.OnTouchListener() {
            private float startTouchX;
            private float startTouchY;
            private int startX;
            private int startY;
            private boolean moved;

            private final int touchSlop =
                    ViewConfiguration.get(
                            OverlayService.this
                    ).getScaledTouchSlop();

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startTouchX = event.getRawX();
                        startTouchY = event.getRawY();
                        startX = buttonParams.x;
                        startY = buttonParams.y;
                        moved = false;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - startTouchX;
                        float dy = event.getRawY() - startTouchY;

                        if (Math.abs(dx) > touchSlop
                                || Math.abs(dy) > touchSlop) {
                            moved = true;
                        }

                        if (moved) {
                            buttonParams.x = startX + (int) dx;
                            buttonParams.y = startY + (int) dy;

                            try {
                                wm.updateViewLayout(
                                        floatingButton,
                                        buttonParams
                                );
                            } catch (RuntimeException ignored) {
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            toggleMenu();
                        }
                        return true;
                }

                return true;
            }
        });

        try {
            floatingButton = button;
            wm.addView(button, buttonParams);

            button.setAlpha(0f);
            button.setScaleX(0.7f);
            button.setScaleY(0.7f);

            button.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

        } catch (RuntimeException e) {
            floatingButton = null;
            stopSelf();
        }
    }

    private void toggleMenu() {
        if (menu == null) {
            showMenu();
        } else {
            closeMenu();
        }
    }

    private void showMenu() {
        if (menu != null) {
            return;
        }

        closing = false;

        FrameLayout root = new FrameLayout(this);
        root.setBackground(shape(BG, 20));

        ParticleView particleView = new ParticleView();
        root.addView(
                particleView,
                new FrameLayout.LayoutParams(-1, -1)
        );

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(13), dp(12), dp(13), dp(10));
        panel.setBackground(
                shape(Color.argb(225, 15, 16, 25), 20)
        );

        root.addView(
                panel,
                new FrameLayout.LayoutParams(-1, -1)
        );

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text("GemsVisuals", 20, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(
                title,
                new LinearLayout.LayoutParams(0, dp(42), 1)
        );

        TextView close = text("✕", 19, Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setBackground(shape(CARD, 12));

        header.addView(
                close,
                new LinearLayout.LayoutParams(dp(40), dp(40))
        );

        close.setOnClickListener(v -> closeMenu());

        panel.addView(header);

        TextView subtitle = text(
                "VISUAL CONTROL PANEL",
                10,
                MUTED
        );

        subtitle.setPadding(dp(2), dp(3), 0, dp(7));
        panel.addView(subtitle);

        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(0, dp(5), 0, dp(10));

        tabs = new TextView[tabNames.length];

        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;

            TextView tab = text(tabNames[i], 12, MUTED);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(2), dp(8), dp(2), dp(8));

            LinearLayout.LayoutParams tabParams =
                    new LinearLayout.LayoutParams(0, dp(40), 1);

            tabParams.setMargins(dp(2), 0, dp(2), 0);
            tabRow.addView(tab, tabParams);
            tabs[i] = tab;

            tab.setOnClickListener(v -> {
                if (selectedTab != index) {
                    selectedTab = index;
                    updateTabs();
                    refreshItems();
                }
            });
        }

        panel.addView(tabRow);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        scroll.addView(
                content,
                new ScrollView.LayoutParams(-1, -2)
        );

        panel.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1)
        );

        TextView footer = text(
                "GEMSVISUALS  •  ANDROID OVERLAY",
                9,
                MUTED
        );

        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, 0);
        panel.addView(footer);

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        dp(320),
                        dp(440),
                        getWindowType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity = Gravity.CENTER;

        try {
            wm.addView(root, params);
            menu = root;

            updateTabs();
            refreshItems();

            root.setAlpha(0f);
            root.setScaleX(0.88f);
            root.setScaleY(0.88f);

            root.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();

        } catch (RuntimeException e) {
            menu = null;
            content = null;
            tabs = null;

            Toast.makeText(
                    this,
                    "Не удалось открыть меню",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void updateTabs() {
        if (tabs == null) {
            return;
        }

        for (int i = 0; i < tabs.length; i++) {
            TextView tab = tabs[i];

            if (i == selectedTab) {
                tab.setTextColor(Color.WHITE);
                tab.setBackground(shape(PURPLE, 11));
            } else {
                tab.setTextColor(MUTED);
                tab.setBackground(shape(CARD, 11));
            }

            tab.setAlpha(0.7f);
            tab.animate().alpha(1f).setDuration(150).start();
        }
    }

    private void refreshItems() {
        if (content == null) {
            return;
        }

        content.removeAllViews();

        for (String name : items[selectedTab]) {
            addItem(name);
        }

        if (selectedTab == 2) {
            addStopButton();
        }

        content.setAlpha(0f);
        content.animate().alpha(1f).setDuration(180).start();
    }

    private void addItem(String name) {
        boolean checked = prefs.getBoolean(name, false);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(4), dp(10), dp(4));
        row.setBackground(shape(CARD, 12));

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(-1, dp(54));

        rowParams.setMargins(0, 0, 0, dp(8));

        TextView label = text(name, 12, Color.WHITE);

        row.addView(
                label,
                new LinearLayout.LayoutParams(0, -1, 1)
        );

        SwitchView toggle = new SwitchView();
        toggle.setChecked(checked, false);

        row.addView(
                toggle,
                new LinearLayout.LayoutParams(dp(44), dp(27))
        );

        row.setOnClickListener(v -> {
            boolean next = !prefs.getBoolean(name, false);

            prefs.edit().putBoolean(name, next).apply();
            toggle.setChecked(next, true);

            row.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(70)
                    .withEndAction(() ->
                            row.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start()
                    )
                    .start();

            if (name.contains("preview")
                    || name.contains("demo")
                    || name.contains("not connected")) {
                Toast.makeText(
                        this,
                        "Игровая функция пока не подключена",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        content.addView(row, rowParams);

        row.setAlpha(0f);
        row.animate().alpha(1f).setDuration(180).start();
    }

    private void addStopButton() {
        TextView stop = text("Stop overlay", 13, Color.WHITE);
        stop.setGravity(Gravity.CENTER);
        stop.setBackground(shape(Color.rgb(130, 45, 65), 12));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(44));

        params.setMargins(0, dp(4), 0, dp(6));

        content.addView(stop, params);
        stop.setOnClickListener(v -> stopSelf());
    }

    private void closeMenu() {
        if (menu == null || closing) {
            return;
        }

        closing = true;

        FrameLayout oldMenu = menu;

        oldMenu.animate()
                .alpha(0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    try {
                        wm.removeView(oldMenu);
                    } catch (RuntimeException ignored) {
                    }

                    if (menu == oldMenu) {
                        menu = null;
                        content = null;
                        tabs = null;
                    }

                    closing = false;
                })
                .start();
    }

    private class SwitchView extends View {
        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private float progress = 0f;

        SwitchView() {
            super(OverlayService.this);
        }

        void setChecked(boolean checked, boolean animate) {
            float target = checked ? 1f : 0f;

            if (!animate) {
                progress = target;
                invalidate();
                return;
            }

            android.animation.ValueAnimator animator =
                    android.animation.ValueAnimator.ofFloat(
                            progress,
                            target
                    );

            animator.setDuration(180);

            animator.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            });

            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();
            float padding = dp(3);

            paint.setColor(Color.rgb(55, 57, 72));

            canvas.drawRoundRect(
                    0, 0, width, height,
                    height / 2f, height / 2f,
                    paint
            );

            paint.setColor(PURPLE);

            canvas.drawRoundRect(
                    0, 0, width * progress, height,
                    height / 2f, height / 2f,
                    paint
            );

            float radius = (height - padding * 2) / 2f;

            float x = padding + radius
                    + (width - padding * 2 - radius * 2)
                    * progress;

            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    x,
                    height / 2f,
                    radius,
                    paint
            );
        }
    }

    private class ParticleView extends View {
        private final Paint paint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Random random = new Random();
        private final ArrayList<Particle> particles =
                new ArrayList<>();

        private boolean running = false;

        private class Particle {
            float x;
            float y;
            float dx;
            float dy;
            float radius;

            Particle(float width, float height) {
                x = random.nextFloat() * width;
                y = random.nextFloat() * height;

                dx = (random.nextFloat() - 0.5f) * dp(0.7f);
                dy = (random.nextFloat() - 0.5f) * dp(0.7f);

                radius = dp(1.5f + random.nextFloat() * 1.5f);
            }
        }

        ParticleView() {
            super(OverlayService.this);
        }

        @Override
        protected void onSizeChanged(
                int width,
                int height,
                int oldWidth,
                int oldHeight
        ) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);

            particles.clear();

            if (width > 0 && height > 0) {
                for (int i = 0; i < 24; i++) {
                    particles.add(new Particle(width, height));
                }
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            running = true;
            postInvalidateDelayed(32);
        }

        @Override
        protected void onDetachedFromWindow() {
            running = false;
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float width = getWidth();
            float height = getHeight();

            if (width <= 0 || height <= 0) {
                return;
            }

            float maxDistance = dp(85);

            paint.setStrokeWidth(dp(0.7f));

            for (int i = 0; i < particles.size(); i++) {
                Particle a = particles.get(i);

                for (int j = i + 1; j < particles.size(); j++) {
                    Particle b = particles.get(j);

                    float dx = a.x - b.x;
                    float dy = a.y - b.y;

                    float distance = (float) Math.sqrt(
                            dx * dx + dy * dy
                    );

                    if (distance < maxDistance) {
                        int alpha = (int) (
                                45 * (1f - distance / maxDistance)
                        );

                        paint.setColor(Color.argb(
                                alpha, 100, 120, 255
                        ));

                        canvas.drawLine(
                                a.x, a.y,
                                b.x, b.y,
                                paint
                        );
                    }
                }
            }

            for (int i = 0; i < particles.size(); i++) {
                Particle p = particles.get(i);

                paint.setColor(
                        i % 2 == 0 ? PURPLE : BLUE
                );

                canvas.drawCircle(
                        p.x, p.y, p.radius, paint
                );

                p.x += p.dx;
                p.y += p.dy;

                if (p.x < 0 || p.x > width) {
                    p.dx = -p.dx;
                }

                if (p.y < 0 || p.y > height) {
                    p.dy = -p.dy;
                }

                p.x = Math.max(0, Math.min(width, p.x));
                p.y = Math.max(0, Math.min(height, p.y));
            }

            if (running) {
                postInvalidateDelayed(32);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (menu != null) {
            menu.animate().cancel();

            try {
                wm.removeView(menu);
            } catch (RuntimeException ignored) {
            }

            menu = null;
        }

        if (floatingButton != null) {
            floatingButton.animate().cancel();

            try {
                wm.removeView(floatingButton);
            } catch (RuntimeException ignored) {
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
