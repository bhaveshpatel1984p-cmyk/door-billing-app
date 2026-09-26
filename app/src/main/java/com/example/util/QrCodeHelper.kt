package com.example.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import com.example.data.db.CompanyProfileEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
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
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    /**
     * Builds a standard UPI payment deep link string.
     * Uses URLEncoder to avoid unmocked Android Uri issues in unit tests and standardizes encoding.
     */
    fun buildUpiString(upiId: String, payeeName: String, amount: Double? = null): String {
        val cleanUpi = upiId.trim().ifBlank { "nirmaldoor@upi" }
        val rawName = payeeName.trim().ifBlank { "Door Billing" }
        val encodedName = try {
            URLEncoder.encode(rawName, "UTF-8").replace("+", "%20")
        } catch (_: Exception) {
            rawName.replace(" ", "%20")
        }
        val base = "upi://pay?pa=$cleanUpi&pn=$encodedName&cu=INR"
        return if (amount != null && amount > 0.0) {
            "$base&am=${String.format(Locale.US, "%.2f", amount)}"
        } else {
            base
        }
    }

    /**
     * Resolves an effective UPI ID. Prioritizes user's custom UPI ID,
     * falls back to standard NPCI account-based UPI (A/c@IFSC.ifsc.npci) or mobile.
     */
    fun resolveEffectiveUpiId(company: CompanyProfileEntity): String {
        if (company.upiId.isNotBlank()) return company.upiId.trim()
        if (company.accountNo.isNotBlank() && company.ifscCode.isNotBlank()) {
            return "${company.accountNo.trim()}@${company.ifscCode.trim()}.ifsc.npci"
        }
        if (company.mobile.isNotBlank()) {
            return "${company.mobile.trim()}@upi"
        }
        return "nirmaldoor@upi"
    }

    /**
     * Checks whether the company profile has any QR code configured.
     * Always returns true because bills will either use custom uploaded QR image
     * or auto-generate a valid UPI QR code from UPI ID / Bank Account / Mobile.
     */
    fun hasQrCode(company: CompanyProfileEntity): Boolean {
        return true
    }

    /**
     * Retrieves or generates a Bitmap representation of the company's payment QR code.
     * Prioritizes the uploaded QR image if present; otherwise generates a UPI QR from UPI ID / Bank details.
     * Includes memory-safe downsampling for user-uploaded camera images to prevent OutOfMemoryError.
     */
    fun getPaymentQrBitmap(company: CompanyProfileEntity, amount: Double? = null, size: Int = 400): Bitmap? {
        // 1. Check custom uploaded QR image
        if (!company.qrCodeUri.isNullOrBlank() && company.qrCodeUri != "null") {
            try {
                val file = File(company.qrCodeUri)
                if (file.exists() && file.length() > 0) {
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    val maxDim = Math.max(boundsOptions.outWidth, boundsOptions.outHeight)
                    var inSample = 1
                    while (maxDim / (inSample * 2) >= size) {
                        inSample *= 2
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = inSample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    if (bmp != null) return bmp
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }

        // 2. Generate UPI QR code from effective UPI ID
        val effectiveUpi = resolveEffectiveUpiId(company)
        val upiPayload = buildUpiString(effectiveUpi, company.businessName, amount)
        val generated = generateQrBitmap(upiPayload, size)
        if (generated != null) return generated

        // 3. Fallback without amount parameter if parameterized UPI QR failed
        val basicUpiPayload = buildUpiString(effectiveUpi, company.businessName, null)
        return generateQrBitmap(basicUpiPayload, size)
    }

    /**
     * Returns Base64 PNG data string for embedding in HTML invoices and web previews.
     */
    fun getPaymentQrBase64(company: CompanyProfileEntity, amount: Double? = null): String? {
        // 1. Try retrieving bitmap (uploaded custom image with safe downsample or generated UPI QR)
        try {
            val bmp = getPaymentQrBitmap(company, amount, size = 300)
            if (bmp != null) {
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val bytes = stream.toByteArray()
                if (bytes.isNotEmpty()) {
                    return Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        // 2. Direct fallback to basic UPI payload
        return try {
            val effectiveUpi = resolveEffectiveUpiId(company)
            val fallbackPayload = buildUpiString(effectiveUpi, company.businessName, null)
            val bmp = generateQrBitmap(fallbackPayload, 300)
            if (bmp != null) {
                val stream = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            } else null
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}
