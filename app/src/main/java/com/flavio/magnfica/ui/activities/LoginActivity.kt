package com.flavio.magnfica.ui.activities

import android.app.ActivityManager
import android.content.Intent
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.flavio.magnfica.R
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.infra.Constants
import com.flavio.magnfica.infra.Constants.LOGIN
import com.flavio.magnfica.infra.Constants.SHOPPING
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.instant.InstantMainActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.wrappers.InstantApps
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_login.*
import java.lang.reflect.Type

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var mStorageRef: StorageReference
    private lateinit var mDatabase: DatabaseReference
    private lateinit var mSharedPreferences: SecurityPreferences
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (InstantApps.isInstantApp(this)) {
            startActivity(Intent(this, InstantMainActivity::class.java))
            finishAffinity()
        } else {


            val act = getSystemService(ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            act.getMemoryInfo(memoryInfo)
            val totalMem = memoryInfo.totalMem / 1024 / 1024

            window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
            MobileAds.initialize(this) {}

            Firebase.messaging.subscribeToTopic("notifications")

            //Auth
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("782307490556-t7dl824uqm3dto9ptlbmu4rae8auvhgm.apps.googleusercontent.com")
                .requestEmail()
                .build()
            FirebaseApp.initializeApp(this)
            auth = Firebase.auth
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            mStorageRef = FirebaseStorage.getInstance().reference
            mSharedPreferences = SecurityPreferences(this)
            Firebase.database.apply {
                mDatabase = this.reference
            }

            if (mSharedPreferences.getInt(LOGIN) == 1) {
                signIn()
                mSharedPreferences.storeInt(LOGIN, 0)
            }

            if (totalMem < 2100 || memoryInfo.lowMemory) {
                mSharedPreferences.storeInt(Constants.IS_LOW_MEMORY_DEVICE, 1)
            } else {
                val list = listOf(R.drawable._1, R.drawable._2, R.drawable._3, R.drawable._4)
                background_imageView.setImageResource(list.random())
                background()
            }

            buttonLogin.setOnClickListener {
                signIn()
            }
            buttonNavegar.setOnClickListener {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }


    private fun background() {
        val list = listOf(R.drawable._1, R.drawable._2, R.drawable._3, R.drawable._4)

        Handler().postDelayed({
            val antigo = background_imageView.drawable
            val tr = TransitionDrawable(
                arrayOf<Drawable>(
                    antigo,
                    ResourcesCompat.getDrawable(resources, list.random(), theme)!!
                )
            )
            background_imageView.setImageDrawable(tr)
            tr.startTransition(1300)
            background()
        }, 2000)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)

        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    private fun updateUI(user: FirebaseUser?) {
        if (InstantApps.isInstantApp(this)) {
            startActivity(Intent(this, InstantMainActivity::class.java))
            finishAffinity()
        } else {
            when (mSharedPreferences.getInt(SHOPPING)) {
                1 -> {
                    startActivity(Intent(this, ViewActivity::class.java))
                }
                else -> {
                    if (user != null) {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                }
            }
        }
    }

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

    companion object {
        val type: Type = object :
            TypeToken<MutableList<Favorites>>() {}.type
        private const val RC_SIGN_IN = 100
        private const val TAG = "EmailPassword"
    }
}
