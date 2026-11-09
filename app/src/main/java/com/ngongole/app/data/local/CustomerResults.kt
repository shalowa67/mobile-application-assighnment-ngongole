package com.ngongole.app.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Result of a query that joins a customer with the SUM of their entries.
 * Not a table: Room fills it from the columns returned by the query.
 */
data class CustomerBalance(
    @Embedded val customer: Customer,
    val balanceNgwee: Long
)

/**
 * A customer together with all of their ledger entries.
 * @Relation tells Room to run a second query that loads every LedgerEntry
 * whose customerId matches this customer's id (the one-to-many relationship).
 */
data class CustomerWithEntries(
    @Embedded val customer: Customer,
    @Relation(parentColumn = "id", entityColumn = "customerId")
    val entries: List<LedgerEntry>
) {
    val balanceNgwee: Long
        get() = entries.sumOf { if (it.type == EntryType.CREDIT) it.amountNgwee else -it.amountNgwee }
}
