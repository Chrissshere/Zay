package com.chrissyx.zay.ui.support

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.chrissyx.zay.R
import com.chrissyx.zay.data.models.SupportTicket
import com.chrissyx.zay.data.models.TicketCategory
import com.chrissyx.zay.data.models.TicketPriority
import com.chrissyx.zay.data.repository.FirebaseRepository
import com.chrissyx.zay.databinding.FragmentContactSupportBinding
import com.chrissyx.zay.utils.UserPreferences
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.Manifest
import com.chrissyx.zay.utils.TicketUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactSupportFragment : Fragment() {
    
    private var _binding: FragmentContactSupportBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var firebaseRepository: FirebaseRepository
    
    // Image upload properties
    private var selectedImages = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImagePreviewAdapter
    
    // Permission and image picker launchers
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(requireContext(), "Permission denied. Cannot access images.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { addSelectedImage(it) }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactSupportBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            userPreferences = UserPreferences(requireContext())
            firebaseRepository = FirebaseRepository()
            
            setupUI()
            setupSpinners()
            setupValidation()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading support form", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupUI() {
        // Back button
        binding.backButton.setOnClickListener {
            try {
                requireActivity().supportFragmentManager.popBackStack()
            } catch (e: Exception) {
            }
        }
        
        // Submit button
        binding.submitButton.setOnClickListener {
            submitSupportRequest()
        }
        
        // Image upload functionality
        setupImageUpload()
    }
    
    private fun setupImageUpload() {
        // Initialize image adapter
        imageAdapter = ImagePreviewAdapter(selectedImages) { position ->
            removeImage(position)
        }
        
        binding.imagePreviewRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }
        
        // Upload image button
        binding.uploadImageButton.setOnClickListener {
            if (selectedImages.size >= 3) {
                Toast.makeText(requireContext(), "Maximum 3 images allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            checkPermissionAndPickImage()
        }
        
        updateImageCount()
    }
    
    private fun checkPermissionAndPickImage() {
        // For Android 13+ (API 33+), we need different permissions
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(permission)
            }
        }
    }
    
    private fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }
    
    private fun addSelectedImage(uri: Uri) {
        if (selectedImages.size < 3) {
            selectedImages.add(uri)
            imageAdapter.updateImages(selectedImages)
            updateImageCount()
            updateImagePreviewVisibility()
        } else {
            Toast.makeText(requireContext(), "Maximum 3 images allowed", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removeImage(position: Int) {
        if (position < selectedImages.size) {
            selectedImages.removeAt(position)
            imageAdapter.notifyItemRemoved(position)
            updateImageCount()
            updateImagePreviewVisibility()
        }
    }
    
    private fun updateImageCount() {
        binding.imageCountText.text = "${selectedImages.size}/3 images"
    }
    
    private fun updateImagePreviewVisibility() {
        binding.imagePreviewRecyclerView.visibility = if (selectedImages.isNotEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    
    private fun clearForm() {
        binding.subjectEditText.setText("")
        binding.descriptionEditText.setText("")
        binding.emailEditText.setText("")
        binding.categorySpinner.setSelection(0)
        // Priority spinner removed - all tickets have medium priority
        selectedImages.clear()
        imageAdapter.updateImages(selectedImages)
        updateImageCount()
        updateImagePreviewVisibility()
        binding.submitButton.isEnabled = false
        binding.submitButton.text = "Submit Support Request"
        binding.submitButton.alpha = 0.5f
    }
    
    private suspend fun uploadImagesToStorage(imageUris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        val uploadedUrls = mutableListOf<String>()
        
        try {
            imageUris.forEachIndexed { index, uri ->
                try {
                    
                    // Generate unique filename for the image
                    val timestamp = System.currentTimeMillis()
                    val filename = "support_tickets/${userPreferences.username}_${timestamp}_$index.jpg"
                    
                    // Upload to Firebase Storage with context
                    val imageUrl = firebaseRepository.uploadImageToStorageWithContext(
                        requireContext(), 
                        uri, 
                        filename
                    )
                    
                    if (imageUrl != null) {
                        uploadedUrls.add(imageUrl)
                    } else {
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
        
        uploadedUrls
    }
    
    private fun setupSpinners() {
        try {
            // Category spinner only - priority removed as all requests are equally urgent
            val categories = TicketCategory.values().map { it.displayName }
            val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.categorySpinner.adapter = categoryAdapter
            
        } catch (e: Exception) {
        }
    }
    
    private fun setupValidation() {
        val textWatcher = {
            validateForm()
        }
        
        binding.subjectEditText.addTextChangedListener { textWatcher() }
        binding.descriptionEditText.addTextChangedListener { textWatcher() }
    }
    
    private fun validateForm() {
        val subject = binding.subjectEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()
        
        val isValid = subject.isNotEmpty() && description.isNotEmpty() && description.length >= 10
        
        binding.submitButton.isEnabled = isValid
        binding.submitButton.alpha = if (isValid) 1.0f else 0.5f
    }
    
    private fun submitSupportRequest() {
        try {
            val subject = binding.subjectEditText.text.toString().trim()
            val description = binding.descriptionEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            
            if (subject.isEmpty() || description.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (description.length < 10) {
                Toast.makeText(requireContext(), "Please provide a more detailed description (at least 10 characters)", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Disable submit button during submission
            binding.submitButton.isEnabled = false
            binding.submitButton.text = "Submitting..."
            
            val selectedCategory = TicketCategory.values()[binding.categorySpinner.selectedItemPosition]
            val selectedPriority = TicketPriority.MEDIUM // All support requests are equally urgent
            
            val deviceInfo = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
            val appVersion = try {
                requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
            
            val supportTicket = SupportTicket(
                id = TicketUtils.generateTicketId(),
                username = userPreferences.username ?: "anonymous",
                email = email,
                subject = subject,
                description = description,
                category = selectedCategory,
                priority = selectedPriority,
                deviceInfo = deviceInfo,
                appVersion = appVersion,
                platform = userPreferences.platform ?: "UNKNOWN"
            )
            
            lifecycleScope.launch {
                try {
                    
                    // Upload images to Firebase Storage first
                    val imageUrls = if (selectedImages.isNotEmpty()) {
                        uploadImagesToStorage(selectedImages)
                    } else {
                        emptyList()
                    }
                    
                    
                    // Create support ticket with image URLs
                    val ticketWithImages = supportTicket.copy(attachments = imageUrls)
                    val success = firebaseRepository.createSupportTicket(ticketWithImages)
                    
                    
                    if (!isAdded) return@launch
                    
                    if (success) {
                        Toast.makeText(requireContext(), "Support request submitted successfully! We'll get back to you soon.", Toast.LENGTH_LONG).show()
                        
                        // Clear form completely
                        clearForm()
                        
                        // Navigate back after short delay
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            try {
                                if (isAdded) {
                                    requireActivity().supportFragmentManager.popBackStack()
                                }
                            } catch (e: Exception) {
                            }
                        }, 2000)
                        
                    } else {
                        Toast.makeText(requireContext(), "Failed to submit support request. Please try again.", Toast.LENGTH_SHORT).show()
                        binding.submitButton.isEnabled = true
                        binding.submitButton.text = "Submit Support Request"
                    }
                    
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Error submitting support request: ${e.message}", Toast.LENGTH_LONG).show()
                        binding.submitButton.isEnabled = true
                        binding.submitButton.text = "Submit Support Request"
                    }
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error submitting support request", Toast.LENGTH_SHORT).show()
            binding.submitButton.isEnabled = true
            binding.submitButton.text = "Submit Support Request"
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}