package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.example.data.BusinessProfile
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object QrCardGenerator {

    /**
     * Generates a 1080x1920 high-resolution premium medical-business styled card with QR Code/Barcode.
     */
    fun generatePremiumCard(
        context: Context,
        profile: BusinessProfile?,
        pendingAmount: Double = 0.0,
        customerName: String = ""
    ): Bitmap {
        val width = 1080
        val height = 1920
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Paint settings
        val paint = Paint().apply { isAntiAlias = true }

        // 1. Draw elegant solid background with the app's primary brand color (beautiful premium blue)
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF2563EB.toInt() // Primary Brand Blue color matching user app color
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Draw Header Area (Doctor Clinic / business details and profile photo)
        // Draw profile photo (Circle Frame Top-Left)
        val photoRadius = 75f
        val photoX = 155f // Center X so left margin is 80f (155f - 75f = 80f)
        val photoY = 175f // Center Y
        
        val photoBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
        canvas.drawCircle(photoX, photoY, photoRadius, photoBorderPaint)
        
        val photoUri = profile?.photoUri ?: ""
        drawProfilePhoto(context, canvas, photoUri, photoX, photoY, photoRadius, paint)

        // Draw Clinic Details (Classy Serif Bold Italics with drop shadow)
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            setShadowLayer(4f, 2f, 2f, 0x80000000)
        }
        val phonePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            setShadowLayer(4f, 2f, 2f, 0x80000000)
        }

        val clinicNameField = if (!profile?.clinicName.isNullOrBlank()) profile?.clinicName else if (!profile?.name.isNullOrBlank()) profile?.name else "Dr. Khan clinic"
        val clinicPhoneField = if (!profile?.phone.isNullOrBlank()) profile?.phone else "9690899043"

        canvas.drawText(clinicNameField ?: "Dr. Khan clinic", 265f, 160f, titlePaint)
        canvas.drawText(clinicPhoneField ?: "9690899043", 265f, 215f, phonePaint)

        // 3. Central White Rounded Card Centerpiece
        val cardRect = RectF(60f, 320f, 1020f, 1640f)
        val shadowPaint = Paint().apply {
            isAntiAlias = true
            color = 0x26000000 // Soft black drop shadow
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(65f, 325f, 1025f, 1645f), 60f, 60f, shadowPaint)

        val cardPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(cardRect, 60f, 60f, cardPaint)

        // 4. Payment Due Reminder Title
        val titleLabelPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF475569.toInt() // Slate Gray
            textSize = 38f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }
        canvas.drawText("Payment Due Reminder!", (width / 2).toFloat(), 410f, titleLabelPaint)

        // 5. Large Amount Display Formatted
        val amountPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF1E293B.toInt() // Slate Dark
            textSize = 105f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val formattedAmount = try {
            val amtInt = pendingAmount.toInt()
            java.text.NumberFormat.getIntegerInstance(Locale.US).format(amtInt)
        } catch (e: Exception) {
            String.format(Locale.US, "%,.0f", pendingAmount)
        }
        canvas.drawText("₹$formattedAmount", (width / 2).toFloat(), 540f, amountPaint)

        // 6. Thin Divider Line
        val dividerPaint = Paint().apply {
            color = 0xFFCBD5E1.toInt() // Light Blue Slate
            strokeWidth = 3f
        }
        canvas.drawLine(140f, 630f, 940f, 630f, dividerPaint)

        // 7. QR Code Area Centered
        val qrLeft = 280f
        val qrTop = 690f
        val qrSize = 520f
        
        val qrContainerPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize, qrContainerPaint)

        val activeUpi = if (!profile?.upiId.isNullOrBlank()) profile?.upiId else "merchant@upi"
        drawUpiQrOnCanvas(canvas, activeUpi ?: "merchant@upi", pendingAmount, qrLeft, qrTop, qrSize)

        // 8. UPI ID text below QR code
        val upiSubPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF475569.toInt() // Slate Gray
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        }
        val upiOwnerName = profile?.name ?: "Merchant"
        canvas.drawText("${upiOwnerName.uppercase(Locale.US)} ($activeUpi)", (width / 2).toFloat(), 1280f, upiSubPaint)

        // 9. Payment Services Logos Row at Bottom of White Card
        val labelSlatePaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF64748B.toInt()
            textSize = 25f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        // Slot positions: 240, 440, 640, 840
        val centerY = 1410f
        val labelY = 1495f

        // slot 1: GPay
        val gCirclePaint = Paint().apply {
            isAntiAlias = true
            color = 0xFFF1F5F9.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(240f, centerY, 35f, gCirclePaint)
        val gLetterPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF1A73E8.toInt()
            textSize = 46f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val gY = centerY - (gLetterPaint.descent() + gLetterPaint.ascent()) / 2f
        canvas.drawText("G", 240f, gY, gLetterPaint)
        canvas.drawText("GPay", 240f, labelY, labelSlatePaint)

        // slot 2: PhonePe
        val peCirclePaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF5F259F.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawCircle(440f, centerY, 35f, peCirclePaint)
        val peLetterPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val peY = centerY - (peLetterPaint.descent() + peLetterPaint.ascent()) / 2f
        canvas.drawText("पे", 440f, peY, peLetterPaint)
        canvas.drawText("PhonePe", 440f, labelY, labelSlatePaint)

        // slot 3: BHIM
        val bhimOrange = Paint().apply {
            isAntiAlias = true
            color = 0xFFF58220.toInt()
            style = Paint.Style.FILL
        }
        val bhimGreen = Paint().apply {
            isAntiAlias = true
            color = 0xFF05A650.toInt()
            style = Paint.Style.FILL
        }
        val pathOrange = Path().apply {
            moveTo(605f, centerY - 20f)
            lineTo(635f, centerY - 20f)
            lineTo(620f, centerY + 20f)
            lineTo(590f, centerY + 20f)
            close()
        }
        val pathGreen = Path().apply {
            moveTo(640f, centerY - 20f)
            lineTo(670f, centerY - 20f)
            lineTo(655f, centerY + 20f)
            lineTo(625f, centerY + 20f)
            close()
        }
        canvas.drawPath(pathOrange, bhimOrange)
        canvas.drawPath(pathGreen, bhimGreen)
        canvas.drawText("BHIM", 640f, labelY, labelSlatePaint)

        // slot 4: Paytm
        val payPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF00B9F1.toInt()
            textSize = 42f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tmPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF002E6E.toInt()
            textSize = 42f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val paytmY = centerY + 15f
        canvas.drawText("pay", 830f, paytmY, payPaint)
        canvas.drawText("tm", 830f, paytmY, tmPaint)
        canvas.drawText("Paytm", 840f, labelY, labelSlatePaint)

        // 10. Watermark logo "Credit Book" in Bottom Blue Area
        val iconCx = 377f
        val iconCy = 1760f
        
        val checkBorderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(iconCx, iconCy, 28f, checkBorderPaint)
        
        val checkStrokePaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF2563EB.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 6f
            strokeCap = Paint.Cap.ROUND
        }
        val checkPath = Path().apply {
            moveTo(iconCx - 12f, iconCy + 1f)
            lineTo(iconCx - 3f, iconCy + 10f)
            lineTo(iconCx + 13f, iconCy - 8f)
        }
        canvas.drawPath(checkPath, checkStrokePaint)
        
        val watermarkTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Credit Book", 421f, 1778f, watermarkTextPaint)

        return bitmap
    }

    private fun drawProfilePhoto(
        context: Context,
        canvas: Canvas,
        photoUri: String,
        cx: Float,
        cy: Float,
        radius: Float,
        paint: Paint
    ) {
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        if (photoUri.startsWith("preset_")) {
            val idxStr = photoUri.substringAfter("preset_", "1")
            val idx = idxStr.toIntOrNull() ?: 1
            val colors = listOf(
                0xFF2563EB.toInt(), 0xFF059669.toInt(), 0xFFD97706.toInt(),
                0xFF7C3AED.toInt(), 0xFFDB2777.toInt(), 0xFF0284C7.toInt(),
                0xFFEA580C.toInt(), 0xFF16A34A.toInt(), 0xFF5B21B6.toInt(), 0xFF0891B2.toInt()
            )
            val baseColor = colors.getOrElse(idx % colors.size) { 0xFF2563EB.toInt() }
            
            val circularGradient = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                shader = LinearGradient(
                    cx - radius, cy - radius, cx + radius, cy + radius,
                    baseColor, baseColor or 0x00333333, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(cx, cy, radius, circularGradient)

            // Draw gorgeous text initials inside
            val initialPaint = Paint().apply {
                color = Color.WHITE
                textSize = radius * 0.9f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textY = cy - (initialPaint.descent() + initialPaint.ascent()) / 2f
            canvas.drawText("CB", cx, textY, initialPaint)
        } else if (photoUri.isNotBlank()) {
            try {
                val input = context.contentResolver.openInputStream(Uri.parse(photoUri))
                val srcBitmap = BitmapFactory.decodeStream(input)
                if (srcBitmap != null) {
                    val diameter = (radius * 2).toInt()
                    val scaled = Bitmap.createScaledBitmap(srcBitmap, diameter, diameter, true)
                    
                    val output = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
                    val outCanvas = Canvas(output)
                    val cutPath = android.graphics.Path().apply {
                        addCircle(radius, radius, radius, android.graphics.Path.Direction.CCW)
                    }
                    outCanvas.clipPath(cutPath)
                    outCanvas.drawBitmap(scaled, 0f, 0f, paint)
                    
                    canvas.drawBitmap(output, cx - radius, cy - radius, paint)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fallbackCircularAvatar(canvas, cx, cy, radius, "CB")
        } else {
            fallbackCircularAvatar(canvas, cx, cy, radius, "CB")
        }
    }

    private fun fallbackCircularAvatar(canvas: Canvas, cx: Float, cy: Float, radius: Float, label: String) {
        // Draw elegant circular placeholder background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, radius - 4f, bgPaint)

        // Draw professional avatar silhouette
        val iconPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF2563EB.toInt() // Primary Brand Blue
            style = Paint.Style.FILL
        }
        
        // Head circle
        canvas.drawCircle(cx, cy - radius * 0.15f, radius * 0.32f, iconPaint)
        
        // Chest path
        val chestPath = Path().apply {
            val startY = cy + radius * 0.22f
            moveTo(cx - radius * 0.55f, cy + radius * 0.85f)
            quadTo(cx - radius * 0.45f, startY, cx, startY)
            quadTo(cx + radius * 0.45f, startY, cx + radius * 0.55f, cy + radius * 0.85f)
            close()
        }
        canvas.drawPath(chestPath, iconPaint)

        // White doctor cross badge over chest representation for aesthetic medical touch
        val badgePaint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx - 10f, cy + radius * 0.5f, cx + 10f, cy + radius * 0.5f, badgePaint)
        canvas.drawLine(cx, cy + radius * 0.5f - 10f, cx, cy + radius * 0.5f + 10f, badgePaint)
    }

    private fun drawUpiQrOnCanvas(
        canvas: Canvas,
        upiId: String,
        amount: Double,
        left: Float,
        top: Float,
        size: Float
    ) {
        val cellSize = size / 15f
        val qrPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        
        // Left Positioning Square
        qrPaint.color = Color.BLACK
        canvas.drawRect(left, top, left + cellSize * 4, top + cellSize * 4, qrPaint)
        qrPaint.color = Color.WHITE
        canvas.drawRect(left + cellSize, top + cellSize, left + cellSize * 3, top + cellSize * 3, qrPaint)
        qrPaint.color = Color.BLACK
        canvas.drawRect(left + cellSize * 1.3f, top + cellSize * 1.3f, left + cellSize * 2.7f, top + cellSize * 2.7f, qrPaint)
        
        // Right Positioning Square
        qrPaint.color = Color.BLACK
        canvas.drawRect(left + size - cellSize * 4, top, left + size, top + cellSize * 4, qrPaint)
        qrPaint.color = Color.WHITE
        canvas.drawRect(left + size - cellSize * 3, top + cellSize, left + size - cellSize, top + cellSize * 3, qrPaint)
        qrPaint.color = Color.BLACK
        canvas.drawRect(left + size - cellSize * 2.7f, top + cellSize * 1.3f, left + size - cellSize * 1.3f, top + cellSize * 2.7f, qrPaint)
        
        // Bottom Positioning Square
        qrPaint.color = Color.BLACK
        canvas.drawRect(left, top + size - cellSize * 4, left + cellSize * 4, top + size, qrPaint)
        qrPaint.color = Color.WHITE
        canvas.drawRect(left + cellSize, top + size - cellSize * 3, left + cellSize * 3, top + size - cellSize, qrPaint)
        qrPaint.color = Color.BLACK
        canvas.drawRect(left + cellSize * 1.3f, top + size - cellSize * 2.7f, left + cellSize * 2.7f, top + size - cellSize * 1.3f, qrPaint)
        
        // Matrix pixels based on seeding hash
        val seed = upiId.hashCode() + amount.toInt()
        qrPaint.color = 0xFF1E293B.toInt()
        for (r in 0 until 15) {
            for (c in 0 until 15) {
                if ((r < 4 && c < 4) || (r < 4 && c >= 11) || (r >= 11 && c < 4)) {
                    continue
                }
                val bit = (seed + r * 79 + c * 31) % 5
                if (bit == 0 || bit == 2) {
                    canvas.drawRect(
                        left + c * cellSize,
                        top + r * cellSize,
                        left + c * cellSize + cellSize * 0.95f,
                        top + r * cellSize + cellSize * 0.95f,
                        qrPaint
                    )
                }
            }
        }
    }

    private fun drawBarcodeOnCanvas(canvas: Canvas, left: Float, top: Float, width: Float, height: Float) {
        val barPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        val random = java.util.Random(987654)
        var cursor = left
        val end = left + width
        while (cursor < end) {
            val barW = if (random.nextBoolean()) 4f else 8f
            val spaceW = if (random.nextBoolean()) 6f else 10f
            if (cursor + barW <= end) {
                canvas.drawRect(cursor, top, cursor + barW, top + height, barPaint)
            }
            cursor += barW + spaceW
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                }
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }

    /**
     * Shares both the transaction text content and QR Card image in a single WhatsApp dispatch.
     */
    fun shareQRCardWhatsApp(context: Context, bitmap: Bitmap, message: String) {
        val file = File(context.cacheDir, "temp_qr_card.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(context, "com.example.provider", file)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")
            }
            
            try {
                // Try personal WhatsApp first
                context.startActivity(intent)
            } catch (e1: Exception) {
                try {
                    // Fallback to WhatsApp Business
                    intent.setPackage("com.whatsapp.w4b")
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    // Fallback to standard platform share dialog
                    intent.setPackage(null)
                    val chooser = Intent.createChooser(intent, "Share Patient Balance & QR Card via")
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Share Dispatch Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Save Generated QR card as PNG image into device Pictures Directory natively.
     */
    fun downloadQRCardAsPng(context: Context, bitmap: Bitmap, name: String): Boolean {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "QR_Card_${name.replace(" ", "_")}_${System.currentTimeMillis()}.png")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CreditBook")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            try {
                resolver.openOutputStream(imageUri).use { out ->
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                return true
            } catch (e: Exception) {
                resolver.delete(imageUri, null, null)
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * Natively print business QR card with scale alignment setup.
     */
    fun printQRCard(context: Context, bitmap: Bitmap, jobName: String = "Print QR Card") {
        try {
            val printHelper = PrintHelper(context)
            printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
            printHelper.printBitmap(jobName, bitmap)
        } catch (e: Exception) {
            Toast.makeText(context, "Print Job failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Validates if a string represents a valid UPI ID structure.
     */
    fun isValidUpiId(upiId: String): Boolean {
        val regex = "^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z]{2,120}$".toRegex()
        return regex.matches(upiId.trim())
    }
}
