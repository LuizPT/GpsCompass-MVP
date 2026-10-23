package com.orbitaldelta.gpscompass;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

public class CompassGpsService extends Service {
    public static final String ACTION_START = "com.orbitaldelta.gpscompass.START_GPS";
    public static final String ACTION_STOP = "com.orbitaldelta.gpscompass.STOP_GPS";
    private static final String CHANNEL_ID = "gps_compass_location";
    private static final int NOTIFICATION_ID = 3101;
    private static final String TAG = "GpsCompassService";
    private static final long LOCATION_UPDATE_MS = 3_000L;
    private static final long WIDGET_RENDER_MIN_MS = 3_000L;
    private static final long STATE_CHECK_MS = 5_000L;
    private static final long NO_SIGNAL_AFTER_MS = 30_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private LocationManager locationManager;
    private boolean updatesRequested;
    private long serviceStartedElapsed;
    private long lastWidgetRenderElapsed;

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            handleLocation(location);
        }

        @Override
        public void onProviderEnabled(String provider) {
            setGpsState(CompassWidgetProvider.STATE_SEARCHING, false);
        }

        @Override
        public void onProviderDisabled(String provider) {
            setGpsState(CompassWidgetProvider.STATE_DISABLED, true);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (!LocationManager.GPS_PROVIDER.equals(provider)) return;
            if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                SharedPreferences prefs = state();
                if (!prefs.getBoolean(CompassWidgetProvider.KEY_HAS_LOCATION, false)) {
                    setGpsState(CompassWidgetProvider.STATE_NO_SIGNAL, false);
                }
            }
        }
    };

    private final Runnable stateChecker = new Runnable() {
        @Override
        public void run() {
            if (!CompassWidgetProvider.hasWidgets(CompassGpsService.this)) {
                state().edit().putBoolean(CompassWidgetProvider.KEY_TRACKING_ENABLED, false).apply();
                stopSelf();
                return;
            }
            SharedPreferences prefs = state();
            String gpsState = prefs.getString(CompassWidgetProvider.KEY_GPS_STATE, CompassWidgetProvider.STATE_SEARCHING);
            long fixElapsed = prefs.getLong(CompassWidgetProvider.KEY_FIX_ELAPSED_NANOS, 0L);
            boolean hasLocation = prefs.getBoolean(CompassWidgetProvider.KEY_HAS_LOCATION, false);
            boolean fresh = hasLocation && CompassLogic.isFresh(SystemClock.elapsedRealtimeNanos(), fixElapsed);
            if (!fresh) {
                prefs.edit().putBoolean(CompassWidgetProvider.KEY_HEADING_CURRENT, false).apply();
                if (!isGpsEnabled()) {
                    gpsState = CompassWidgetProvider.STATE_DISABLED;
                } else if (hasLocation || SystemClock.elapsedRealtime() - serviceStartedElapsed >= NO_SIGNAL_AFTER_MS) {
                    gpsState = CompassWidgetProvider.STATE_NO_SIGNAL;
                } else {
                    gpsState = CompassWidgetProvider.STATE_SEARCHING;
                }
                prefs.edit().putString(CompassWidgetProvider.KEY_GPS_STATE, gpsState).apply();
            } else if (!CompassWidgetProvider.STATE_ACTIVE.equals(gpsState)) {
                prefs.edit().putString(CompassWidgetProvider.KEY_GPS_STATE, CompassWidgetProvider.STATE_ACTIVE).apply();
            }
            renderWidgetIfDue(false);
            handler.postDelayed(this, STATE_CHECK_MS);
        }
    };

    public static boolean start(Context context) {
        Intent intent = new Intent(context, CompassGpsService.class).setAction(ACTION_START);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent);
            else context.startService(intent);
            return true;
        } catch (RuntimeException exception) {
            Log.w(TAG, "Could not start the GPS foreground service", exception);
            context.getSharedPreferences(CompassWidgetProvider.PREFS, Context.MODE_PRIVATE).edit()
                    .putString(CompassWidgetProvider.KEY_GPS_STATE, CompassWidgetProvider.STATE_STOPPED)
                    .apply();
            CompassWidgetProvider.renderAll(context);
            return false;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        serviceStartedElapsed = SystemClock.elapsedRealtime();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            state().edit().putBoolean(CompassWidgetProvider.KEY_TRACKING_ENABLED, false).apply();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!CompassWidgetProvider.hasWidgets(this)) {
            state().edit().putBoolean(CompassWidgetProvider.KEY_TRACKING_ENABLED, false).apply();
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            setGpsState(CompassWidgetProvider.STATE_PERMISSION, true);
            stopSelf();
            return START_NOT_STICKY;
        }

        state().edit().putBoolean(CompassWidgetProvider.KEY_TRACKING_ENABLED, true).apply();
        serviceStartedElapsed = SystemClock.elapsedRealtime();
        startGpsUpdates();
        handler.removeCallbacks(stateChecker);
        handler.postDelayed(stateChecker, STATE_CHECK_MS);
        return START_STICKY;
    }

    private void startGpsUpdates() {
        if (locationManager == null) {
            setGpsState(CompassWidgetProvider.STATE_NO_SIGNAL, true);
            return;
        }
        if (!isGpsEnabled()) {
            setGpsState(CompassWidgetProvider.STATE_DISABLED, true);
        } else {
            setGpsState(CompassWidgetProvider.STATE_SEARCHING, false);
        }
        if (updatesRequested) return;
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_MS, 0.0f,
                    locationListener, Looper.getMainLooper());
            updatesRequested = true;
        } catch (SecurityException | IllegalArgumentException e) {
            setGpsState(CompassWidgetProvider.STATE_PERMISSION, true);
        }
    }

    private boolean isGpsEnabled() {
        if (locationManager == null) return false;
        try {
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void handleLocation(Location location) {
        if (location == null) return;
        long nowNanos = SystemClock.elapsedRealtimeNanos();
        long fixNanos = location.getElapsedRealtimeNanos();
        if (!CompassLogic.isFresh(nowNanos, fixNanos)
                || !CompassLogic.isValidCoordinates(location.getLatitude(), location.getLongitude())) {
            setGpsState(CompassWidgetProvider.STATE_NO_SIGNAL, false);
            return;
        }

        SharedPreferences.Editor edit = state().edit()
                .putBoolean(CompassWidgetProvider.KEY_HAS_LOCATION, true)
                .putLong(CompassWidgetProvider.KEY_LATITUDE, Double.doubleToRawLongBits(location.getLatitude()))
                .putLong(CompassWidgetProvider.KEY_LONGITUDE, Double.doubleToRawLongBits(location.getLongitude()))
                .putLong(CompassWidgetProvider.KEY_LOCATION_TIME, location.getTime())
                .putLong(CompassWidgetProvider.KEY_FIX_ELAPSED_NANOS, fixNanos)
                .putString(CompassWidgetProvider.KEY_GPS_STATE, CompassWidgetProvider.STATE_ACTIVE);

        boolean hasAccuracy = location.hasAccuracy() && CompassLogic.isValidAccuracy(location.getAccuracy());
        edit.putBoolean(CompassWidgetProvider.KEY_HAS_ACCURACY, hasAccuracy);
        if (hasAccuracy) edit.putFloat(CompassWidgetProvider.KEY_ACCURACY, location.getAccuracy());

        boolean hasBearing = location.hasBearing()
                && CompassLogic.isValidBearing(location.getBearing())
                && CompassLogic.hasSufficientMovement(location.hasSpeed(), location.getSpeed());
        edit.putBoolean(CompassWidgetProvider.KEY_HEADING_CURRENT, hasBearing);
        if (hasBearing) {
            int degrees = CompassLogic.roundedBearing(location.getBearing());
            edit.putBoolean(CompassWidgetProvider.KEY_HAS_BEARING, true)
                    .putFloat(CompassWidgetProvider.KEY_BEARING_VALUE, location.getBearing())
                    .putString(CompassWidgetProvider.KEY_DIRECTION, CompassLogic.cardinal(location.getBearing()))
                    .putString(CompassWidgetProvider.KEY_DEGREES, degrees + "°");
        }
        edit.apply();
        renderWidgetIfDue(false);
    }

    private void setGpsState(String gpsState, boolean forceRender) {
        state().edit()
                .putString(CompassWidgetProvider.KEY_GPS_STATE, gpsState)
                .putBoolean(CompassWidgetProvider.KEY_HEADING_CURRENT, false)
                .apply();
        renderWidgetIfDue(forceRender);
    }

    private SharedPreferences state() {
        return getSharedPreferences(CompassWidgetProvider.PREFS, Context.MODE_PRIVATE);
    }

    private void renderWidgetIfDue(boolean force) {
        long now = SystemClock.elapsedRealtime();
        if (force || lastWidgetRenderElapsed == 0L || now - lastWidgetRenderElapsed >= WIDGET_RENDER_MIN_MS) {
            lastWidgetRenderElapsed = now;
            CompassWidgetProvider.renderAll(this);
        }
    }

    private Notification buildNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Localização GPS", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Indica que o GPS está a atualizar o widget");
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(this, 5001, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stopIntent = new Intent(this, CompassGpsService.class).setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 5002, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return builder.setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("GPS Compass")
                .setContentText("Localização ativa para o widget")
                .setContentIntent(openPending)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Parar GPS", stopPending)
                .build();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(stateChecker);
        if (locationManager != null && updatesRequested) locationManager.removeUpdates(locationListener);
        state().edit()
                .putString(CompassWidgetProvider.KEY_GPS_STATE, CompassWidgetProvider.STATE_STOPPED)
                .putBoolean(CompassWidgetProvider.KEY_HEADING_CURRENT, false)
                .apply();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
        if (CompassWidgetProvider.hasWidgets(this)) CompassWidgetProvider.renderAll(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
