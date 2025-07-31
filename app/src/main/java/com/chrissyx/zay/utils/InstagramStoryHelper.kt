package com.chrissyx.zay.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.chrissyx.zay.R
import java.io.File
import java.io.FileOutputStream

object InstagramStoryHelper {
    
    fun shareProfileStory(
        fragment: Fragment,
        username: String,
        prompt: String,
        profileBitmap: Bitmap?,
        isVerified: Boolean,
        linkText: String
    ) {
        val storyImage = generateProfileStoryImage(
            fragment,
            username,
            prompt,
            profileBitmap,
            isVerified
        )
        shareToInstagramStory(fragment, storyImage, linkText)
    }
    
    fun shareMessageResponseStory(
        fragment: Fragment,
        userPrompt: String,
        anonymousMessage: String,
        profileBitmap: Bitmap?,
        isVerified: Boolean,
        username: String
    ) {
        val storyImage = generateMessageResponseStoryImage(
            fragment,
            userPrompt,
            anonymousMessage,
            profileBitmap,
            isVerified,
            username
        )
        shareToInstagramStory(fragment, storyImage, "Check out my response on Zay!")
    }
    
    fun generateProfileStoryImage(
        fragment: Fragment,
        username: String,
        prompt: String,
        profileBitmap: Bitmap?,
        isVerified: Boolean
    ): Bitmap {
        val width = 1080
        val height = 1920
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Create gradient background (purple to blue)
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.parseColor("#8A2BE2"), // Blue violet
                Color.parseColor("#4B0082"), // Indigo
                Color.parseColor("#6A0DAD")  // Purple
            )
        )
        gradientDrawable.setBounds(0, 0, width, height)
        gradientDrawable.draw(canvas)
        
        // Paint for text
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }
        
        val boldPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        // Profile picture or initial circle
        val profileSize = 180
        val profileX = width / 2
        val profileY = 400
        
        drawProfile(canvas, profileBitmap, profileX, profileY, profileSize, username, textPaint)
        
        // Username with verification checkmark
        val usernameY = profileY + profileSize / 2 + 80
        boldPaint.textSize = 60f
        
        // Calculate username width to position checkmark
        val usernameText = "@$username"
        val usernameWidth = boldPaint.measureText(usernameText)
        
        // Draw username
        canvas.drawText(usernameText, profileX.toFloat(), usernameY.toFloat(), boldPaint)
        
        // Draw verification checkmark if verified
        if (isVerified) {
            drawVerificationCheckmark(fragment, canvas, profileX, usernameWidth, usernameY)
        }
        
        // Prompt text (only under username, single location)
        val promptY = usernameY + 80
        textPaint.textSize = 44f
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.WHITE
        
        drawPromptText(canvas, prompt, textPaint, width, promptY.toFloat())
        
        // "Put link here" placeholder - centered in middle of screen
        val linkY = height / 2 + 100 // Center of screen plus offset
        textPaint.textSize = 48f
        textPaint.color = Color.parseColor("#CCCCCC")
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Put link here", (width / 2).toFloat(), linkY.toFloat(), textPaint)
        
        // Draw logo and branding
        drawLogoAndBranding(fragment, canvas, width, height, boldPaint)
        
        return bitmap
    }
    
    private fun generateMessageResponseStoryImage(
        fragment: Fragment,
        userPrompt: String,
        anonymousMessage: String,
        profileBitmap: Bitmap?,
        isVerified: Boolean,
        username: String
    ): Bitmap {
        val width = 1080
        val height = 1920
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Dark background like Instagram
        canvas.drawColor(Color.parseColor("#1A1A1A"))
        
        // Paint for text
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        // Draw ONE CARD with random positioning for uniqueness
        val randomSeed = (userPrompt + anonymousMessage).hashCode()
        val random = kotlin.random.Random(randomSeed)
        
        val cardY = (height / 2) - 200 + random.nextInt(-100, 100) // Random vertical offset
        val cardHeight = 400 + random.nextInt(-50, 50) // Random height variation
        val rotation = random.nextFloat() * 6 - 3 // Random rotation -3 to +3 degrees
        
        canvas.save()
        canvas.rotate(rotation, (width / 2).toFloat(), (cardY + cardHeight / 2).toFloat())
        drawCombinedResponseCard(canvas, userPrompt, anonymousMessage, width, cardY, cardHeight, textPaint)
        canvas.restore()
        
        // Draw logo and branding
        val boldPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        drawLogoAndBranding(fragment, canvas, width, height, boldPaint)
        
        return bitmap
    }
    
    private fun drawCombinedResponseCard(
        canvas: Canvas,
        userPrompt: String,
        anonymousMessage: String,
        canvasWidth: Int,
        cardY: Int,
        cardHeight: Int,
        textPaint: Paint
    ) {
        val cardPadding = 60
        val cardX = cardPadding
        val cardWidth = canvasWidth - (cardPadding * 2)
        val cornerRadius = 25f
        
        // Draw ONE card with white background
        val cardRect = android.graphics.RectF(
            cardX.toFloat(),
            cardY.toFloat(),
            (cardX + cardWidth).toFloat(),
            (cardY + cardHeight).toFloat()
        )
        
        val whitePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, whitePaint)
        
        // Draw gradient top section for prompt
        val topSectionHeight = cardHeight * 0.5f // Top half
        val gradientRect = android.graphics.RectF(
            cardX.toFloat(),
            cardY.toFloat(),
            (cardX + cardWidth).toFloat(),
            cardY + topSectionHeight
        )
        
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(
                Color.parseColor("#E91E63"), // Pink 
                Color.parseColor("#9C27B0"), // Purple middle
                Color.parseColor("#673AB7")  // Deep purple
            )
        )
        gradientDrawable.setBounds(
            cardX,
            cardY,
            cardX + cardWidth,
            (cardY + topSectionHeight).toInt()
        )
        gradientDrawable.cornerRadii = floatArrayOf(
            cornerRadius, cornerRadius, // top-left
            cornerRadius, cornerRadius, // top-right
            0f, 0f, // bottom-right
            0f, 0f  // bottom-left
        )
        gradientDrawable.draw(canvas)
        
        // Draw prompt text in gradient section (white text, bigger)
        textPaint.color = Color.WHITE
        textPaint.textSize = 48f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        
        val promptLines = wrapText(userPrompt, textPaint, cardWidth - 80)
        val promptLineHeight = textPaint.textSize * 1.2f
        val promptTotalHeight = promptLines.size * promptLineHeight
        val promptStartY = cardY + (topSectionHeight / 2) - (promptTotalHeight / 2) + (promptLineHeight / 2)
        
        promptLines.forEachIndexed { index, line ->
            val lineY = promptStartY + (index * promptLineHeight)
            canvas.drawText(line, (canvasWidth / 2).toFloat(), lineY, textPaint)
        }
        
        // Draw message text in bottom white section (dark text, bigger)
        textPaint.color = Color.parseColor("#333333")
        textPaint.textSize = 42f
        textPaint.typeface = Typeface.DEFAULT
        
        val messageLines = wrapText(anonymousMessage, textPaint, cardWidth - 80)
        val messageLineHeight = textPaint.textSize * 1.2f
        val messageTotalHeight = messageLines.size * messageLineHeight
        val messageStartY = cardY + topSectionHeight + ((cardHeight - topSectionHeight) / 2) - (messageTotalHeight / 2) + (messageLineHeight / 2)
        
        messageLines.forEachIndexed { index, line ->
            val lineY = messageStartY + (index * messageLineHeight)
            canvas.drawText(line, (canvasWidth / 2).toFloat(), lineY, textPaint)
        }
    }
    
    private fun drawResponseCard(
        canvas: Canvas,
        text: String,
        canvasWidth: Int,
        cardY: Int,
        cardHeight: Int,
        textPaint: Paint,
        isPrompt: Boolean
    ) {
        val cardPadding = 60
        val cardX = cardPadding
        val cardWidth = canvasWidth - (cardPadding * 2)
        val cornerRadius = 25f
        
        // Create card like your second image: gradient top + white bottom
        val cardRect = android.graphics.RectF(
            cardX.toFloat(),
            cardY.toFloat(),
            (cardX + cardWidth).toFloat(),
            (cardY + cardHeight).toFloat()
        )
        
        // Draw white background first (full card)
        val whitePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, whitePaint)
        
        // Draw gradient top section (like your image)
        val gradientHeight = cardHeight * 0.4f // Top 40% is gradient
        val gradientRect = android.graphics.RectF(
            cardX.toFloat(),
            cardY.toFloat(),
            (cardX + cardWidth).toFloat(),
            cardY + gradientHeight
        )
        
        // Gradient colors based on card type
        val gradientColors = if (isPrompt) {
            intArrayOf(
                Color.parseColor("#E91E63"), // Pink
                Color.parseColor("#FF9800")  // Orange
            )
        } else {
            intArrayOf(
                Color.parseColor("#4CAF50"), // Green  
                Color.parseColor("#8BC34A")  // Light green
            )
        }
        
        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            gradientColors
        )
        gradientDrawable.setBounds(
            cardX,
            cardY,
            cardX + cardWidth,
            (cardY + gradientHeight).toInt()
        )
        gradientDrawable.cornerRadii = floatArrayOf(
            cornerRadius, cornerRadius, // top-left
            cornerRadius, cornerRadius, // top-right
            0f, 0f, // bottom-right
            0f, 0f  // bottom-left
        )
        gradientDrawable.draw(canvas)
        
        // Draw text in the white bottom section
        textPaint.color = Color.parseColor("#333333")
        textPaint.textSize = 42f
        
        val maxTextWidth = cardWidth - 80
        val lines = wrapText(text, textPaint, maxTextWidth)
        
        val lineHeight = textPaint.textSize * 1.2f
        val totalTextHeight = lines.size * lineHeight
        val textSectionY = cardY + gradientHeight + 40 // Start below gradient with padding
        val textStartY = textSectionY + ((cardHeight - gradientHeight - 80) / 2) - (totalTextHeight / 2) + (lineHeight / 2)
        
        lines.forEachIndexed { index, line ->
            val lineY = textStartY + (index * lineHeight)
            canvas.drawText(line, (canvasWidth / 2).toFloat(), lineY, textPaint)
        }
    }
    
    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)
            
            if (testWidth <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    // Single word is too long, add it anyway
                    lines.add(word)
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
    
    private fun drawProfile(
        canvas: Canvas,
        profileBitmap: Bitmap?,
        profileX: Int,
        profileY: Int,
        profileSize: Int,
        username: String,
        textPaint: Paint
    ) {
        if (profileBitmap != null) {
            // Create circular profile picture bitmap
            val circularProfile = Bitmap.createBitmap(profileSize, profileSize, Bitmap.Config.ARGB_8888)
            val circularCanvas = Canvas(circularProfile)
            
            // Create circular clipping path
            val path = Path()
            path.addCircle(profileSize / 2f, profileSize / 2f, profileSize / 2f, Path.Direction.CW)
            circularCanvas.clipPath(path)
            
            // Scale and draw the profile image
            val scaledProfile = Bitmap.createScaledBitmap(profileBitmap, profileSize, profileSize, true)
            circularCanvas.drawBitmap(scaledProfile, 0f, 0f, Paint().apply { isAntiAlias = true })
            
            // Draw white border circle first
            val borderPaint = Paint().apply {
                color = Color.WHITE
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }
            canvas.drawCircle(profileX.toFloat(), profileY.toFloat(), (profileSize / 2).toFloat(), borderPaint)
            
            // Draw the circular profile picture
            canvas.drawBitmap(
                circularProfile,
                (profileX - profileSize / 2).toFloat(),
                (profileY - profileSize / 2).toFloat(),
                Paint().apply { isAntiAlias = true }
            )
        } else {
            // Draw initial circle
            val circlePaint = Paint().apply {
                color = Color.parseColor("#3366FF")
                isAntiAlias = true
            }
            
            canvas.drawCircle(profileX.toFloat(), profileY.toFloat(), (profileSize / 2).toFloat(), circlePaint)
            
            // Draw initials
            val initials = username.take(2).uppercase()
            textPaint.textSize = (profileSize * 0.4).toFloat()
            textPaint.typeface = Typeface.DEFAULT_BOLD
            
            val textBounds = Rect()
            textPaint.getTextBounds(initials, 0, initials.length, textBounds)
            canvas.drawText(
                initials,
                profileX.toFloat(),
                profileY + textBounds.height() / 2f,
                textPaint
            )
        }
    }
    
    private fun drawVerificationCheckmark(
        fragment: Fragment,
        canvas: Canvas,
        profileX: Int,
        usernameWidth: Float,
        usernameY: Int,
        checkmarkSize: Int = 64
    ) {
        try {
            val checkmarkDrawable = ContextCompat.getDrawable(fragment.requireContext(), R.drawable.ic_verified_blue)
            checkmarkDrawable?.let { drawable ->
                val checkmarkX = (profileX + usernameWidth / 2 + 15).toInt()
                val checkmarkY = (usernameY - checkmarkSize / 2).toInt()
                
                drawable.setBounds(
                    checkmarkX,
                    checkmarkY - checkmarkSize / 2,
                    checkmarkX + checkmarkSize,
                    checkmarkY + checkmarkSize / 2
                )
                drawable.draw(canvas)
            }
        } catch (e: Exception) {
            // Fallback: Draw a simple blue circle with checkmark
            val checkmarkX = (profileX + usernameWidth / 2 + 15).toInt()
            val checkmarkY = usernameY - checkmarkSize / 2
            
            val checkPaint = Paint().apply {
                color = Color.parseColor("#1DA1F2")
                isAntiAlias = true
            }
            
            canvas.drawCircle(
                checkmarkX.toFloat(), 
                checkmarkY.toFloat(), 
                (checkmarkSize / 2).toFloat(), 
                checkPaint
            )
            
            val checkTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 28f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }
            
            canvas.drawText(
                "✓",
                checkmarkX.toFloat(),
                checkmarkY + 8f,
                checkTextPaint
            )
        }
    }
    
    private fun drawPromptText(canvas: Canvas, prompt: String, textPaint: Paint, width: Int, startY: Float) {
        val maxPromptWidth = width - 160
        val words = prompt.split(" ")
        var currentLine = ""
        var lineY = startY
        val lineHeight = 60f
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val lineWidth = textPaint.measureText(testLine)
            
            if (lineWidth <= maxPromptWidth && currentLine.count { it == ' ' } < 6) { // Max 6 words per line
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, (width / 2).toFloat(), lineY, textPaint)
                    lineY += lineHeight
                }
                currentLine = word
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, (width / 2).toFloat(), lineY, textPaint)
        }
    }
    
    private fun drawMessageCard(canvas: Canvas, message: String, width: Int, centerY: Int, textPaint: Paint) {
        // Draw card background
        val cardPaint = Paint().apply {
            color = Color.parseColor("#33FFFFFF") // Semi-transparent white
            isAntiAlias = true
        }
        
        val cardWidth = width - 120
        val cardHeight = 200
        val cardLeft = (width - cardWidth) / 2f
        val cardTop = (centerY - cardHeight / 2).toFloat()
        
        // Draw rounded rectangle background
        canvas.drawRoundRect(
            cardLeft,
            cardTop,
            cardLeft + cardWidth,
            cardTop + cardHeight,
            24f,
            24f,
            cardPaint
        )
        
        // Draw message text
        textPaint.textSize = 32f
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        
        // Wrap text within card
        val maxMessageWidth = cardWidth - 60
        val words = message.split(" ")
        var currentLine = ""
        var lineY = centerY - 30f
        val lineHeight = 45f
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val lineWidth = textPaint.measureText(testLine)
            
            if (lineWidth <= maxMessageWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, (width / 2).toFloat(), lineY, textPaint)
                    lineY += lineHeight
                }
                currentLine = word
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, (width / 2).toFloat(), lineY, textPaint)
        }
    }
    
    private fun drawLogoAndBranding(fragment: Fragment, canvas: Canvas, width: Int, height: Int, boldPaint: Paint) {
        try {
            val logoDrawable = ContextCompat.getDrawable(fragment.requireContext(), R.drawable.zay_main_logo)
            if (logoDrawable != null) {
                val logoSize = 80
                val logoX = (width / 2) - (logoSize / 2)
                val logoY = height - 250 // Moved further up from bottom to give more space from text
                
                // Create rounded logo bitmap
                val logoBitmap = Bitmap.createBitmap(logoSize, logoSize, Bitmap.Config.ARGB_8888)
                val logoCanvas = Canvas(logoBitmap)
                
                // Create rounded clipping path with corner radius
                val cornerRadius = 12f
                val path = Path()
                path.addRoundRect(0f, 0f, logoSize.toFloat(), logoSize.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
                logoCanvas.clipPath(path)
                
                // Draw the logo into the rounded bitmap
                logoDrawable.setBounds(0, 0, logoSize, logoSize)
                logoDrawable.draw(logoCanvas)
                
                // Draw the rounded logo bitmap onto the main canvas
                canvas.drawBitmap(logoBitmap, logoX.toFloat(), logoY.toFloat(), Paint().apply { isAntiAlias = true })
                
                // Add "Zay" text below logo for clarity
                val brandingY = height - 120 // Positioned below logo with proper spacing
                boldPaint.textSize = 32f
                boldPaint.color = Color.WHITE
                boldPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Zay", (width / 2).toFloat(), brandingY.toFloat(), boldPaint)
            } else {
                // Fallback to text only if logo fails to load
                val brandingY = height - 120
                boldPaint.textSize = 48f
                boldPaint.color = Color.WHITE
                boldPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("Zay", (width / 2).toFloat(), brandingY.toFloat(), boldPaint)
            }
        } catch (e: Exception) {
            // Fallback to text only if there's an error
            val brandingY = height - 120
            boldPaint.textSize = 48f
            boldPaint.color = Color.WHITE
            boldPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Zay", (width / 2).toFloat(), brandingY.toFloat(), boldPaint)
        }
    }
    
    private fun shareToInstagramStory(fragment: Fragment, storyImage: Bitmap, linkText: String) {
        try {
            // Facebook App ID (same as TutorialFragment)
            val facebookAppId = "4270194749933462"
            
            // Save image to cache directory
            val cacheDir = File(fragment.requireContext().cacheDir, "story_images")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val imageFile = File(cacheDir, "zay_story_${System.currentTimeMillis()}.png")
            val outputStream = FileOutputStream(imageFile)
            storyImage.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // Check if Instagram is installed first
            val instagramIntent = fragment.requireContext().packageManager.getLaunchIntentForPackage("com.instagram.android")
            if (instagramIntent == null) {
                Toast.makeText(fragment.requireContext(), "Instagram app not found. Please install Instagram to share stories.", Toast.LENGTH_LONG).show()
                return
            }
            
            // Create Instagram Stories URL with source_application
            val instagramStoriesUrl = "instagram-stories://share?source_application=$facebookAppId"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(instagramStoriesUrl)).apply {
                setPackage("com.instagram.android")
                
                // Add sticker functionality for movable elements
                val imageUri = FileProvider.getUriForFile(
                    fragment.requireContext(),
                    "${fragment.requireContext().packageName}.fileprovider",
                    imageFile
                )
                putExtra("interactive_asset_uri", imageUri)
                putExtra("content_url", linkText)
                putExtra("top_background_color", "#1A1A1A")
                putExtra("bottom_background_color", "#673AB7")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Copy link to clipboard
            val clipboard = fragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val linkClip = ClipData.newPlainText("Zay Link", linkText)
            clipboard.setPrimaryClip(linkClip)
            
            if (intent.resolveActivity(fragment.requireContext().packageManager) != null) {
                fragment.startActivity(intent)
                Toast.makeText(fragment.requireContext(), "📱 Story shared! Elements are movable. Link copied to clipboard.", Toast.LENGTH_LONG).show()
            } else {
                // Fallback to standard share intent
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/*"
                    val imageUri = FileProvider.getUriForFile(
                        fragment.requireContext(),
                        "${fragment.requireContext().packageName}.fileprovider",
                        imageFile
                    )
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    setPackage("com.instagram.android")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                if (shareIntent.resolveActivity(fragment.requireContext().packageManager) != null) {
                    fragment.startActivity(shareIntent)
                } else {
                    Toast.makeText(fragment.requireContext(), "Unable to share to Instagram Stories", Toast.LENGTH_SHORT).show()
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(fragment.requireContext(), "Error sharing to Instagram: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}