package com.example.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.RouteWiseViewModel
import com.example.ui.SharedRouteViewModel
import com.example.ui.screens.RouteScreen
import com.example.ui.theme.MyApplicationTheme

/**
 * RouteFragment: Handles address input, Nominatim geocoding resolution,
 * and updates the SharedRouteViewModel so MapFragment / MapScreen immediately reflects changes.
 */
class RouteFragment : Fragment() {

    private val sharedRouteViewModel: SharedRouteViewModel by lazy {
        ViewModelProvider(requireActivity())[SharedRouteViewModel::class.java]
    }
    private val routeWiseViewModel: RouteWiseViewModel by lazy {
        ViewModelProvider(requireActivity())[RouteWiseViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MyApplicationTheme {
                    val packages = routeWiseViewModel.packages.collectAsStateWithLifecycle().value
                    val trafficAlerts = routeWiseViewModel.trafficAlerts.collectAsStateWithLifecycle().value
                    val vehicleProfile = routeWiseViewModel.vehicleProfile.collectAsStateWithLifecycle().value

                    RouteScreen(
                        packages = packages,
                        trafficAlerts = trafficAlerts,
                        vehicleProfile = vehicleProfile,
                        onOptimizeRouteClick = {
                            routeWiseViewModel.optimizeRoute()
                            sharedRouteViewModel.recalculateRouteWithOnnx()
                        },
                        onPackageStatusUpdateClick = { routeWiseViewModel.selectPackageForUpdate(it) },
                        onAddPackageClick = { routeWiseViewModel.toggleAddPackageDialog(true) },
                        onApplyAlternateRoute = { routeWiseViewModel.applyAlternateRoute(it) },
                        onDismissAlert = { routeWiseViewModel.dismissTrafficAlert(it) }
                    )
                }
            }
        }
    }

    /**
     * Public method to geocode and add an address to SharedRouteViewModel.
     */
    fun addNewAddress(
        address: String,
        recipientName: String,
        phone: String = "",
        priority: String = "REGULER",
        packageType: String = "Paket Reguler (1 kg)"
    ) {
        sharedRouteViewModel.addDestinationWithGeocoding(
            address = address,
            recipientName = recipientName,
            phone = phone,
            priority = priority,
            packageType = packageType
        )
    }
}
