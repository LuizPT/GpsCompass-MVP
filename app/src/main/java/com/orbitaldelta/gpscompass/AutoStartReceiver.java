package com.orbitaldelta.gpscompass;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

public final class AutoStartReceiver extends BroadcastReceiver {
    private static final String TAG = "GpsCompassAutoStart";
    private static final String ACTION_MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        boolean packageReplaced = ACTION_MY_PACKAGE_REPLACED.equals(action);
        if (!packageReplaced && !Intent.ACTION_BOOT_COMPLETED.equals(action)) return;
        Log.i(TAG, "Received " + action);

        SharedPreferences preferences = context.getSharedPreferences(
                CompassWidgetProvider.PREFS, Context.MODE_PRIVATE);
        boolean hasWidgets = CompassWidgetProvider.hasWidgets(context);
        boolean hasPreciseLocation = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        // Existing builds started tracking when opened with a widget; preserve that choice after upgrading.
        if (packageReplaced && !preferences.contains(CompassWidgetProvider.KEY_TRACKING_ENABLED)) {
            boolean shouldMigrate = hasWidgets && hasPreciseLocation;
            preferences.edit().putBoolean(CompassWidgetProvider.KEY_TRACKING_ENABLED, shouldMigrate).apply();
        }

        if (!preferences.getBoolean(CompassWidgetProvider.KEY_TRACKING_ENABLED, false)) {
            Log.i(TAG, "Tracking was not enabled; skipping automatic restart");
            return;
        }
        if (!hasWidgets) {
            Log.i(TAG, "No active widget; disabling automatic restart");
            preferences.edit().putBoolean(CompassWidgetProvider.KEY_TRACKING_ENABLED, false).apply();
            return;
        }
        if (!hasPreciseLocation) {
            Log.i(TAG, "Precise location permission is missing");
            showWidgetState(context, CompassWidgetProvider.STATE_PERMISSION);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Background location permission is missing");
            showWidgetState(context, CompassWidgetProvider.STATE_BACKGROUND_PERMISSION);
            return;
        }

        Log.i(TAG, "Restarting GPS tracking");
        CompassGpsService.start(context);
    }

    private static void showWidgetState(Context context, String state) {
        context.getSharedPreferences(CompassWidgetProvider.PREFS, Context.MODE_PRIVATE)
                .edit().putString(CompassWidgetProvider.KEY_GPS_STATE, state)
                .putBoolean(CompassWidgetProvider.KEY_HEADING_CURRENT, false)
                .apply();
        CompassWidgetProvider.renderAll(context);
    }
}
