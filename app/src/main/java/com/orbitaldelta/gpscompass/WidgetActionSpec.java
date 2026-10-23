package com.orbitaldelta.gpscompass;

/** The only dial actions exposed by the widget. Tests inspect these specifications without dialing. */
public final class WidgetActionSpec {
    public static final String ACTION_DIAL = "android.intent.action.DIAL";
    public static final String EMERGENCY_NUMBER = "112";
    public static final String FIRE_ALERT_NUMBER = "117";

    private WidgetActionSpec() { }

    public static String dialUri(String number) {
        if (!EMERGENCY_NUMBER.equals(number) && !FIRE_ALERT_NUMBER.equals(number)) {
            throw new IllegalArgumentException("Unsupported widget number");
        }
        return "tel:" + number;
    }
}
