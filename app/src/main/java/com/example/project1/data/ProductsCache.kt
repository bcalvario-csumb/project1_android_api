package com.example.project1.data

import android.content.Context

//Carlos is working on this page
/**
 * The goal of this page is to be able to ache our API calls
 * this will also help us in the future when turing our API calls
 * into actual trading cards
 *
 *
 * 
 */
class ProductsCache(context: Context) {
    private val p = context.getSharedPreferences("products_cache", Context.MODE_PRIVATE)

    fun saveProducts(json: String) {
        p.edit().putString("cached_products", json).apply()
    }

    fun getProducts(): String? {
        return p.getString("cached_products", null)
    }
}