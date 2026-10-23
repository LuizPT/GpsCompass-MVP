package com.orbitaldelta.gpscompass;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CompassWidgetProvider extends AppWidgetProvider {
    public static final String PREFS = "compass_state";
    public static final String KEY_DIRECTION = "direction";
    public static final String KEY_DEGREES = "degrees";
    public static final String KEY_BEARING_VALUE = "bearing_value";
    public static final String KEY_HAS_BEARING = "has_bearing";
    public static final String KEY_HEADING_CURRENT = "heading_current";
    public static final String KEY_GPS_STATE = "gps_state";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_HAS_LOCATION = "has_location";
    public static final String KEY_HAS_ACCURACY = "has_accuracy";
    public static final String KEY_ACCURACY = "accuracy";
    public static final String KEY_LOCATION_TIME = "location_time";
    public static final String KEY_FIX_ELAPSED_NANOS = "fix_elapsed_nanos";
    public static final String KEY_TRACKING_ENABLED = "tracking_enabled";

    static final String STATE_PERMISSION = "permission";
    static final String STATE_DISABLED = "disabled";
    static final String STATE_SEARCHING = "searching";
    static final String STATE_NO_SIGNAL = "no_signal";
    static final String STATE_ACTIVE = "active";
    static final String STATE_STOPPED = "stopped";
    static final String STATE_BACKGROUND_PERMISSION = "background_permission";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        renderAll(context);
    }

    @Override
    public void onDisabled(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_TRACKING_ENABLED, false)
                .putString(KEY_GPS_STATE, STATE_STOPPED)
                .apply();
        context.stopService(new Intent(context, CompassGpsService.class));
    }

    public static boolean hasWidgets(Context context) {
        ComponentName component = new ComponentName(context, CompassWidgetProvider.class);
        return AppWidgetManager.getInstance(context).getAppWidgetIds(component).length > 0;
    }

    public static void renderAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        ComponentName component = new ComponentName(context, CompassWidgetProvider.class);
        for (int id : manager.getAppWidgetIds(component)) {
            renderOne(context, manager, id);
        }
    }

    private static void renderOne(Context context, AppWidgetManager manager, int widgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_compass);

        boolean hasBearing = prefs.getBoolean(KEY_HAS_BEARING, false);
        String direction = hasBearing ? prefs.getString(KEY_DIRECTION, "--") : "--";
        String degrees = hasBearing ? prefs.getString(KEY_DEGREES, "--°") : "--°";
        views.setTextViewText(R.id.widget_direction, direction);
        views.setTextViewText(R.id.widget_degrees, degrees);
        boolean headingCurrent = hasBearing && prefs.getBoolean(KEY_HEADING_CURRENT, false);
        views.setTextViewText(R.id.widget_heading_status, headingCurrent ? "Rumo GPS atual" : (hasBearing ? "Último rumo" : "Sem rumo GPS"));
        if (hasBearing) {
            float bearing = prefs.getFloat(KEY_BEARING_VALUE, 0f);
            String compassDescription = (headingCurrent ? "Rumo GPS atual: " : "Último rumo: ")
                    + degrees + " " + direction;
            views.setViewVisibility(R.id.widget_compass_pointer, android.view.View.VISIBLE);
            views.setFloat(R.id.widget_compass_pointer, "setRotation", bearing);
            views.setFloat(R.id.widget_compass_pointer, "setAlpha", headingCurrent ? 1f : 0.45f);
            views.setContentDescription(R.id.widget_compass_face, compassDescription);
        } else {
            views.setViewVisibility(R.id.widget_compass_pointer, android.view.View.GONE);
            views.setContentDescription(R.id.widget_compass_face, "Sem rumo GPS disponível");
        }

        boolean hasLocation = prefs.getBoolean(KEY_HAS_LOCATION, false);
        long fixElapsed = prefs.getLong(KEY_FIX_ELAPSED_NANOS, 0L);
        boolean fresh = hasLocation && CompassLogic.isFresh(SystemClock.elapsedRealtimeNanos(), fixElapsed);
        if (hasLocation) {
            String ageLabel = fresh ? "atual" : "antiga";
            String latitude = CompassLogic.formatLatitude(Double.longBitsToDouble(prefs.getLong(KEY_LATITUDE, Double.doubleToRawLongBits(Double.NaN))));
            String longitude = CompassLogic.formatLongitude(Double.longBitsToDouble(prefs.getLong(KEY_LONGITUDE, Double.doubleToRawLongBits(Double.NaN))));
            views.setTextViewText(R.id.widget_latitude, "Latitude " + ageLabel + ": " + latitude);
            views.setTextViewText(R.id.widget_longitude, "Longitude " + ageLabel + ": " + longitude);
            boolean hasAccuracy = prefs.getBoolean(KEY_HAS_ACCURACY, false);
            float accuracy = prefs.getFloat(KEY_ACCURACY, Float.NaN);
            String accuracyText = hasAccuracy ? CompassLogic.formatAccuracy(accuracy) : "Precisão indisponível";
            long locationTime = prefs.getLong(KEY_LOCATION_TIME, 0L);
            String timeText = locationTime > 0L
                    ? new SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(new Date(locationTime))
                    : "hora indisponível";
            views.setTextViewText(R.id.widget_location_detail, (fresh ? "Precisão " : "Última posição · precisão ") + accuracyText + " · " + timeText);
        } else {
            views.setTextViewText(R.id.widget_latitude, "Latitude: --");
            views.setTextViewText(R.id.widget_longitude, "Longitude: --");
            views.setTextViewText(R.id.widget_location_detail, "Sem coordenadas recebidas");
        }

        String gpsState = prefs.getString(KEY_GPS_STATE, STATE_STOPPED);
        String status;
        if (fresh) {
            status = "Localização atual · GPS";
        } else if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || STATE_PERMISSION.equals(gpsState)) {
            status = "Permissão de localização precisa necessária";
        } else if (STATE_BACKGROUND_PERMISSION.equals(gpsState)) {
            status = "Permita localização sempre para reiniciar o GPS";
        } else if (STATE_DISABLED.equals(gpsState)) {
            status = hasLocation ? "GPS indisponível · coordenadas antigas" : "GPS indisponível";
        } else if (STATE_STOPPED.equals(gpsState)) {
            status = hasLocation ? "GPS parado · coordenadas antigas" : "GPS parado · toque para iniciar";
        } else if (hasLocation) {
            status = "Coordenadas antigas · sem sinal GPS";
        } else if (STATE_NO_SIGNAL.equals(gpsState)) {
            status = "Sem sinal GPS";
        } else {
            status = "À procura de sinal GPS";
        }
        views.setTextViewText(R.id.widget_status, status);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(context, widgetId * 10 + 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, openPending);

        bindDialButton(context, views, R.id.widget_emergency, widgetId, WidgetActionSpec.EMERGENCY_NUMBER,
                "112 — Emergência");
        bindDialButton(context, views, R.id.widget_fire, widgetId, WidgetActionSpec.FIRE_ALERT_NUMBER,
                "117 — Alerta Incêndios");

        Intent copyIntent = new Intent(context, CopyCoordinatesActivity.class);
        PendingIntent copyPending = PendingIntent.getActivity(context, widgetId * 10 + 4, copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_copy, copyPending);
        manager.updateAppWidget(widgetId, views);
    }

    private static void bindDialButton(Context context, RemoteViews views, int viewId, int widgetId,
                                       String number, String label) {
        Intent confirmIntent = new Intent(context, DialConfirmationActivity.class)
                .putExtra(DialConfirmationActivity.EXTRA_NUMBER, number)
                .putExtra(DialConfirmationActivity.EXTRA_LABEL, label);
        PendingIntent pending = PendingIntent.getActivity(context, widgetId * 10 + (number.equals("112") ? 2 : 3),
                confirmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(viewId, pending);
    }
}
