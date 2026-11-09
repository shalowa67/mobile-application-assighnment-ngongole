package com.ngongole.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database. Lists the tables (entities) and gives access to the DAO.
 * If you add a field to an entity, increase [version] and add a migration.
 */
@Database(
    entities = [Customer::class, LedgerEntry::class],
    version = 1,
    exportSchema = true
)
abstract class NgongoleDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao

    companion object {
        @Volatile
        private var instance: NgongoleDatabase? = null

        /** Returns the single database instance, creating it the first time. */
        fun getInstance(context: Context): NgongoleDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NgongoleDatabase::class.java,
                    "ngongole.db"
                ).build().also { instance = it }
            }
    }
}
