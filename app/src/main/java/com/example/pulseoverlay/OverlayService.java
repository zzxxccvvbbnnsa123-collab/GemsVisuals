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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class OverlayService extends Service {

    private WindowManager wm;
    private View floatingButton;
    private FrameLayout menuView;
    private LinearLayout content;
    private TextView[] tabViews;

    private WindowManager.LayoutParams buttonParams;

    private int currentTab = 0;
    private boolean closingMenu = false;

    private final int PURPLE = Color.rgb(135, 105, 255);
    private final int BLUE = Color.rgb(65, 155, 255);
    private final int BG = Color.rgb(13, 14, 21);
    private final int CARD = Color.rgb(28, 29, 41);
    private final int MUTED = Color.rgb(151, 153, 174);

    private final String PREFS_NAME = "gemsvisuals_settings";

    private SharedPreferences prefs;

    private final Map<String, Boolean> enabled =
            new HashMap<>();

    private final String[][] options = {
            {
                    "Particles (menu)",
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

    private final String[] tabNames = {
            "Visuals",
            "HUD",
            "Utilities"
    };

    private int dp(float value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density + 0.5f
        );
    }

    private GradientDrawable background(
            int color,
            int radius
    ) {
        GradientDrawable drawable =
                new GradientDrawable();

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

        wm = (WindowManager)
                getSystemService(WINDOW_SERVICE);

        prefs = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        createNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {

            Toast.makeText(
                    this,
                    "Разреши отображение поверх приложений",
                    Toast.LENGTH_LONG
            ).show();

            stopSelf();
            return;
        }

        createFloatingButton();
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {
        return START_NOT_STICKY;
    }

    private int windowType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams
                    .TYPE_APPLICATION_OVERLAY;
        }

        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void createNotification() {
        String channelId = "gemsvisuals_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(
                    this,
                    channelId
            );
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

    // Плавающая кнопка G

    private void createFloatingButton() {
        TextView button = makeText(
                "G",
                23,
                Color.WHITE
        );

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

        button.setOnTouchListener(
                new View.OnTouchListener() {

                    private float downX;
                    private float downY;
                    private int startX;
                    private int startY;
                    private boolean dragging;
                    private final int touchSlop =
                            ViewConfiguration.get(
                                    OverlayService.this
                            ).getScaledTouchSlop();

                    @Override
                    public boolean onTouch(
                            View view,
                            MotionEvent event
                    ) {
                        switch (event.getAction()) {

                            case MotionEvent.ACTION_DOWN:

                                downX = event.getRawX();
                                downY = event.getRawY();

                                startX = buttonParams.x;
                                startY = buttonParams.y;

                                dragging = false;

                                return true;

                            case MotionEvent.ACTION_MOVE:

                                float dx =
                                        event.getRawX() - downX;

                                float dy =
                                        event.getRawY() - downY;

                                if (Math.abs(dx) > touchSlop
                                        || Math.abs(dy) > touchSlop) {
                                    dragging = true;
                                }

                                if (dragging) {
                                    buttonParams.x =
                                            startX + (int) dx;

                                    buttonParams.y =
                                            startY + (int) dy;

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

                                if (!dragging) {
                                    toggleMenu();
                                }

                                return true;
                        }

                        return true;
                    }
                }
        );

        try {
            wm.addView(button, buttonParams);
            floatingButton = button;

            button.setAlpha(0f);
            button.setScaleX(0.6f);
            button.setScaleY(0.6f);

            button.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(350)
                    .setInterpolator(
                            new DecelerateInterpolator()
                    )
                    .start();

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

    // Создание меню

    private void showMenu() {
        if (menuView != null) {
            return;
        }

        closingMenu = false;

        FrameLayout root = new FrameLayout(this);

        root.setBackground(
                background(BG, 20)
        );

        root.setClipToOutline(true);

        ParticleBackground particles =
                new ParticleBackground(this);

        root.addView(
                particles,
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                )
        );

        LinearLayout panel = new LinearLayout(this);

        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
        );

        panel.setBackground(
                background(
                        Color.argb(225, 13, 14, 21),
                        20
                )
        );

        FrameLayout.LayoutParams panelParams =
                new FrameLayout.LayoutParams(
                        -1,
                        -1
                );

        root.addView(panel, panelParams);

        // Заголовок

        LinearLayout header = new LinearLayout(this);

        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = makeText(
                "GemsVisuals",
                20,
                Color.WHITE
        );

        title.setTypeface(null, Typeface.BOLD);

        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(40),
                        1
                )
        );

        TextView close = makeText(
                "✕",
                20,
                MUTED
        );

        close.setGravity(Gravity.CENTER);
        close.setBackground(background(CARD, 14));

        header.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(40)
                )
        );

        close.setOnClickListener(v -> hideMenu());

        panel.addView(header);

        TextView subtitle = makeText(
                "VISUAL CONTROL PANEL",
                10,
                MUTED
        );

        subtitle.setPadding(
                dp(2),
                dp(3),
                0,
                dp(4)
        );

        panel.addView(subtitle);

        // Вкладки

        LinearLayout tabs = new LinearLayout(this);

        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setPadding(0, dp(8), 0, dp(10));

        tabViews = new TextView[tabNames.length];

        for (int i = 0; i < tabNames.length; i++) {

            final int index = i;

            TextView tab = makeText(
                    tabNames[i],
                    12,
                    MUTED
            );

            tab.setGravity(Gravity.CENTER);
            tab.setPadding(
                    dp(3),
                    dp(9),
                    dp(3),
                    dp(9)
            );

            LinearLayout.LayoutParams tabParams =
                    new LinearLayout.LayoutParams(
                            0,
                            dp(40),
                            1
                    );

            tabParams.setMargins(
                    dp(2),
                    0,
                    dp(2),
                    0
            );

            tabs.addView(tab, tabParams);

            tabViews[i] = tab;

            tab.setOnClickListener(v -> {
                if (currentTab != index) {
                    currentTab = index;

                    updateTabs();
                    refreshContent();
                }
            });
        }

        panel.addView(tabs);

        // Прокручиваемая область

        ScrollView scroll = new ScrollView(this);

        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        scroll.addView(
                content,
                new ScrollView.LayoutParams(
                        -1,
                        -2
                )
        );

        panel.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1
                )
        );

        TextView footer = makeText(
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

            root.setAlpha(0f);
            root.setScaleX(0.88f);
            root.setScaleY(0.88f);

            root.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(220)
                    .setInterpolator(
                            new DecelerateInterpolator()
                    )
                    .start();

        } catch (RuntimeException e) {
            menuView = null;
            content = null;
            tabViews = null;

            Toast.makeText(
                    this,
                    "Не удалось открыть меню",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void updateTabs() {
        if (tabViews == null) {
            return;
        }

        for (int i = 0; i < tabViews.length; i++) {

            TextView tab = tabViews[i];

            if (i == currentTab) {
                tab.setTextColor(Color.WHITE);
                tab.setBackground(
                        background(PURPLE, 12)
                );
            } else {
                tab.setTextColor(MUTED);
                tab.setBackground(
                        background(CARD, 12)
                );
            }

            tab.animate()
                    .alpha(0.75f)
                    .setDuration(80)
                    .withEndAction(() ->
                            tab.animate()
                                    .alpha(1f)
                                    .setDuration(100)
                                    .start()
                    )
                    .start();
        }
    }

    // Обновление содержимого выбранной вкладки

    private void refreshContent() {
        if (content == null) {
            return;
        }

        content.animate()
                .alpha(0f)
                .setDuration(90)
                .withEndAction(() -> {

                    if (content == null || menuView == null) {
                        return;
                    }

                    content.removeAllViews();

                    for (String option : options[currentTab]) {
                        addOption(option);
                    }

                    if (currentTab == 2) {
                        addStopButton();
                    }

                    content.setAlpha(0f);

                    content.animate()
                            .alpha(1f)
                            .setDuration(180)
                            .start();
                })
                .start();
    }

    private void addOption(String name) {

        boolean state = prefs.getBoolean(name, false);

        enabled.put(name, state);

        LinearLayout row = new LinearLayout(this);

        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(
                dp(12),
                dp(4),
                dp(10),
                dp(4)
        );

        row.setBackground(background(CARD, 13));

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(54)
                );

        rowParams.setMargins(
                0,
                0,
                0,
                dp(8)
        );

        TextView label = makeText(
                name,
                12,
                Color.WHITE
        );

        row.addView(
                label,
                new LinearLayout.LayoutParams(
                        0,
                        -1,
                        1
                )
        );

        SwitchView toggle = new SwitchView(this);
        toggle.setChecked(state, false);

        row.addView(
                toggle,
                new LinearLayout.LayoutParams(
                        dp(44),
                        dp(28)
                )
        );

        row.setOnClickListener(v -> {

            boolean next = !enabled.get(name);

            enabled.put(name, next);

            prefs.edit()
                    .putBoolean(name, next)
                    .apply();

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

            showOptionInfo(name, next);
        });

        content.addView(row, rowParams);

        row.setAlpha(0f);

        row.animate()
                .alpha(1f)
                .setDuration(180)
                .start();
    }

    private void showOptionInfo(
            String name,
            boolean state
    ) {
        if (name.contains("preview")
                || name.contains("demo")
                || name.contains("not connected")) {

            Toast.makeText(
                    this,
                    "Переключатель интерфейса: "
                            + (state ? "ON" : "OFF")
                            + ". Игровая функция ещё не подключена.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void addStopButton() {
        TextView stop = makeText(
                "Stop overlay",
                13,
                Color.WHITE
        );

        stop.setGravity(Gravity.CENTER);
        stop.setBackground(
                background(
                        Color.rgb(130, 45, 65),
                        12
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(46)
                );

        params.setMargins(
                0,
                dp(4),
                0,
                dp(6)
        );

        content.addView(stop, params);

        stop.setOnClickListener(v -> stopSelf());
    }

    // Плавное закрытие меню

    private void hideMenu() {
        if (menuView == null || closingMenu) {
            return;
        }

        closingMenu = true;

        FrameLayout oldMenu = menuView;

        oldMenu.animate()
                .alpha(0f)
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(160)
                .withEndAction(() -> {

                    try {
                        wm.removeView(oldMenu);
                    } catch (RuntimeException ignored) {
                    }

                    if (menuView == oldMenu) {
                        menuView = null;
                        content = null;
                        tabViews = null;
                    }

                    closingMenu = false;
                })
                .start();
    }

    // Анимированный переключатель

    private class SwitchView extends View {

        private final Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        private float progress = 0f;

        private SwitchView(android.content.Context context) {
            super(context);
        }

        private void setChecked(
                boolean checked,
                boolean animate
        ) {
            float target = checked ? 1f : 0f;

            if (!animate) {
                progress = target;
                invalidate();
                return;
            }

            android.animation.ValueAnimator animator =
                    android.animation.ValueAnimator
                            .ofFloat(progress, target);

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

            float h = getHeight();
            float w = getWidth();

            float radius = h / 2f;

            int offColor = Color.rgb(55, 57, 72);

            int red = Color.red(offColor)
                    + (int) (
                    (Color.red(PURPLE) - Color.red(offColor))
                            * progress
            );

            int green = Color.green(offColor)
                    + (int) (
                    (Color.green(PURPLE) - Color.green(offColor))
                            * progress
            );

            int blue = Color.blue(offColor)
                    + (int) (
                    (Color.blue(PURPLE) - Color.blue(offColor))
                            * progress
            );

            paint.setColor(
                    Color.rgb(red, green, blue)
            );

            canvas.drawRoundRect(
                    0,
                    0,
                    w,
                    h,
                    radius,
                    radius,
                    paint
            );

            float padding = dp(3);

            float thumbX = padding
                    + (w - 2 * padding - (h - 2 * padding))
                    * progress
                    + (h - 2 * padding) / 2f;

            paint.setColor(Color.WHITE);

            canvas.drawCircle(
                    thumbX,
                    h / 2f,
                    (h - 2 * padding) / 2f,
                    paint
            );
        }
    }

    // Движущиеся частицы и соединяющие их линии

    private class ParticleBackground extends View {

        private final Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        private final Random random = new Random();

        private final ArrayList<Particle> particles =
                new ArrayList<>();

        private boolean running = false;

        private class Particle {
            float x;
            float y;
            float dx;
            float dy;
            float size;

            Particle(float width, float height) {
                x = random.nextFloat() * width;
                y = random.nextFloat() * height;

                dx = (random.nextFloat() - 0.5f) * dp(0.7f);
                dy = (random.nextFloat() - 0.5f) * dp(0.7f);

                size = dp(1.5f + random.nextFloat() * 2f);
            }
        }

        ParticleBackground(android.content.Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onSizeChanged(
                int width,
                int height,
                int oldWidth,
                int oldHeight
        ) {
            super.onSizeChanged(
                    width,
                    height,
                    oldWidth,
                    oldHeight
            );

            particles.clear();

            if (width > 0 &&
