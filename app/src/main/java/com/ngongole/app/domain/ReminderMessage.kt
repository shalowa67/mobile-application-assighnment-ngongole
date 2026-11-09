package com.ngongole.app.domain

/** Builds the ready-written text sent to a customer by SMS or WhatsApp. */
object ReminderMessage {

    fun build(customerName: String, balanceNgwee: Long): String {
        val firstName = customerName.trim().substringBefore(" ")
        return "Hello $firstName, this is a friendly reminder that your balance of " +
            "${Money.format(balanceNgwee)} at our shop is still unpaid. " +
            "Please pay when you can. Thank you!"
    }
}
