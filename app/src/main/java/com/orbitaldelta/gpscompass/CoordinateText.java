package com.orbitaldelta.gpscompass;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class CoordinateText {
    private CoordinateText() { }

    static String fromPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CompassWidgetProvider.PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(CompassWidgetProvider.KEY_HAS_LOCATION, false)) {
            return "Localização GPS indisponível; ainda não foram recebidas coordenadas.";
        }
        double latitude = Double.longBitsToDouble(prefs.getLong(CompassWidgetProvider.KEY_LATITUDE, Double.doubleToRawLongBits(Double.NaN)));
        double longitude = Double.longBitsToDouble(prefs.getLong(CompassWidgetProvider.KEY_LONGITUDE, Double.doubleToRawLongBits(Double.NaN)));
        if (!CompassLogic.isValidCoordinates(latitude, longitude)) {
            return "Localização GPS indisponível; coordenadas inválidas.";
        }
        String freshness = CompassLogic.isFresh(android.os.SystemClock.elapsedRealtimeNanos(),
                prefs.getLong(CompassWidgetProvider.KEY_FIX_ELAPSED_NANOS, 0L)) ? "atual" : "antiga";
        String accuracy = prefs.getBoolean(CompassWidgetProvider.KEY_HAS_ACCURACY, false)
                ? CompassLogic.formatAccuracy(prefs.getFloat(CompassWidgetProvider.KEY_ACCURACY, Float.NaN))
                : "indisponível";
        long time = prefs.getLong(CompassWidgetProvider.KEY_LOCATION_TIME, 0L);
        String timeText = time > 0L
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z", Locale.getDefault()).format(new Date(time))
                : "indisponível";
        return "Latitude WGS84: " + CompassLogic.formatLatitude(latitude)
                + "\nLongitude WGS84: " + CompassLogic.formatLongitude(longitude)
                + "\nEstado: " + freshness
                + "\nPrecisão horizontal: " + accuracy
                + "\nHora da última localização: " + timeText;
    }
}
