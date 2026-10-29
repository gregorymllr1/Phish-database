package com.phishdb.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VenueEntity::class,
        SongEntity::class,
        ShowEntity::class,
        SetlistEntryEntity::class,
        SyncMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PhishDatabase : RoomDatabase() {
    abstract fun venueDao(): VenueDao
    abstract fun songDao(): SongDao
    abstract fun showDao(): ShowDao
    abstract fun setlistDao(): SetlistEntryDao
    abstract fun metaDao(): MetaDao

    companion object {
        fun build(context: Context): PhishDatabase =
            Room.databaseBuilder(context, PhishDatabase::class.java, "phish.db")
                // Seeded on first launch from the desktop database (`phish.ps1 android-db`).
                .createFromAsset("databases/phish.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
