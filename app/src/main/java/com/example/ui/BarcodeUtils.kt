package com.example.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import java.util.EnumMap

object BarcodeUtils {
    /**
     * Generates a QR Code as an ImageBitmap.
     */
    fun generateQRCode(text: String, size: Int = 512): ImageBitmap? {
        if (text.isEmpty()) return null
        return try {
            val qrCodeWriter = com.google.zxing.qrcode.QRCodeWriter()
            val hints = EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java)
            hints[com.google.zxing.EncodeHintType.MARGIN] = 1 // 1 block white border
            hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"
            
            val bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, 
                        y, 
                        if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a Code 128 Barcode as an ImageBitmap.
     */
    fun generateBarcode128(text: String, width: Int = 600, height: Int = 150): ImageBitmap? {
        if (text.isEmpty()) return null
        return try {
            val writer = MultiFormatWriter()
            val hints = EnumMap<com.google.zxing.EncodeHintType, Any>(com.google.zxing.EncodeHintType::class.java)
            hints[com.google.zxing.EncodeHintType.MARGIN] = 2
            
            val bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, width, height, hints)
            val finalWidth = bitMatrix.width
            val finalHeight = bitMatrix.height
            val bitmap = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
            
            for (x in 0 until finalWidth) {
                for (y in 0 until finalHeight) {
                    bitmap.setPixel(
                        x, 
                        y, 
                        if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    )
                }
            }
            bitmap.asImageBitmap()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
