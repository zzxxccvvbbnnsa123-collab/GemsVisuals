package com.example.pulseoverlay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 36, 28, 28);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackgroundColor(0xFFF4F4F7);
        TextView title = new TextView(this);
        title.setText("Pulse Overlay — прототип");
        title.setTextSize(24);
        title.setTextColor(0xFF202027);
        TextView desc = new TextView(this);
        desc.setText("Плавающая кнопка и компактное меню поверх Minecraft. Разрешение наложения нужно включить вручную.");
        desc.setTextSize(16);
        desc.setTextColor(0xFF5F606A);
        desc.setPadding(0, 16, 0, 24);
        Button permission = new Button(this);
        permission.setText("Разрешить отображение поверх приложений");
        permission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else startOverlay();
        });
        Button start = new Button(this);
        start.setText("Запустить оверлей");
        start.setOnClickListener(v -> startOverlay());
        root.addView(title);
        root.addView(desc);
        root.addView(permission);
        root.addView(start);
        setContentView(root);
    }
    private void startOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            return;
        }
        startService(new Intent(this, OverlayService.class));
        finish();
    }
}
