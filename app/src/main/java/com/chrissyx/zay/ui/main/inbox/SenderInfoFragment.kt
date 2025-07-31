package com.chrissyx.zay.ui.main.inbox

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.SenderInfo
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentSenderInfoBinding
import com.chrissyx.zay.utils.DeviceUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class SenderInfoFragment : BottomSheetDialogFragment(), OnMapReadyCallback {

    private var _binding: FragmentSenderInfoBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var username: String
    private lateinit var firebaseRepository: FirebaseRepository
    private var googleMap: GoogleMap? = null
    private var senderLocation: SenderInfo.Location? = null

    companion object {
        private const val ARG_USERNAME = "username"

        fun newInstance(username: String): SenderInfoFragment {
            val fragment = SenderInfoFragment()
            val args = Bundle().apply {
                putString(ARG_USERNAME, username)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        username = arguments?.getString(ARG_USERNAME) ?: ""
        firebaseRepository = FirebaseRepository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSenderInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupGoogleMap()
        loadSenderInfo()
    }

    private fun setupUI() {
        // Close button
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        // Show loading state
        showLoading(true)
    }

    private fun setupGoogleMap() {
        // Get the SupportMapFragment and request notification when the map is ready
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // Configure map settings to match iOS style
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = false
            uiSettings.isMapToolbarEnabled = false
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = false
            uiSettings.isRotateGesturesEnabled = false
            uiSettings.isTiltGesturesEnabled = false
        }
        
        // If we already have location data, show it on the map
        senderLocation?.let { location ->
            showLocationOnMap(location.latitude, location.longitude)
        }
    }

    private fun loadSenderInfo() {
        lifecycleScope.launch {
            try {
                val senderInfo = firebaseRepository.getSenderInfo(username)
                
                if (senderInfo != null) {
                    displaySenderInfo(senderInfo)
                } else {
                    showError()
                }
            } catch (e: Exception) {
                showError()
            }
        }
    }

    private fun displaySenderInfo(senderInfo: SenderInfo) {
        showLoading(false)
        
        // Username
        binding.usernameText.text = "@${senderInfo.username}"
        
        // Device
        val deviceName = DeviceUtils.getDeviceModelName(senderInfo.device)
        binding.deviceText.text = deviceName
        
        // Location
        if (senderInfo.location != null) {
            binding.locationLayout.visibility = View.VISIBLE
            binding.noLocationText.visibility = View.GONE
            
            // Store location for map
            senderLocation = senderInfo.location
            
            // Display coordinates overlay
            val lat = String.format("%.6f", senderInfo.location.latitude)
            val lon = String.format("%.6f", senderInfo.location.longitude)
            binding.locationText.text = "Lat: $lat, Lon: $lon"
            binding.locationText.visibility = View.VISIBLE
            
            // Show location on map if map is ready
            googleMap?.let {
                showLocationOnMap(senderInfo.location.latitude, senderInfo.location.longitude)
            }
        } else {
            binding.locationLayout.visibility = View.GONE
            binding.noLocationText.visibility = View.VISIBLE
        }
        
        // Show content
        binding.contentLayout.visibility = View.VISIBLE
        binding.errorLayout.visibility = View.GONE
    }

    private fun showLocationOnMap(latitude: Double, longitude: Double) {
        val location = LatLng(latitude, longitude)
        
        googleMap?.apply {
            // Clear any existing markers
            clear()
            
            // Add a purple marker (to match iOS design)
            val markerOptions = MarkerOptions()
                .position(location)
                .title("Sender Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            
            addMarker(markerOptions)
            
            // Move camera to location with appropriate zoom
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 12f)
            animateCamera(cameraUpdate)
            
        }
    }

    private fun showError() {
        showLoading(false)
        binding.contentLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingLayout.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 