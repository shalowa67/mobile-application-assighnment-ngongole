package com.ngongole.app.domain

import java.text.NumberFormat
import java.util.Locale

/** Helpers for turning ngwee (whole numbers) into Kwacha text and back. */
object Money {

    /** 125050 ngwee -> "K 1,250.50" */
    fun format(ngwee: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val sign = if (ngwee < 0) "-" else ""
        val kwacha = Math.abs(ngwee) / 100.0
        return "${sign}K ${formatter.format(kwacha)}"
    }

    /**
     * Parses what the user typed ("50", "50.5", "1,250.75") into ngwee.
     * Returns null if the text is not a valid amount with at most 2 decimal places.
     */
    fun parseToNgwee(text: String): Long? {
        val cleaned = text.trim().replace(",", "").replace(" ", "")
        if (!AMOUNT_PATTERN.matches(cleaned)) return null
        val parts = cleaned.split(".")
        val kwacha = parts[0].ifEmpty { "0" }.toLongOrNull() ?: return null
        // "5" -> 50 ngwee, "75" -> 75 ngwee, missing -> 0 ngwee
        val ngwee = parts.getOrNull(1).orEmpty().padEnd(2, '0').toLong()
        return kwacha * 100 + ngwee
    }

    // Up to 9 digits of Kwacha, optionally a dot and 1-2 digits of ngwee.
    private val AMOUNT_PATTERN = Regex("""^\d{0,9}(\.\d{1,2})?$""")
}
