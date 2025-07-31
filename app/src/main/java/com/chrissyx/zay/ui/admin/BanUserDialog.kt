package com.chrissyx.zay.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.chrissyx.zay.databinding.DialogBanUserBinding

class BanUserDialog : DialogFragment() {
    
    private var _binding: DialogBanUserBinding? = null
    private val binding get() = _binding!!
    
    private var username: String = ""
    private var onBanConfirmed: ((String, Long) -> Unit)? = null
    
    companion object {
        fun newInstance(
            username: String,
            onBanConfirmed: (reason: String, duration: Long) -> Unit
        ): BanUserDialog {
            return BanUserDialog().apply {
                this.username = username
                this.onBanConfirmed = onBanConfirmed
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogBanUserBinding.inflate(layoutInflater)
        
        // Set username
        binding.usernameText.text = "@$username"
        
        // Setup duration spinner
        val durations = arrayOf(
            "1 Hour",
            "24 Hours", 
            "7 Days",
            "30 Days",
            "Permanent"
        )
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, durations)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.durationSpinner.adapter = adapter
        
        return AlertDialog.Builder(requireContext())
            .setTitle("Ban User")
            .setView(binding.root)
            .setPositiveButton("Ban") { _, _ ->
                val reason = binding.reasonEditText.text.toString().trim()
                if (reason.isEmpty()) {
                    return@setPositiveButton
                }
                
                val duration = when (binding.durationSpinner.selectedItemPosition) {
                    0 -> 3600L // 1 hour
                    1 -> 86400L // 24 hours
                    2 -> 604800L // 7 days
                    3 -> 2592000L // 30 days
                    else -> -1L // Permanent
                }
                
                onBanConfirmed?.invoke(reason, duration)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 