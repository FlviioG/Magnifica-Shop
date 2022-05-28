package com.flavio.magnfica.database

import android.content.Context
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

@Database(entities = [Favorites::class, Cart::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoritesDao(): Dao
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "favorites"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }

}

class Converters {

    @TypeConverter
    fun estoqueListFromJson(value: String): List<Estoque> {
        val type: Type = object : TypeToken<List<Estoque>>() {}.type
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun estoqueListToJson(value: List<Estoque>): String {
        return Gson().toJson(value)
    }
    @TypeConverter
    fun estoqueFromJson(value: String): Estoque {
        return Gson().fromJson(value, Estoque::class.java)
    }

    @TypeConverter
    fun estoqueToJson(value: Estoque): String {
        return Gson().toJson(value)
    }

}
