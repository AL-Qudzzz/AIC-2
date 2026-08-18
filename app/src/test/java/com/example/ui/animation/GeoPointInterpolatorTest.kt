package com.example.ui.animation

import com.example.data.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.osmdroid.util.GeoPoint
import kotlin.math.abs

class GeoPointInterpolatorTest {

    @Test
    fun `interpolate returns start point at fraction 0`() {
        val start = GeoPoint(-6.3025, 106.7210)
        val end = GeoPoint(-6.3050, 106.7250)

        val result = GeoPointInterpolator.interpolate(0.0f, start, end)

        assertEquals(start.latitude, result.latitude, 1e-6)
        assertEquals(start.longitude, result.longitude, 1e-6)
    }

    @Test
    fun `interpolate returns end point at fraction 1`() {
        val start = GeoPoint(-6.3025, 106.7210)
        val end = GeoPoint(-6.3050, 106.7250)

        val result = GeoPointInterpolator.interpolate(1.0f, start, end)

        assertEquals(end.latitude, result.latitude, 1e-6)
        assertEquals(end.longitude, result.longitude, 1e-6)
    }

    @Test
    fun `interpolate returns accurate midpoint at fraction 0_5`() {
        val start = GeoPoint(-6.3000, 106.7200)
        val end = GeoPoint(-6.3100, 106.7300)

        val result = GeoPointInterpolator.interpolate(0.5f, start, end)

        assertEquals(-6.3050, result.latitude, 0.0001)
        assertEquals(106.7250, result.longitude, 0.0001)
    }

    @Test
    fun `computeHeading accurately identifies cardinal directions`() {
        val origin = GeoPoint(-6.3000, 106.7000)

        // North
        val north = GeoPoint(-6.2900, 106.7000)
        val headingNorth = GeoPointInterpolator.computeHeading(origin, north)
        assertTrue("Heading North should be ~0° or ~360°, was $headingNorth", abs(headingNorth - 0f) < 1f || abs(headingNorth - 360f) < 1f)

        // East
        val east = GeoPoint(-6.3000, 106.7100)
        val headingEast = GeoPointInterpolator.computeHeading(origin, east)
        assertEquals(90f, headingEast, 1.5f)

        // South
        val south = GeoPoint(-6.3100, 106.7000)
        val headingSouth = GeoPointInterpolator.computeHeading(origin, south)
        assertEquals(180f, headingSouth, 1.5f)

        // West
        val west = GeoPoint(-6.3000, 106.6900)
        val headingWest = GeoPointInterpolator.computeHeading(origin, west)
        assertEquals(270f, headingWest, 1.5f)
    }

    @Test
    fun `computeShortestRotationDelta takes shortest path across 0 and 360 boundaries`() {
        // Clockwise across 0° (350° to 10° -> +20°)
        val deltaClockwise = GeoPointInterpolator.computeShortestRotationDelta(350f, 10f)
        assertEquals(20f, deltaClockwise, 0.001f)

        // Counter-clockwise across 0° (10° to 350° -> -20°)
        val deltaCounter = GeoPointInterpolator.computeShortestRotationDelta(10f, 350f)
        assertEquals(-20f, deltaCounter, 0.001f)

        // Normal acute delta (45° to 90° -> +45°)
        val deltaNormal = GeoPointInterpolator.computeShortestRotationDelta(45f, 90f)
        assertEquals(45f, deltaNormal, 0.001f)

        // Direct opposite (0° to 180° -> 180° or -180°)
        val deltaOpposite = abs(GeoPointInterpolator.computeShortestRotationDelta(0f, 180f))
        assertEquals(180f, deltaOpposite, 0.001f)
    }

    @Test
    fun `interpolateRotation smoothly interpolates without spinning`() {
        // Crossing 0°: from 350° to 10° at 50% should be 0° (or 360°)
        val midRotation = GeoPointInterpolator.interpolateRotation(0.5f, 350f, 10f)
        assertTrue("Mid rotation should be 0° or 360°, was $midRotation", midRotation == 0f || abs(midRotation - 360f) < 0.01f || abs(midRotation - 0f) < 0.01f)

        // Mid rotation between 100° and 120° at 25%
        val quarterRotation = GeoPointInterpolator.interpolateRotation(0.25f, 100f, 120f)
        assertEquals(105f, quarterRotation, 0.001f)
    }

    @Test
    fun `computeDistanceMeters returns valid great circle distance`() {
        val p1 = GeoPoint(-6.3000, 106.7000)
        val p2 = GeoPoint(-6.3000, 106.7000)
        assertEquals(0.0, GeoPointInterpolator.computeDistanceMeters(p1, p2), 0.01)

        // Sawah to Bintaro ~ 2.2 km
        val p3 = GeoPoint(-6.3000, 106.7000)
        val p4 = GeoPoint(-6.2800, 106.7000)
        val dist = GeoPointInterpolator.computeDistanceMeters(p3, p4)
        assertTrue("Distance should be approximately 2220m, was $dist", dist in 2200.0..2240.0)
    }

    @Test
    fun `VehicleType maps from profile strings accurately`() {
        assertEquals(VehicleType.MOTORCYCLE, VehicleType.fromProfileString("Motor Matik"))
        assertEquals(VehicleType.MOTORCYCLE, VehicleType.fromProfileString("Motor Bebek/Sport"))
        assertEquals(VehicleType.CAR, VehicleType.fromProfileString("Mobil Sedan"))
        assertEquals(VehicleType.TRUCK, VehicleType.fromProfileString("Mobil Van / Box"))
        assertEquals(VehicleType.TRUCK, VehicleType.fromProfileString("Truk Box"))
        assertEquals(VehicleType.MOTORCYCLE, VehicleType.fromProfileString(null))
        assertEquals(VehicleType.MOTORCYCLE, VehicleType.fromProfileString(""))
    }
}
