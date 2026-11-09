package com.ngongole.app.data

import com.ngongole.app.data.local.Customer
import com.ngongole.app.data.local.CustomerBalance
import com.ngongole.app.data.local.CustomerDao
import com.ngongole.app.data.local.CustomerWithEntries
import com.ngongole.app.data.local.EntryType
import com.ngongole.app.data.local.LedgerEntry
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * The single place the rest of the app goes to for customer data.
 * ViewModels and the reminder worker talk to this class, never to Room directly,
 * so the screens do not need to know how or where data is stored.
 */
class CustomerRepository(private val dao: CustomerDao) {

    fun observeCustomers(search: String): Flow<List<CustomerBalance>> =
        dao.observeCustomers(search.trim())

    fun observeTotalOwed(): Flow<Long> = dao.observeTotalOwed()

    fun observeCustomer(customerId: Long): Flow<CustomerWithEntries?> =
        dao.observeCustomerWithEntries(customerId)

    suspend fun phoneExists(phone: String): Boolean = dao.countWithPhone(phone) > 0

    suspend fun addCustomer(name: String, phone: String, openingCreditNgwee: Long, note: String): Long =
        dao.insertCustomerWithOpeningCredit(
            customer = Customer(name = name.trim(), phone = phone, note = note.trim()),
            amountNgwee = openingCreditNgwee,
            note = note.trim()
        )

    suspend fun addEntry(customerId: Long, type: EntryType, amountNgwee: Long, note: String) {
        dao.insertEntry(
            LedgerEntry(customerId = customerId, type = type, amountNgwee = amountNgwee, note = note.trim())
        )
    }

    suspend fun deleteCustomer(customerId: Long) = dao.deleteCustomer(customerId)

    /** Customers who have owed money for [days] days or more without paying. */
    suspend fun getOverdueCustomers(days: Int = OVERDUE_DAYS, now: Long = System.currentTimeMillis()): List<CustomerBalance> =
        dao.getOverdueCustomers(cutoffMillis = now - TimeUnit.DAYS.toMillis(days.toLong()))

    companion object {
        const val OVERDUE_DAYS = 7
    }
}
