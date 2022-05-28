package com.flavio.magnfica.ui.instant

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
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
import com.flavio.magnfica.infra.Constants
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.fragments.MenuFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_view.*
import java.lang.reflect.Type

class InstantViewActivity : AppCompatActivity() {

    private var dataList: ArrayList<Favorites> = ArrayList()
    private lateinit var auth: FirebaseAuth
    private lateinit var actualFav: Favorites
    private lateinit var mDatabase: AppDatabase
    private lateinit var mFirebaseDatabase: DatabaseReference
    private lateinit var itemAdapter: ItemAdapter
    private lateinit var mStorageRef: StorageReference
    private lateinit var mSharedPreferences: SecurityPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private var memory: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        postponeEnterTransition()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_instant)

        mDatabase = AppDatabase.getDatabase(this)
        mStorageRef = FirebaseStorage.getInstance().reference
        mSharedPreferences = SecurityPreferences(this)
        mFirebaseDatabase = Firebase.database.reference
        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("782307490556-t7dl824uqm3dto9ptlbmu4rae8auvhgm.apps.googleusercontent.com")
            .requestEmail()
            .build()
        FirebaseApp.initializeApp(this)
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        actualFav = intent.extras?.getParcelable("extra")!!
        readData()

        findViewById<ImageButton>(R.id.imageBack).setOnClickListener {
            onBackPressed()
        }

        findViewById<Button>(R.id.buttonShop).setOnClickListener {
            updateUI(Firebase.auth.currentUser)
            mSharedPreferences.saveFav(Constants.ACTUAL_VIEW, actualFav)
        }
    }

    private fun readData() {

        val textName = findViewById<TextView>(R.id.textName)
        val textDesc = findViewById<TextView>(R.id.textDesc)

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
            .into(findViewById(R.id.imageMain))

        findViewById<ProgressBar>(R.id.progress_view).visibility = View.GONE

        mFirebaseDatabase.child("blusas").addValueEventListener(object : ValueEventListener {
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
                    if (this == null) {
                        buttons_l.visibility = View.GONE
                        Toast.makeText(
                            this@InstantViewActivity,
                            "Opa, parece que este item já foi comprado",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        buttons_l.visibility = View.VISIBLE
                    }
                }
                itemAdapter = ItemAdapter { adapter, imageView -> onClick(adapter, imageView) }
                recyclerViewInView.adapter = itemAdapter
                itemAdapter.setDataSet(dataList)
            }

            override fun onCancelled(error: DatabaseError) {
                Firebase.database.reference.removeEventListener(this)
            }
        })
    }


    private fun onClick(adapter: Favorites, imageView: ImageView) {
        val intent = Intent(this, InstantViewActivity::class.java)
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


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    updateUI(user)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    updateUI(null)
                }
            }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            view_frame.visibility = View.VISIBLE
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.view_frame, MenuFragment(), "MENU_FRAGMENT")
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .commit()

            mSharedPreferences.storeInt("buy", 1)
        } else {
            signIn()
        }
    }

    companion object {
        val type: Type = object :
            TypeToken<MutableList<Favorites>>() {}.type
        private const val RC_SIGN_IN = 100
        private const val TAG = "EmailPassword"
    }
}