package com.ngongole.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Is this entry money the customer took on credit, or money they paid back? */
enum class EntryType { CREDIT, PAYMENT }

/**
 * One line in a customer's credit book: either goods taken on credit or a payment.
 *
 * Many entries belong to one customer (one-to-many). The foreign key with CASCADE
 * means that deleting a customer also deletes all of their entries.
 *
 * Money is stored in ngwee (1 Kwacha = 100 ngwee) as a whole number, because
 * decimal numbers (Double) cannot store values like 0.10 exactly.
 */
@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("customerId")]
)
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val type: EntryType,
    val amountNgwee: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
