package com.flavio.magnfica.database

import androidx.room.*
import androidx.room.Dao

@Dao
interface Dao {

    @Query("SELECT * FROM favorites")
    fun getAll(): MutableList<Favorites>

    //    @Query("SELECT * FROM favorites WHERE id IN (:id)")
//    fun finById(id: Int): List<Favorites>

    @Insert
    fun insertAll(vararg favs: Favorites)

    @Delete
    fun delete(fav: Favorites)

    @Query("DELETE FROM favorites")
    fun deleteAll()
}

@Dao
interface CartDao {

    @Query("SELECT * FROM cart")
    fun getAll(): MutableList<Cart>

    //    @Query("SELECT * FROM favorites WHERE id IN (:id)")
//    fun finById(id: Int): List<Favorites>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg item: Cart)

    @Delete
    fun delete(item: Cart)

    @Query("DELETE FROM cart")
    fun deleteAll()
}