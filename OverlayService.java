package com.example.pulseoverlay;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import java.util.*;

public class OverlayService extends Service {

    private WindowManager wm;
    private View iconView, panelView;
    private WindowManager.LayoutParams iconParams;
    private boolean opened = false;

    private float downX, downY;
    private int startX, startY;

    private final int BG = Color.rgb(12, 13, 19);
    private final int CARD = Color.rgb(22, 24, 34);
    private final int ACCENT = Color.rgb(125, 103, 255);
    private final int MUTED = Color.rgb(145, 150, 170);

    private LinearLayout content;
    private LinearLayout sidebar;
    private TextView[] tabs = new TextView[3];
    private String[] tabNames = {"Visuals", "HUD", "Utilities"};
    private int selectedTab = 0;

    private final Map<String, Boolean> switches = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        startNotification();
        createFloatingIcon();
    }

    private void startNotification() {
        String id = "gems_visuals";

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                id, "GemsVisuals", NotificationManager.IMPORTANCE_LOW
            );

            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
        }

        Notification.Builder builder =
            Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, id)
                : new Notification.Builder(this);

        builder.setContentTitle("GemsVisuals")
            .setContentText("Плавающее меню активно")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true);

        startForeground(42, builder.build());
    }

    private int dp(float value) {
        return (int) (value * getResources()
            .getDisplayMetrics().density + 0.5f);
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private void createFloatingIcon() {
        TextView icon = text("G", 21, Color.WHITE);
        icon.setGravity(Gravity.CENTER);
        icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        icon.setBackground(shape(0xFF7964FF, 50));
        icon.setElevation(dp(8));

        iconView = icon;

        iconParams = new WindowManager.LayoutParams(
            dp(54), dp(54), overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );

        iconParams.gravity = Gravity.TOP | Gravity.LEFT;
        iconParams.x = dp(16);
        iconParams.y = dp(180);

        icon.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getRawX();
                    downY = e.getRawY();
                    startX = iconParams.x;
                    startY = iconParams.y;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (e.getRawX() - downX);
                    int dy = (int) (e.getRawY() - downY);

                    if (Math.abs(dx) + Math.abs(dy) > dp(5)) {
                        iconParams.x = Math.max(0, startX + dx);
                        iconParams.y = Math.max(0, startY + dy);
                        try {
                            wm.updateViewLayout(iconView, iconParams);
                        } catch (Exception ignored) {}
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    float moved = Math.abs(e.getRawX() - downX)
                        + Math.abs(e.getRawY() - downY);

                    if (moved < dp(8)) {
                        if (opened) closePanel();
                        else showPanel();
                    }
                    return true;
            }
            return true;
        });

        wm.addView(iconView, iconParams);
    }

    private void showPanel() {
        if (opened) return;
        opened = true;

        FrameLayout root = new FrameLayout(this);

        ParticleView particles = new ParticleView();
        root.addView(particles, new FrameLayout.LayoutParams(
            -1, -1
        ));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setBackground(shape(BG, 20));
        card.setElevation(dp(18));

        int width = Math.min(
            getResources().getDisplayMetrics().widthPixels - dp(28),
            dp(440)
        );

        FrameLayout.LayoutParams cardParams =
            new FrameLayout.LayoutParams(
                width, dp(370), Gravity.CENTER
            );

        root.addView(card, cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView logo = text("G", 20, Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        logo.setBackground(shape(ACCENT, 10));

        header.addView(logo, new LinearLayout.LayoutParams(
            dp(38), dp(38)
        ));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);

        TextView title = text("GemsVisuals", 17, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView subtitle = text(
            "VISUAL CLIENT", 9, ACCENT
        );

        titles.addView(title);
        titles.addView(subtitle);

        header.addView(titles, new LinearLayout.LayoutParams(
            0, -2, 1
        ));

        TextView close = text("×", 27, Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setBackground(shape(CARD, 12));

        header.addView(close, new LinearLayout.LayoutParams(
            dp(38), dp(38)
        ));

        close.setOnClickListener(v -> closePanel());

        card.addView(header, new LinearLayout.LayoutParams(
            -1, dp(44)
        ));

        View divider = new View(this);
        divider.setBackgroundColor(0xFF292B3B);

        LinearLayout.LayoutParams dividerParams =
            new LinearLayout.LayoutParams(-1, dp(1));

        dividerParams.topMargin = dp(12);
        dividerParams.bottomMargin = dp(12);

        card.addView(divider, dividerParams);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.HORIZONTAL);

        sidebar = new LinearLayout(this);
        sidebar.setOrientation(LinearLayout.VERTICAL);
        sidebar.setBackground(shape(CARD, 13));
        sidebar.setPadding(dp(6), dp(6), dp(6), dp(6));

        String[] icons = {"◈", "▦", "⚙"};

        for (int i = 0; i < tabNames.length; i++) {
            final int index = i;

            TextView tab = text(
                icons[i] + "  " + tabNames[i],
                12, i == selectedTab ? Color.WHITE : MUTED
            );

            tab.setPadding(dp(8), 0, 0, 0);
            tab.setBackground(shape(
                i == selectedTab ? 0xFF38304F : Color.TRANSPARENT,
                10
            ));

            LinearLayout.LayoutParams tp =
                new LinearLayout.LayoutParams(-1, dp(44));

            tp.bottomMargin = dp(5);
            sidebar.addView(tab, tp);
            tabs[i] = tab;

            tab.setOnClickListener(v -> selectTab(index));
        }

        main.addView(sidebar, new LinearLayout.LayoutParams(
            dp(112), -1
        ));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(4), 0, 0);

        main.addView(content, new LinearLayout.LayoutParams(
            0, -1, 1
        ));

        LinearLayout.LayoutParams mainParams =
            new LinearLayout.LayoutParams(-1, 0, 1);

        card.addView(main, mainParams);

        TextView footer = text(
            "GEMSVISUALS  •  ANDROID", 9, MUTED
        );

        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams fp =
            new LinearLayout.LayoutParams(-1, dp(22));

        fp.topMargin = dp(8);
        card.addView(footer, fp);

        panelView = root;

        WindowManager.LayoutParams params =
            new WindowManager.LayoutParams(
                -1, -1, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            );

        params.gravity = Gravity.TOP | Gravity.LEFT;
        wm.addView(panelView, params);

        root.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                int[] location = new int[2];
                card.getLocationOnScreen(location);

                float x = e.getRawX();
                float y = e.getRawY();

                boolean inside =
                    x >= location[0]
                    && x <= location[0] + card.getWidth()
                    && y >= location[1]
                    && y <= location[1] + card.getHeight();

                if (!inside) {
                    closePanel();
                    return true;
                }
            }
            return false;
        });

        selectTab(selectedTab);

        card.setScaleX(0.88f);
        card.setScaleY(0.88f);
        card.setAlpha(0f);

        card.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(new DecelerateInterpolator())
            .start();
    }

    private void selectTab(int index) {
        selectedTab = index;

        for (int i = 0; i < tabs.length; i++) {
            boolean active = i == index;

            tabs[i].setTextColor(active ? Color.WHITE : MUTED);
            tabs[i].setBackground(shape(
                active ? 0xFF38304F : Color.TRANSPARENT,
                10
            ));

            tabs[i].animate()
                .alpha(active ? 1f : 0.65f)
                .setDuration(120)
                .start();
        }

        content.removeAllViews();

        String[] names;
        String[] descriptions;

        if (index == 0) {
            names = new String[] {
                "Animated particles",
                "Particle glow",
                "Menu animations",
                "Accent effects"
            };

            descriptions = new String[] {
                "Движущиеся точки и линии",
                "Свечение частиц",
                "Анимация интерфейса",
                "Подсветка элементов"
            };

        } else if (index == 1) {
            names = new String[] {
                "Target HUD",
                "Armor display",
                "Potion display",
                "Hit counter"
            };

            descriptions = new String[] {
                "Панель цели — макет",
                "Индикатор брони — макет",
                "Эффекты — макет",
                "Счётчик попаданий — макет"
            };

        } else {
            names = new String[] {
                "Purple theme",
                "Compact menu",
                "Floating button",
                "Animations"
            };

            descriptions = new String[] {
                "Фиолетовое оформление",
                "Компактное меню",
                "Плавающая кнопка",
                "Плавные переходы"
            };
        }

        TextView heading = text(
            tabNames[index], 16, Color.WHITE
        );

        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        content.addView(heading, new LinearLayout.LayoutParams(
            -1, dp(30)
        ));

        for (int i = 0; i < names.length; i++) {
            addSwitch(names[i], descriptions[i]);
        }
    }

    private void addSwitch(String name, String description) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(8), dp(4), dp(8), dp(4));
        row.setBackground(shape(CARD, 10));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView title = text(name, 11, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView sub = text(description, 9, MUTED);

        texts.addView(title);
        texts.addView(sub);

        row.addView(texts, new LinearLayout.LayoutParams(
            0, -2, 1
        ));

        final boolean[] enabled = {
            switches.containsKey(name)
                ? switches.get(name) : true
        };

        FrameLayout toggle = new FrameLayout(this);
        toggle.setBackground(shape(
            enabled[0] ? ACCENT : 0xFF414353, 20
        ));

        View knob = new View(this);
        knob.setBackground(shape(Color.WHITE, 20));

        FrameLayout.LayoutParams kp =
            new FrameLayout.LayoutParams(
                dp(16), dp(16),
                enabled[0] ? Gravity.RIGHT | Gravity.CENTER_VERTICAL
                           : Gravity.LEFT | Gravity.CENTER_VERTICAL
            );

        kp.leftMargin = dp(3);
        kp.rightMargin = dp(3);

        toggle.addView(knob, kp);

        row.addView(toggle, new LinearLayout.LayoutParams(
            dp(40), dp(22)
        ));

        row.setOnClickListener(v -> {
            enabled[0] = !enabled[0];
            switches.put(name, enabled[0]);

            toggle.animate()
                .alpha(0.7f)
                .setDuration(70)
                .withEndAction(() -> {
                    toggle.setBackground(shape(
                        enabled[0] ? ACCENT : 0xFF414353,
                        20
                    ));

                    FrameLayout.LayoutParams next =
                        (FrameLayout.LayoutParams)
                            knob.getLayoutParams();

                    next.gravity = enabled[0]
                        ? Gravity.RIGHT | Gravity.CENTER_VERTICAL
                        : Gravity.LEFT | Gravity.CENTER_VERTICAL;

                    knob.setLayoutParams(next);

                    toggle.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start();
                }).start();
        });

        LinearLayout.LayoutParams rp =
            new LinearLayout.LayoutParams(-1, dp(51));

        rp.bottomMargin = dp(6);
        content.addView(row, rp);
    }

    private class ParticleView extends View {
        private final Random random = new Random();
        private final float[] x = new float[42];
        private final float[] y = new float[42];
        private final float[] vx = new float[42];
        private final float[] vy = new float[42];

        private final Paint paint = new Paint(3);

        ParticleView() {
            super(OverlayService.this);

            for (int i = 0; i < x.length; i++) {
                x[i] = random.nextFloat();
                y[i] = random.nextFloat();
                vx[i] = (random.nextFloat() - 0.5f) * 0.0008f;
                vy[i] = (random.nextFloat() - 0.5f) * 0.0008f;
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float w = getWidth();
            float h = getHeight();

            paint.setStrokeWidth(dp(1));
            paint.setColor(0x667F70FF);

            for (int i = 0; i < x.length; i++) {
                x[i] += vx[i];
                y[i] += vy[i];

                if (x[i] < 0 || x[i] > 1) vx[i] = -vx[i];
                if (y[i] < 0 || y[i] > 1) vy[i] = -vy[i];

                for (int j = i + 1; j < x.length; j++) {
                    float dx = (x[i] - x[j]) * w;
                    float dy = (y[i] - y[j]) * h;

                    if (dx * dx + dy * dy < dp(105) * dp(105)) {
                        canvas.drawLine(
                            x[i] * w, y[i] * h,
                            x[j] * w, y[j] * h,
                            paint
                        );
                    }
                }

                paint.setColor(0xFFD5CCFF);
                canvas.drawCircle(
                    x[i] * w, y[i] * h, dp(1.5f), paint
                );

                paint.setColor(0x667F70FF);
            }

            postInvalidateDelayed(35);
        }
    }

    private void closePanel() {
        if (!opened) return;
        opened = false;

        if (panelView != null) {
            View old = panelView;
            panelView = null;

            old.animate()
                .alpha(0f)
                .setDuration(100)
                .withEndAction(() -> {
                    try {
                        wm.removeView(old);
                    } catch (Exception ignored) {}
                })
                .start();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        closePanel();

        try {
            if (iconView != null) wm.removeView(iconView);
        } catch (Exception ignored) {}

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
            }
