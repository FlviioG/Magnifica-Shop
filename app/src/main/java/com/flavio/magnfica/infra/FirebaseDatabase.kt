package com.flavio.magnfica.infra

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.flavio.magnfica.database.Estoque
import com.flavio.magnfica.database.Favorites
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


class FirebaseDatabase : Service() {

    companion object {
        val getList = ArrayList<Favorites>()
        var isReady = false
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val mDatabase = Firebase.database.reference

      mDatabase.child("blusas").addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                getList.clear()
                for (postSnapshot in snapshot.children) {
                    val fav: Favorites? = postSnapshot.getValue(Favorites::class.java)
                    val listToRemove = mutableListOf<Estoque>()
                    if (fav != null) {
                        fav.estoque.onEach {
                            if (it.qt == 0) {
                                listToRemove.add(it)
                            }
                        }

                        fav.estoque.removeAll(listToRemove.toSet())
                        if (fav.estoque.isNotEmpty()) {
                            getList.add(fav)
                        }
                    }
                }
                isReady = true
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        return START_STICKY
    }

    private fun schedule() {
        Handler(Looper.myLooper()!!).postDelayed({startService(Intent(this, FirebaseDatabase::class.java))},2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
        isReady = false
    }
}

