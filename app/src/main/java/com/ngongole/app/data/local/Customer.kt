package com.ngongole.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row in the "customers" table: a person who buys on credit.
 *
 * The phone number is stored in one standard form (+260XXXXXXXXX) and is unique,
 * so the same customer cannot be added twice.
 */
@Entity(
    tableName = "customers",
    indices = [Index(value = ["phone"], unique = true)]
)
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
