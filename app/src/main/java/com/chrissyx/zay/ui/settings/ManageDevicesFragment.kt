package com.chrissyx.zay.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentManageDevicesBinding
import com.chrissyx.zay.utils.DeviceAuthManager
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.launch

class ManageDevicesFragment : Fragment() {
    
    private var _binding: FragmentManageDevicesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var firebaseRepository: FirebaseRepository
    private lateinit var deviceAuthManager: DeviceAuthManager
    private lateinit var userPreferences: UserPreferences
    private lateinit var deviceAdapter: TrustedDeviceAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        firebaseRepository = FirebaseRepository()
        deviceAuthManager = DeviceAuthManager(requireContext(), firebaseRepository)
        userPreferences = UserPreferences(requireContext())
        
        setupUI()
        loadTrustedDevices()
    }
    
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        deviceAdapter = TrustedDeviceAdapter { deviceId, deviceInfo ->
            showRemoveDeviceDialog(deviceId, deviceInfo)
        }
        
        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
        
        binding.refreshButton.setOnClickListener {
            loadTrustedDevices()
        }
        
        binding.clearAllButton.setOnClickListener {
            showClearAllDevicesDialog()
        }
    }
    
    private fun loadTrustedDevices() {
        val username = userPreferences.username ?: return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val devices = deviceAuthManager.getTrustedDevices(username)
                val currentDeviceId = deviceAuthManager.getDeviceId()
                
                // Convert to display format and mark current device
                val deviceItems = devices.map { (deviceId, deviceInfo) ->
                    TrustedDeviceItem(
                        deviceId = deviceId,
                        deviceInfo = deviceInfo,
                        isCurrentDevice = deviceId == currentDeviceId
                    )
                }
                
                binding.progressBar.visibility = View.GONE
                
                if (deviceItems.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.devicesRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.devicesRecyclerView.visibility = View.VISIBLE
                    deviceAdapter.submitList(deviceItems)
                }
                
                binding.deviceCountText.text = "Trusted devices: ${deviceItems.size}"
                
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading devices: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showRemoveDeviceDialog(deviceId: String, deviceInfo: String) {
        val currentDeviceId = deviceAuthManager.getDeviceId()
        
        // Warn about removing current device but allow it with extra confirmation
        if (deviceId == currentDeviceId) {
            AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Remove Current Device")
                .setMessage("You are about to remove the device you're currently using:\n\n$deviceInfo\n\nThis will log you out and you'll need to verify your account to log back in.\n\nAre you sure you want to continue?")
                .setPositiveButton("Remove & Logout") { _, _ ->
                    removeCurrentDevice(deviceId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("Remove Trusted Device")
                .setMessage("Remove trust for:\n\n$deviceInfo\n\nYou will need to verify your account if you log in from this device again.")
                .setPositiveButton("Remove") { _, _ ->
                    removeDevice(deviceId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    private fun removeDevice(deviceId: String) {
        val username = userPreferences.username ?: return
        
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.removeTrustedDevice(username, deviceId)
                if (success) {
                    Toast.makeText(requireContext(), "Device removed successfully", Toast.LENGTH_SHORT).show()
                    loadTrustedDevices() // Refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to remove device", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error removing device: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun removeCurrentDevice(deviceId: String) {
        val username = userPreferences.username ?: return
        
        lifecycleScope.launch {
            try {
                val success = firebaseRepository.removeTrustedDevice(username, deviceId)
                if (success) {
                    // Clear user preferences and logout
                    userPreferences.logout()
                    
                    Toast.makeText(requireContext(), "Device removed. You have been logged out.", Toast.LENGTH_LONG).show()
                    
                    // Navigate back to login
                    if (activity != null) {
                        (requireActivity() as com.chrissyx.zay.MainActivity).showLogin()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to remove current device", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error removing current device: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showClearAllDevicesDialog() {
        val username = userPreferences.username ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Clear All Trusted Devices")
            .setMessage("This will remove ALL trusted devices from your account.\n\nYou will need to verify your account when logging in from any device.\n\nAre you sure you want to continue?")
            .setPositiveButton("Clear All") { _, _ ->
                clearAllDevices()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun clearAllDevices() {
        val username = userPreferences.username ?: return
        
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val success = firebaseRepository.clearAllTrustedDevices(username)
                binding.progressBar.visibility = View.GONE
                
                if (success) {
                    Toast.makeText(requireContext(), "All devices cleared successfully", Toast.LENGTH_SHORT).show()
                    loadTrustedDevices() // Refresh list
                } else {
                    Toast.makeText(requireContext(), "Failed to clear devices", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error clearing devices: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class TrustedDeviceItem(
    val deviceId: String,
    val deviceInfo: String,
    val isCurrentDevice: Boolean
)