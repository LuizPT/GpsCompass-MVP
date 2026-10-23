package com.orbitaldelta.gpscompass;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WidgetActionSpecTest {
    @Test
    public void emergencyAndFireActionsOnlyDescribeDialUris() {
        assertEquals("android.intent.action.DIAL", WidgetActionSpec.ACTION_DIAL);
        assertEquals("tel:112", WidgetActionSpec.dialUri(WidgetActionSpec.EMERGENCY_NUMBER));
        assertEquals("tel:117", WidgetActionSpec.dialUri(WidgetActionSpec.FIRE_ALERT_NUMBER));
    }

    @Test
    public void unsupportedDialNumbersAreRejected() {
        try {
            WidgetActionSpec.dialUri("911");
            fail("Only the two requested numbers may be dialed from the widget");
        } catch (IllegalArgumentException expected) {
            // The tests only inspect the intent specification and never start a real dialer.
        }
    }
}
