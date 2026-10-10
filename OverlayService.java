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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Random;

public class OverlayService extends Service {
    private WindowManager wm;
    private WindowManager.LayoutParams iconParams, panelParams;
    private View iconView, panelView, outsideView;
    private boolean open = false;
    private float downX, downY;
    private int startX, startY;
    private final int dark = Color.rgb(38, 39, 46);
    private final int card = Color.rgb(49, 50, 59);
    private final int accent = Color.rgb(181, 161, 255);

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForegroundNotification();
        createFloatingIcon();
    }
    private void startForegroundNotification() {
        String id = "pulse_overlay_running";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(id, "Pulse Overlay", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, id) : new Notification.Builder(this);
        b.setContentTitle("Pulse Overlay запущен").setContentText("Нажмите плавающую кнопку, чтобы открыть меню").setSmallIcon(android.R.drawable.ic_menu_view).setOngoing(true);
        startForeground(42, b.build());
    }
    private int overlayType() { return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE; }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(radius); return d;
    }
    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER); return t;
    }
    private void createFloatingIcon() {
        TextView icon = label("P", 20, Color.WHITE); icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD); icon.setBackground(shape(0xFF8170D8, 100));
        icon.setElevation(12);
        iconView = icon;
        iconParams = new WindowManager.LayoutParams(dp(54), dp(54), overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        iconParams.gravity = Gravity.TOP | Gravity.LEFT; iconParams.x = dp(18); iconParams.y = dp(180);
        icon.setOnTouchListener((v, e) -> {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN: downX = e.getRawX(); downY = e.getRawY(); startX = iconParams.x; startY = iconParams.y; return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int)(e.getRawX() - downX), dy = (int)(e.getRawY() - downY);
                    if (Math.abs(dx) + Math.abs(dy) > dp(5)) { iconParams.x = Math.max(0, startX + dx); iconParams.y = Math.max(0, startY + dy); wm.updateViewLayout(iconView, iconParams); }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (Math.abs(e.getRawX() - downX) + Math.abs(e.getRawY() - downY) < dp(8)) togglePanel(); return true;
            }
            return false;
        });
        wm.addView(iconView, iconParams);
    }
    private void togglePanel() { if (open) closePanel(); else showPanel(); }
    private void showPanel() {
        if (open) return;
        open = true;
        outsideView = new View(this); outsideView.setBackgroundColor(Color.TRANSPARENT);
        WindowManager.LayoutParams outside = new WindowManager.LayoutParams(-1, -1, overlayType(), WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        outside.gravity = Gravity.TOP | Gravity.LEFT;
        outsideView.setOnTouchListener((v,e) -> { if (e.getAction() == MotionEvent.ACTION_DOWN) closePanel(); return true; });
        wm.addView(outsideView, outside);

        FrameLayout frame = new FrameLayout(this); frame.setClipChildren(false); frame.setClipToPadding(false);
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(16), dp(14), dp(16), dp(14)); body.setBackground(shape(dark, dp(20))); body.setElevation(dp(18));
        TextView heading = label("PULSE VISUALS", 15, Color.WHITE); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD); heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); heading.setPadding(dp(4), 0, 0, dp(10)); body.addView(heading, new LinearLayout.LayoutParams(-1, dp(34)));
        TextView line = label("ВИЗУАЛЬНЫЙ МЕНЮ", 10, accent); line.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); body.addView(line, new LinearLayout.LayoutParams(-1, dp(22)));
        addItem(body, "HUD", "Интерфейс и индикаторы");
        addItem(body, "Particles", "Декоративные частицы");
        addItem(body, "Animations", "Анимация меню");
        addItem(body, "Settings", "Настройки оверлея");
        TextView foot = label("Прототип · функции пока визуальные", 10, 0xFF9B9CA7); foot.setPadding(0, dp(10), 0, 0); body.addView(foot, new LinearLayout.LayoutParams(-1, dp(24)));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(280), -2, Gravity.CENTER);
        frame.addView(body, bp);
        addParticles(frame);
        panelView = frame;
        panelParams = new WindowManager.LayoutParams(-1, -1, overlayType(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT);
        panelParams.gravity = Gravity.TOP | Gravity.LEFT;
        wm.addView(panelView, panelParams);
        body.setScaleX(0.88f); body.setScaleY(0.88f); body.setAlpha(0f);
        body.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(170).setInterpolator(new DecelerateInterpolator()).start();
    }
    private void addItem(LinearLayout parent, String title, String subtitle) {
        LinearLayout item = new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setPadding(dp(12), dp(7), dp(10), dp(7)); item.setBackground(shape(card, dp(12)));
        TextView t = label(title, 14, Color.WHITE); t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); t.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        TextView s = label(subtitle, 10, 0xFFB8B9C4); s.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL); s.setPadding(0, dp(2), 0, 0);
        item.addView(t, new LinearLayout.LayoutParams(-1, dp(22))); item.addView(s, new LinearLayout.LayoutParams(-1, dp(18)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54)); p.topMargin = dp(7); parent.addView(item, p);
    }
    private void addParticles(FrameLayout frame) {
        Random r = new Random(8);
        for (int i=0; i<18; i++) {
            View dot = new View(this); int size = dp(2 + r.nextInt(4)); dot.setBackground(shape(i%3==0 ? 0xFFD2C5FF : 0x887F6BDD, size*2));
            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(size, size);
            // Decorative dots kept near the sides so the central menu stays readable.
            p.leftMargin = i%2==0 ? dp(r.nextInt(42)) : getResources().getDisplayMetrics().widthPixels - dp(42 + r.nextInt(30));
            p.topMargin = dp(100 + r.nextInt(Math.max(100, getResources().getDisplayMetrics().heightPixels/dp(1)-200)));
            frame.addView(dot, p);
            dot.setAlpha(0.2f); dot.animate().alpha(0.85f).setDuration(500 + r.nextInt(800)).withEndAction(() -> dot.animate().alpha(0.25f).setDuration(900).start()).start();
        }
    }
    private void closePanel() {
        if (!open) return; open = false;
        try { if (panelView != null) wm.removeView(panelView); } catch (Exception ignored) {}
        try { if (outsideView != null) wm.removeView(outsideView); } catch (Exception ignored) {}
        panelView = null; outsideView = null;
    }
    private int dp(float n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public void onDestroy() { closePanel(); try { if (iconView != null) wm.removeView(iconView); } catch (Exception ignored) {} super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
