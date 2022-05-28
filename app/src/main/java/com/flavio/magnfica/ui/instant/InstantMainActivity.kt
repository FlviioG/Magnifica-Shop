package com.flavio.magnfica.ui.instant

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.widget.RecyclerView
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.ItemAdapter
import com.flavio.magnfica.database.Estoque
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.infra.SecurityPreferences
import com.google.android.gms.instantapps.InstantApps.showInstallPrompt
import com.google.android.material.color.DynamicColors
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class InstantMainActivity : AppCompatActivity() {
    private lateinit var mStorageRef: StorageReference
    private lateinit var mSharedPreferences: SecurityPreferences
    private lateinit var mDatabase: DatabaseReference
    private var list: MutableList<Favorites> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_instant)
        DynamicColors.applyToActivityIfAvailable(this)
        mSharedPreferences = SecurityPreferences(this)
        mStorageRef = FirebaseStorage.getInstance().reference
        FirebaseDatabase.getInstance().apply {
            mDatabase = this.reference
        }

        Thread {
            mDatabase.child("blusas").get().addOnSuccessListener { snapshot ->
                list.clear()
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
                            list.add(
                                Favorites(
                                    postSnapshot.key.toString(),
                                    fav.title,
                                    fav.desc,
                                    fav.estoque,
                                    fav.ref
                                )
                            )
                        }
                    }

                    if (list.isNotEmpty()) {
                        readData(list)
                    }
                }
            }
        }.run()

        findViewById<Button>(R.id.l1).setOnClickListener {
                val postInstall = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .setPackage("com.flavio.magnifica")

                // The request code is passed to startActivityForResult().
                showInstallPrompt(
                    this,
                    postInstall, 1, /* referrer= */ null
                )
        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    private fun readData(list: MutableList<Favorites>) {
        val recyclerView = findViewById<RecyclerView>(R.id.main_frame_inst)
        val itemAdapter = ItemAdapter { adapter, imageView -> onClick(adapter, imageView) }
        recyclerView.adapter = itemAdapter
        itemAdapter.setDataSet(list)
    }


    private fun onClick(adapter: Favorites, imageView: ImageView) {
        val intent = Intent(this, InstantViewActivity::class.java)
        intent.putExtra("extra", adapter)

        val opt: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            imageView,
            "photo"
        )
        startActivity(intent, opt.toBundle())

    }
}