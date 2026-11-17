package com.example.m2tmdbluque.data.db

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.m2tmdbluque.BuildConfig
import java.util.concurrent.Executors

@Database(entities = [SocialBarEntity::class], version = 1, exportSchema = false)
abstract class TmdbDatabase : RoomDatabase() {
    abstract fun socialBarDao(): SocialBarDao

    companion object {
        @Volatile
        private var instance: TmdbDatabase? = null

        fun getInstance(context: Context): TmdbDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        private fun buildDatabase(context: Context): TmdbDatabase =
            Room.databaseBuilder(
                context,
                TmdbDatabase::class.java,
                "tmdb.db",
            ).apply {
                if (BuildConfig.DEBUG) {
                    setQueryCallback(
                        { sql, args -> Log.d("RoomSQL", "SQL: $sql args: $args") },
                        Executors.newSingleThreadExecutor(),
                    )
                }
            }.build()
    }
}
