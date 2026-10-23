package com.orbitaldelta.gpscompass;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CompassLogicTest {
    @Test
    public void cardinalPointsCoverEightSectorsAndWrapNorth() {
        assertEquals("N", CompassLogic.cardinal(0f));
        assertEquals("NE", CompassLogic.cardinal(45f));
        assertEquals("E", CompassLogic.cardinal(90f));
        assertEquals("SE", CompassLogic.cardinal(135f));
        assertEquals("S", CompassLogic.cardinal(180f));
        assertEquals("SW", CompassLogic.cardinal(225f));
        assertEquals("W", CompassLogic.cardinal(270f));
        assertEquals("NW", CompassLogic.cardinal(315f));
        assertEquals("N", CompassLogic.cardinal(359f));
    }

    @Test
    public void bearingMustBeInGpsRangeAndMovementMustBeSufficient() {
        assertTrue(CompassLogic.isValidBearing(0f));
        assertTrue(CompassLogic.isValidBearing(359.9f));
        assertFalse(CompassLogic.isValidBearing(-0.1f));
        assertFalse(CompassLogic.isValidBearing(360f));
        assertEquals(0, CompassLogic.roundedBearing(359.8f));
        assertTrue(CompassLogic.hasSufficientMovement(true, 1.0f));
        assertFalse(CompassLogic.hasSufficientMovement(true, 0.99f));
        assertFalse(CompassLogic.hasSufficientMovement(false, 20f));
    }

    @Test
    public void coordinatesAreValidatedAndFormattedToSixDecimalPlaces() {
        assertTrue(CompassLogic.isValidCoordinates(90d, 180d));
        assertFalse(CompassLogic.isValidCoordinates(90.1d, 0d));
        assertFalse(CompassLogic.isValidCoordinates(0d, Double.NaN));
        assertEquals("38.736946", CompassLogic.formatLatitude(38.7369463d));
        assertEquals("-9.142685", CompassLogic.formatLongitude(-9.1426847d));
        assertEquals("--", CompassLogic.formatLatitude(91d));
    }

    @Test
    public void fixFreshnessRejectsOldFutureAndMissingTimes() {
        long now = 80_000_000_000L;
        assertTrue(CompassLogic.isFresh(now, now - 15_000_000_000L));
        assertFalse(CompassLogic.isFresh(now, now - 15_000_000_001L));
        assertFalse(CompassLogic.isFresh(now, now + 1L));
        assertFalse(CompassLogic.isFresh(now, 0L));
    }

    @Test
    public void accuracyMustBeFiniteAndNonNegative() {
        assertTrue(CompassLogic.isValidAccuracy(0f));
        assertTrue(CompassLogic.isValidAccuracy(5.25f));
        assertFalse(CompassLogic.isValidAccuracy(-1f));
        assertFalse(CompassLogic.isValidAccuracy(Float.NaN));
        assertEquals("±5.3 m", CompassLogic.formatAccuracy(5.25f));
        assertEquals("Precisão indisponível", CompassLogic.formatAccuracy(Float.NaN));
    }
}
