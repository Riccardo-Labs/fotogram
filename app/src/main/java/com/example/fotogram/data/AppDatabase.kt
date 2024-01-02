package com.example.fotogram.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fotogram.model.PostDetail

// Elenchiamo le tabelle (entities) e la versione del DB
@Database(entities = [PostDetail::class], version = 1)  // se app crasha dopo aver cambiato database è possibile modificare la versione per evitare di disinstallare l'app
abstract class AppDatabase : RoomDatabase() {


    abstract fun postDao(): PostDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fotogram_database" // Nome del file fisico sul telefono
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}