package com.orbitaldelta.gpscompass;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int LOCATION_REQUEST = 1001;
    private static final int NOTIFICATION_REQUEST = 1002;
    private TextView status;
    private boolean serviceStartIssued;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        if (!serviceStartIssued
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && CompassWidgetProvider.hasWidgets(this)) {
            serviceStartIssued = true;
            CompassGpsService.start(this);
        }
    }

    private void buildUi() {
        int navy = 0xff061425;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(32, 24, 32, 24);
        root.setBackgroundColor(navy);

        TextView title = new TextView(this);
        title.setText("GPS Compass");
        title.setTextColor(0xffffffff);
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("O rumo usa apenas a direção GPS de deslocação e atualiza quando o veículo circula a pelo menos 1 m/s. O widget marca as coordenadas antigas quando o sinal deixa de ser recente.");
        description.setTextColor(0xffcbd5e1);
        description.setTextSize(17);
        description.setPadding(0, 12, 0, 16);
        root.addView(description);

        status = new TextView(this);
        status.setTextColor(0xff7dd3fc);
        status.setTextSize(17);
        status.setPadding(0, 0, 0, 16);
        root.addView(status);

        Button enable = new Button(this);
        enable.setText("Ativar localização GPS");
        enable.setOnClickListener(v -> requestNeededPermissions());
        root.addView(enable, new LinearLayout.LayoutParams(-2, -2));

        TextView addWidget = new TextView(this);
        addWidget.setText("Depois, adicione o widget GPS Compass ao ecrã inicial do Nova Launcher. Toque no widget para iniciar o GPS; remover o último widget pára o serviço.");
        addWidget.setTextColor(0xff94a3b8);
        addWidget.setTextSize(15);
        addWidget.setPadding(0, 16, 0, 0);
        root.addView(addWidget);
        setContentView(root);
    }

    private void requestNeededPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
        } else {
            requestNotificationsIfNeeded();
        }
    }

    private void requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        } else {
            startIfWidgetExists();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                requestNotificationsIfNeeded();
            } else {
                status.setText("É necessária localização precisa para receber direção GPS.");
            }
        } else if (requestCode == NOTIFICATION_REQUEST) {
            startIfWidgetExists();
        }
    }

    private void startIfWidgetExists() {
        ComponentName component = new ComponentName(this, CompassWidgetProvider.class);
        int[] widgetIds = AppWidgetManager.getInstance(this).getAppWidgetIds(component);
        if (widgetIds.length == 0) {
            status.setText("Permissão concedida. Adicione o widget ao ecrã inicial para iniciar o GPS.");
        } else {
            CompassGpsService.start(this);
            status.setText("A iniciar localização GPS para o widget.");
        }
    }

    private void updateStatus() {
        if (status == null) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status.setText("A aplicação precisa de localização precisa.");
            return;
        }
        if (CompassWidgetProvider.hasWidgets(this)) status.setText("Widget instalado. A localização é partilhada apenas com o widget.");
        else status.setText("Adicione o widget ao ecrã inicial para ativar o GPS.");
    }
}
