package com.flavio.magnfica.ui.activities

import android.content.Intent
import android.content.Intent.*
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.ItemAdapter
import com.flavio.magnfica.database.AppDatabase
import com.flavio.magnfica.database.Estoque
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.infra.Constants.ACTUAL_VIEW
import com.flavio.magnfica.infra.Constants.IS_LOW_MEMORY_DEVICE
import com.flavio.magnfica.infra.Constants.LOGIN
import com.flavio.magnfica.infra.Constants.SHOPPING
import com.flavio.magnfica.infra.FirebaseDatabase.Companion.getList
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.fragments.MenuFragment
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_view.*
import java.io.File


class ViewActivity : AppCompatActivity() {

    private lateinit var valueListener : ValueEventListener
    private lateinit var actualFav: Favorites
    private lateinit var mDatabase: AppDatabase
    private lateinit var mFirebaseDatabase: DatabaseReference
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var mStorageRef: StorageReference
    private lateinit var dataList: ArrayList<Favorites>
    private lateinit var mSharedPreferences: SecurityPreferences
    private var memory: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        supportPostponeEnterTransition()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        dataList = getList
        mDatabase = AppDatabase.getDatabase(this)
        mStorageRef = FirebaseStorage.getInstance().reference
        mSharedPreferences = SecurityPreferences(this)
        mFirebaseDatabase = Firebase.database.reference

        memory = mSharedPreferences.getInt(IS_LOW_MEMORY_DEVICE)
        if (memory == 1) {
            blur_v.visibility = View.GONE
            realtimeBlurView2.setBackgroundColor(window.navigationBarColor)
        }

        when (mSharedPreferences.getInt(SHOPPING)) {
            1 -> {
                actualFav = mSharedPreferences.getFav(ACTUAL_VIEW)
                mSharedPreferences.storeInt(SHOPPING, 0)
            }
            else -> {
                val extras = intent.extras
                actualFav = extras?.getParcelable("extra")!!
            }
        }

        buttonFav.setOnClickListener {
            val favs = mDatabase.favoritesDao().getAll()
            if (favs.contains(actualFav)) {
                mDatabase.favoritesDao().delete(actualFav)
                buttonFav.setImageResource(R.drawable.heart)
                Toast.makeText(this, "Favorito removido", Toast.LENGTH_SHORT).show()
            } else {
                mDatabase.favoritesDao().insertAll(actualFav)
                buttonFav.setImageResource(R.drawable.heart_full)
                Toast.makeText(this, "Favorito adicionado", Toast.LENGTH_SHORT).show()
            }
        }

        imageBack.setOnClickListener {
            onBackPressed()
        }

        buttonShop.setOnClickListener {

            mSharedPreferences.saveFav(
                ACTUAL_VIEW,
                actualFav
            )
            if (Firebase.auth.currentUser == null) {
                startActivity(Intent(this, LoginActivity::class.java))
                Toast.makeText(this, "Faça login primeiro", Toast.LENGTH_SHORT).show()
                mSharedPreferences.storeInt(LOGIN, 1)
                mSharedPreferences.storeInt(SHOPPING, 1)
            } else {
                view_frame.visibility = View.VISIBLE
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.view_frame, MenuFragment(), "MENU_FRAGMENT")
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .commit()

                mSharedPreferences.storeInt("buy", 1)
            }
        }

        buttonShare.setOnClickListener {
            mStorageRef.child(actualFav.ref)
                .getFile(File(externalCacheDir, actualFav.title + ".webp"))
                .addOnSuccessListener {

                    val t = Intent().apply {
                        action = ACTION_SEND
                        putExtra(
                            EXTRA_TEXT,
                            getString(R.string.share)
                        )
                        putExtra(
                            EXTRA_STREAM,
                            FileProvider.getUriForFile(
                                this@ViewActivity,
                                "com.codepath.fileprovider",
                                File(externalCacheDir, actualFav.title + ".webp")
                            )
                        )
                    }
                    t.type = "*/*"

                    val shareIntent = createChooser(t, null)
                    startActivity(shareIntent)
                }
        }

        buttonAddToCart.setOnClickListener {
            mSharedPreferences.saveFav(ACTUAL_VIEW, actualFav)
            if (buttonAddToCart.text == "Adicionado à sacola") {

                buttonAddToCart.text = "Adicionar à sacola"
                mDatabase.cartDao().getAll().filter {
                    actualFav.title == it.title
                }.apply {
                    this.onEach {
                        mDatabase.cartDao().delete(it)
                    }
                }
                Toast.makeText(this, "Removido da sacola", Toast.LENGTH_SHORT).show()
            } else {
                mSharedPreferences.saveFav(
                    ACTUAL_VIEW,
                    actualFav
                )
                view_frame.visibility = View.VISIBLE
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.view_frame, MenuFragment(), "MENU_FRAGMENT")
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                    .commit()
                mSharedPreferences.storeInt("buy", 0)
            }
        }

      valueListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dataList.clear()
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
                            dataList.add(fav)
                        }
                    }
                }
                dataList.find {
                    it.title == actualFav.title
                }.apply {
                    if(this == null) {
                        buttons_l.visibility = View.GONE
                        Toast.makeText(
                            this@ViewActivity,
                            "Opa, parece que este item já foi comprado",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        buttons_l.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Firebase.database.reference.removeEventListener(this)
            }
        }
        readData()
    }

    override fun onBackPressed() {
        Firebase.database.reference.child("blusas").removeEventListener(valueListener)
        finishAfterTransition()
    }

    private fun readData() {

        Firebase.database.reference.child("blusas")
            .addValueEventListener(valueListener)


        itemAdapter = ItemAdapter { adapter, imageView -> onClick(adapter, imageView) }
        recyclerViewInView.adapter = itemAdapter
        dataList.remove(actualFav)
        dataList.shuffle()
        itemAdapter.setDataSet(dataList)

        textName.text = actualFav.title
        textDesc.text = actualFav.desc
        Glide.with(this)
            .load(mStorageRef.child(actualFav.ref))
            .priority(Priority.IMMEDIATE)
            .dontTransform()
            .dontAnimate()
            .fitCenter()
            .listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    supportStartPostponedEnterTransition()
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    supportStartPostponedEnterTransition()
                    return false
                }
            })
            .into(imageMain)

        progress_view.visibility = View.GONE

        Thread {
            val fav = mDatabase.favoritesDao().getAll()
            if (fav.contains(actualFav)) {
                buttonFav.setImageResource(R.drawable.heart_full)
            }

            mDatabase.cartDao().getAll().onEach {
                if (it.title == actualFav.title) {
                    buttonAddToCart.text = "Adicionado à sacola"
                }
            }
        }.run()
    }

    private fun onClick(adapter: Favorites, imageView: ImageView) {
        val intent = Intent(this, ViewActivity::class.java)
        intent.putExtra("extra", adapter)

        val opt: ActivityOptionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            imageView,
            "photo"
        )

        if (memory == 0) {
            startActivity(intent, opt.toBundle())
        } else {
            startActivity(intent)
        }
    }
}