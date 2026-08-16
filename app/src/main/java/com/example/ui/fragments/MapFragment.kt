package com.example.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ui.SharedRouteViewModel
import com.example.ui.screens.MapScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

/**
 * MapFragment: Observes the Activity-scoped SharedRouteViewModel, draws markers and polylines,
 * prevents map state loss on resume / tab transitions, and executes startNavigationMode() logic.
 */
class MapFragment : Fragment() {

    // Activity-scoped ViewModel to preserve state across fragment recreation
    private val sharedRouteViewModel: SharedRouteViewModel by lazy {
        ViewModelProvider(requireActivity())[SharedRouteViewModel::class.java]
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
                    MapScreen(sharedViewModel = sharedRouteViewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe StateFlows to verify state restoration
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedRouteViewModel.destinations.collect { destinations ->
                        // Automatically updates markers on map
                    }
                }
                launch {
                    sharedRouteViewModel.routePolyline.collect { polylineState ->
                        // Automatically renders recalculated ONNX polyline
                    }
                }
                launch {
                    sharedRouteViewModel.navigationState.collect { navState ->
                        if (navState.isNavigating) {
                            // Navigation UI Active
                        }
                    }
                }
            }
        }
    }

    /**
     * Programmatically triggers 3D Google Maps Driving Navigation Mode.
     */
    fun startNavigationMode() {
        sharedRouteViewModel.startNavigationMode()
    }

    /**
     * Exits Navigation Mode.
     */
    fun stopNavigationMode() {
        sharedRouteViewModel.stopNavigationMode()
    }
}
