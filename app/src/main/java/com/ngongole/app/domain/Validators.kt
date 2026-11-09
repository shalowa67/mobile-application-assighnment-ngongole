package com.ngongole.app.domain

/**
 * Form validation rules. Each function returns an error message to show the user,
 * or null when the input is fine. Kept out of the screens so they can be unit tested.
 */
object Validators {

    const val MAX_AMOUNT_NGWEE = 100_000_000L // K 1,000,000.00

    fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Enter the customer's name"
            trimmed.length < 2 -> "Name is too short"
            trimmed.length > 50 -> "Name must be 50 characters or less"
            else -> null
        }
    }

    fun validatePhone(phone: String): String? = when {
        phone.isBlank() -> "Enter the customer's phone number"
        normalisePhone(phone) == null -> "Enter a valid Zambian mobile number, e.g. 0977 123456"
        else -> null
    }

    /**
     * Zambian mobile numbers: 095/096/097 or 075/076/077 followed by 7 digits.
     * Accepts "0971234567", "097 123 4567", "+260971234567" or "260971234567"
     * and always returns the standard form "+260971234567", or null if invalid.
     */
    fun normalisePhone(phone: String): String? {
        val digits = phone.replace(Regex("""[\s\-()]"""), "").removePrefix("+")
        val local = when {
            digits.startsWith("260") && digits.length == 12 -> digits.substring(3)
            digits.startsWith("0") && digits.length == 10 -> digits.substring(1)
            else -> return null
        }
        return if (ZAMBIAN_MOBILE.matches(local)) "+260$local" else null
    }

    /**
     * Checks a money amount. [maxNgwee] lets the payment form stop a customer
     * paying more than they owe.
     */
    fun validateAmount(text: String, maxNgwee: Long = MAX_AMOUNT_NGWEE, maxMessage: String? = null): String? {
        if (text.isBlank()) return "Enter an amount"
        val ngwee = Money.parseToNgwee(text) ?: return "Enter a number like 150 or 150.50"
        return when {
            ngwee <= 0 -> "Amount must be more than zero"
            ngwee > maxNgwee -> maxMessage ?: "Amount is too large"
            else -> null
        }
    }

    fun validateNote(note: String): String? =
        if (note.trim().length > 100) "Note must be 100 characters or less" else null

    // 9 digits after the leading 0: network code (95/96/97/75/76/77) + 7 digits
    private val ZAMBIAN_MOBILE = Regex("""^(9[5-7]|7[5-7])\d{7}$""")
}
