package com.example.util

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DimensionCalculator {

    private val dfCurrency = DecimalFormat("#,##,##0.00")
    private val dfSqFt = DecimalFormat("0.00")
    private val dfDimension = DecimalFormat("0.##")

    /**
     * Calculates Sq.Ft for door measurements.
     * @param height Height dimension
     * @param width Width dimension
     * @param qty Quantity of doors
     * @param unit "Inches" or "Feet"
     * Formula:
     * - Inches: (Height * Width / 144.0) * Qty
     * - Feet: (Height * Width) * Qty
     */
    fun calculateSqFt(height: Double, width: Double, qty: Int, unit: String): Double {
        return calculateSqFt(height, width, qty.toDouble(), unit)
    }

    fun calculateSqFt(height: Double, width: Double, qty: Double, unit: String): Double {
        if (height <= 0 || width <= 0 || qty <= 0.0) return 0.0
        val baseSqFt = if (unit.equals("Inches", ignoreCase = true)) {
            (height * width) / 144.0
        } else {
            height * width
        }
        val total = baseSqFt * qty
        return Math.round(total * 100.0) / 100.0
    }

    fun formatQtyWithUnit(qty: Double, unit: String?): String {
        val effectiveUnit = if (unit.isNullOrBlank()) "Pcs" else unit.trim()
        val qtyStr = if (qty % 1.0 == 0.0) qty.toLong().toString() else String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
        return "$qtyStr $effectiveUnit"
    }

    fun formatQtyOnly(qty: Double): String {
        return if (qty % 1.0 == 0.0) qty.toLong().toString() else String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
    }

    /**
     * Calculates Amount = Sq.Ft * Rate
     */
    fun calculateAmount(sqFt: Double, rate: Double): Double {
        val total = sqFt * rate
        return Math.round(total * 100.0) / 100.0
    }

    fun formatCurrency(amount: Double): String {
        return "₹" + dfCurrency.format(amount)
    }

    fun formatSqFt(sqFt: Double): String {
        return dfSqFt.format(sqFt) + " Sq.Ft"
    }

    fun formatDimension(value: Double): String {
        return dfDimension.format(value)
    }

    fun formatDate(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    /**
     * Converts a number into Indian Rupees words.
     * E.g. 15420.50 -> "Rupees Fifteen Thousand Four Hundred Twenty and Fifty Paise Only"
     */
    fun convertToIndianCurrencyWords(amount: Double): String {
        val units = arrayOf(
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
        )
        val tens = arrayOf(
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
        )

        fun convertLessThanOneThousand(number: Int): String {
            var current = ""
            var n = number
            if (n % 100 < 20) {
                current = units[n % 100]
                n /= 100
            } else {
                current = units[n % 10]
                n /= 10
                current = tens[n % 10] + if (current.isNotEmpty()) " $current" else ""
                n /= 10
            }
            if (n == 0) return current
            return units[n] + " Hundred" + if (current.isNotEmpty()) " and $current" else ""
        }

        if (amount == 0.0) return "Zero Rupees Only"

        val wholePart = amount.toLong()
        val paisePart = Math.round((amount - wholePart) * 100).toInt()

        var num = wholePart
        var result = ""

        val crore = (num / 10000000).toInt()
        num %= 10000000
        val lakh = (num / 100000).toInt()
        num %= 100000
        val thousand = (num / 1000).toInt()
        num %= 1000
        val hundred = num.toInt()

        if (crore > 0) {
            result += convertLessThanOneThousand(crore) + " Crore "
        }
        if (lakh > 0) {
            result += convertLessThanOneThousand(lakh) + " Lakh "
        }
        if (thousand > 0) {
            result += convertLessThanOneThousand(thousand) + " Thousand "
        }
        if (hundred > 0) {
            result += convertLessThanOneThousand(hundred)
        }

        result = result.trim()
        val rupeesStr = if (result.isNotEmpty()) "Rupees $result" else ""
        val paiseStr = if (paisePart > 0) {
            val pText = convertLessThanOneThousand(paisePart)
            if (rupeesStr.isNotEmpty()) " and $pText Paise" else "$pText Paise"
        } else ""

        return "$rupeesStr$paiseStr Only".trim()
    }

    /**
     * Formats jurisdiction clause consistently to:
     * "SUBJECT TO <PLACE> JURISDICTION ONLY"
     * Handles inputs like "Kudachi", "Subject to Kudachi Jurisdiction only",
     * or previously duplicated strings like "SUBJECT TO SUBJECT TO ...".
     */
    fun formatJurisdictionClause(raw: String?): String {
        var clean = raw?.trim()?.uppercase(Locale.getDefault()) ?: ""
        if (clean.isBlank()) return "SUBJECT TO KUDACHI JURISDICTION ONLY"

        while (clean.startsWith("SUBJECT TO")) {
            clean = clean.removePrefix("SUBJECT TO").trim()
        }
        var changed = true
        while (changed) {
            changed = false
            if (clean.endsWith("ONLY")) {
                clean = clean.removeSuffix("ONLY").trim()
                changed = true
            }
            if (clean.endsWith("JURISDICTION")) {
                clean = clean.removeSuffix("JURISDICTION").trim()
                changed = true
            }
        }
        val place = if (clean.isBlank()) "KUDACHI" else clean
        return "SUBJECT TO $place JURISDICTION ONLY"
    }
}
