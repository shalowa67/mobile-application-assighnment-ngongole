package com.ngongole.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO = Data Access Object. Every database operation the app needs is declared here;
 * Room generates the real implementation at build time (via KSP).
 *
 * Functions returning Flow are "observable": Room re-runs the query and emits a new
 * result whenever the tables change, so the screens update automatically.
 */
@Dao
interface CustomerDao {

    @Insert
    suspend fun insertCustomer(customer: Customer): Long

    @Insert
    suspend fun insertEntry(entry: LedgerEntry): Long

    /**
     * Adds a customer and their first credit entry together. @Transaction means
     * either both rows are saved or neither is (no customer without a balance).
     */
    @Transaction
    suspend fun insertCustomerWithOpeningCredit(customer: Customer, amountNgwee: Long, note: String): Long {
        val id = insertCustomer(customer)
        insertEntry(LedgerEntry(customerId = id, type = EntryType.CREDIT, amountNgwee = amountNgwee, note = note))
        return id
    }

    /**
     * Every customer with their balance (credit minus payments), filtered by name or phone.
     * LEFT JOIN keeps customers even if they somehow have no entries (balance 0).
     */
    @Query(
        """
        SELECT c.*, COALESCE(SUM(CASE WHEN e.type = 'CREDIT' THEN e.amountNgwee ELSE -e.amountNgwee END), 0) AS balanceNgwee
        FROM customers c
        LEFT JOIN ledger_entries e ON e.customerId = c.id
        WHERE c.name LIKE '%' || :search || '%' OR c.phone LIKE '%' || :search || '%'
        GROUP BY c.id
        ORDER BY balanceNgwee DESC, c.name COLLATE NOCASE ASC
        """
    )
    fun observeCustomers(search: String): Flow<List<CustomerBalance>>

    /** Total money owed to the shop by all customers. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amountNgwee ELSE -amountNgwee END), 0)
        FROM ledger_entries
        """
    )
    fun observeTotalOwed(): Flow<Long>

    @Transaction
    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun observeCustomerWithEntries(customerId: Long): Flow<CustomerWithEntries?>

    @Query("SELECT COUNT(*) FROM customers WHERE phone = :phone")
    suspend fun countWithPhone(phone: String): Int

    /**
     * Customers who still owe money and have not paid anything since [cutoffMillis].
     * "Owing since" = their last payment, or their first credit if they never paid.
     * Used by the daily reminder worker.
     */
    @Query(
        """
        SELECT c.*, SUM(CASE WHEN e.type = 'CREDIT' THEN e.amountNgwee ELSE -e.amountNgwee END) AS balanceNgwee
        FROM customers c
        JOIN ledger_entries e ON e.customerId = c.id
        GROUP BY c.id
        HAVING SUM(CASE WHEN e.type = 'CREDIT' THEN e.amountNgwee ELSE -e.amountNgwee END) > 0
           AND COALESCE(MAX(CASE WHEN e.type = 'PAYMENT' THEN e.createdAt END), MIN(e.createdAt)) <= :cutoffMillis
        ORDER BY balanceNgwee DESC
        """
    )
    suspend fun getOverdueCustomers(cutoffMillis: Long): List<CustomerBalance>

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomer(customerId: Long)
}
