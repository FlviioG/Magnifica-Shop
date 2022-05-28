package com.flavio.magnfica.infra

import android.content.Context
import com.flavio.magnfica.database.Cart
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.database.Pedido
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class SecurityPreferences(context: Context) {
    private val mSharedPreferences = context.getSharedPreferences("Magnifica", Context.MODE_PRIVATE)

    private fun getString(key: String): String {
        return mSharedPreferences.getString(key, "") ?: ""
    }

    fun storeInt(key: String, value: Int) {
        mSharedPreferences.edit().putInt(key, value).apply()
    }

    fun getInt(key: String): Int {
        return mSharedPreferences.getInt(key, 0)
    }

    fun saveFav(key: String, fav: Favorites) {
        val gson = Gson()
        val json = gson.toJson(fav)
        mSharedPreferences.edit().putString(key, json).apply()
    }

    fun getFav(key: String): Favorites {
        val gson = Gson()
        val json = getString(key)

        return gson.fromJson(json, Favorites::class.java)
    }

    fun saveCartList(key: String, item: List<Cart>) {
        val gson = Gson()
        val json = gson.toJson(item)
        mSharedPreferences.edit().putString(key, json).apply()
    }

    fun getCartItem(key: String): List<Cart> {
        val gson = Gson()
        val json = getString(key)
        val type: Type = object : TypeToken<List<Cart>>() {}.type
        return gson.fromJson(json, type)
    }

    fun savePedido(key: String, item: Pedido) {
        val gson = Gson()
        val json = gson.toJson(item)
        mSharedPreferences.edit().putString(key, json).apply()
    }

    fun getPedido(key: String): Pedido {
        val gson = Gson()
        val json = getString(key)
        val type: Type = object : TypeToken<Pedido>() {}.type
        return gson.fromJson(json, type)
    }

}