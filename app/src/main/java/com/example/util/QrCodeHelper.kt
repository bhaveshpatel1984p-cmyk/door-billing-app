package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import com.example.data.db.CompanyProfileEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale

object QrCodeHelper {

    /**
     * Generates a QR Code Bitmap from given string content using ZXing.
     */
    fun generateQrBitmap(content: String, size: Int = 400): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
            val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Builds a standard UPI payment deep link string.
     */
    fun buildUpiString(upiId: String, payeeName: String, amount: Double? = null): String {
        val cleanUpi = upiId.trim()
        val encodedName = Uri.encode(payeeName.trim().ifBlank { "Door Billing" })
        val base = "upi://pay?pa=$cleanUpi&pn=$encodedName&cu=INR"
        return if (amount != null && amount > 0.0) {
            "$base&am=${String.format(Locale.US, "%.2f", amount)}"
        } else {
            base
        }
    }

    /**
     * Checks whether the company profile has any QR code configured (uploaded image or UPI ID).
     */
    fun hasQrCode(company: CompanyProfileEntity): Boolean {
        if (!company.qrCodeUri.isNullOrBlank()) {
            val file = File(company.qrCodeUri)
            if (file.exists() && file.length() > 0) return true
        }
        return company.upiId.isNotBlank()
    }

    /**
     * Retrieves or generates a Bitmap representation of the company's payment QR code.
     * Prioritizes the uploaded QR image if present; otherwise generates a UPI QR from upiId.
     */
    fun getPaymentQrBitmap(company: CompanyProfileEntity, amount: Double? = null, size: Int = 400): Bitmap? {
        // 1. Check custom uploaded QR image
        if (!company.qrCodeUri.isNullOrBlank()) {
            try {
                val file = File(company.qrCodeUri)
                if (file.exists() && file.length() > 0) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) return bmp
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Generate UPI QR code from UPI ID if provided
        if (company.upiId.isNotBlank()) {
            val upiPayload = buildUpiString(company.upiId, company.businessName, amount)
            return generateQrBitmap(upiPayload, size)
        }

        return null
    }

    /**
     * Returns Base64 PNG data string for embedding in HTML invoices and web previews.
     */
    fun getPaymentQrBase64(company: CompanyProfileEntity, amount: Double? = null): String? {
        // 1. Check custom uploaded QR image
        if (!company.qrCodeUri.isNullOrBlank()) {
            try {
                val file = File(company.qrCodeUri)
                if (file.exists() && file.length() > 0) {
                    val bytes = file.readBytes()
                    if (bytes.isNotEmpty()) {
                        return Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback to generating UPI QR bitmap and converting to Base64
        if (company.upiId.isNotBlank()) {
            val bmp = getPaymentQrBitmap(company, amount, size = 300)
            if (bmp != null) {
                try {
                    val stream = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val bytes = stream.toByteArray()
                    return Base64.encodeToString(bytes, Base64.NO_WRAP)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return null
    }
}
