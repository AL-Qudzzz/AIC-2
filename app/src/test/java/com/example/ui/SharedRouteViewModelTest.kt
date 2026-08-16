package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.ml.ModelAssetManager
import com.example.data.model.PackageDestination
import com.example.data.service.GeocodedLocation
import com.example.data.service.RouteOptimizerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class SharedRouteViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application
    private lateinit var viewModel: SharedRouteViewModel
    private lateinit var optimizerService: RouteOptimizerService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        viewModel = SharedRouteViewModel(application)
        val assetManager = ModelAssetManager(application)
        optimizerService = RouteOptimizerService(assetManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialMapStateIsPreserved() {
        val camera = viewModel.cameraState.value
        assertEquals(-6.3025, camera.center.latitude, 0.001)
        assertEquals(106.7210, camera.center.longitude, 0.001)
        assertEquals(15.0, camera.zoomLevel, 0.001)
        assertFalse(viewModel.navigationState.value.isNavigating)
    }

    @Test
    fun testTwoWayGeocodingDestinationAddition() = runTest(testDispatcher) {
        val testAddress = "Jl. Sawah Raya No. 10, Tangerang Selatan"
        val recipient = "Bpk. Budi Santoso"
        val latch = java.util.concurrent.CountDownLatch(1)

        viewModel.addDestinationWithGeocoding(
            address = testAddress,
            recipientName = recipient,
            phone = "08123456789",
            priority = "EKSPRES",
            onComplete = {
                latch.countDown()
            }
        )
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)

        val dests = viewModel.destinations.value
        assertTrue(dests.isNotEmpty())
        val added = dests.find { it.title == recipient }
        assertNotNull(added)
        assertEquals("EKSPRES", added?.priority)
    }

    @Test
    fun testMultiStopOptimizationFromUserLocation() = runTest(testDispatcher) {
        val userLocation = GeoPoint(-6.3000, 106.7200)
        val stop1 = PackageDestination(
            id = "DEST-1",
            title = "Stop Far Away",
            address = "Jl. Jauh",
            lat = -6.3200,
            lng = 106.7500,
            priority = "REGULER"
        )
        val stop2 = PackageDestination(
            id = "DEST-2",
            title = "Stop Very Close",
            address = "Jl. Dekat Sekali",
            lat = -6.3005,
            lng = 106.7210,
            priority = "REGULER"
        )

        val result = optimizerService.optimizeRouteSequence(userLocation, listOf(stop1, stop2))
        assertNotNull(result)
        assertEquals(2, result.optimizedDestinations.size)

        // Closest stop to origin should be sequenced first
        assertEquals("DEST-2", result.optimizedDestinations.first().id)
        assertTrue(result.polylineState.points.isNotEmpty())
    }

    @Test
    fun testNavigationModeStartAndStop() = runTest(testDispatcher) {
        val stop = PackageDestination(
            id = "DEST-101",
            title = "Penerima A",
            address = "Jl. Cendrawasih No. 5",
            lat = -6.3010,
            lng = 106.7220
        )
        viewModel.setUserLocation(GeoPoint(-6.3000, 106.7200))
        viewModel.addDestination(GeocodedLocation("Jl. Cendrawasih No. 5", stop.point))

        // Start Navigation
        viewModel.startNavigationMode()
        val navState = viewModel.navigationState.value
        assertTrue(navState.isNavigating)
        assertNotNull(navState.currentDestination)
        assertTrue(navState.distanceToNextKm >= 0.0)

        // Stop Navigation
        viewModel.stopNavigationMode()
        assertFalse(viewModel.navigationState.value.isNavigating)
    }
}
