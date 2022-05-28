package com.flavio.magnfica.ui.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.ViewPagerAdapter
import com.flavio.magnfica.infra.Constants
import com.flavio.magnfica.infra.Constants.IS_LOW_MEMORY_DEVICE
import com.flavio.magnfica.infra.Constants.SHOPPING
import com.flavio.magnfica.infra.FirebaseDatabase
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.fragments.AccountFragment
import com.flavio.magnfica.ui.fragments.CartFragment
import com.flavio.magnfica.ui.fragments.FavoritesFragment
import com.flavio.magnfica.ui.fragments.ShopFragment
import com.flavio.magnfica.ui.instant.InstantMainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.wrappers.InstantApps
import com.google.android.material.color.DynamicColors
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_shop.*


class MainActivity : AppCompatActivity() {
    private lateinit var mStorageRef: StorageReference
    private lateinit var mSharedPreferences: SecurityPreferences
    private lateinit var list: List<Fragment>
    private var a: Boolean? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mSharedPreferences = SecurityPreferences(this)
        mStorageRef = FirebaseStorage.getInstance().reference
        val id = GoogleSignIn.getLastSignedInAccount(this)?.id.toString()

        startService(Intent(this, FirebaseDatabase::class.java))
        DynamicColors.applyToActivityIfAvailable(this)

        createNotificationChannel()
        if (GoogleSignIn.getLastSignedInAccount(this) != null) {
            createShorcut()
        }

        Thread {
            Firebase.database.reference.child("usuarios/${id}/admin")
                .addListenerForSingleValueEvent(object :
                    ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            a = snapshot.getValue(Boolean::class.java)
                            if (a == true) {
                                startActivity(Intent(this@MainActivity, AdminActivity::class.java))
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }.run()

        list = listOf(ShopFragment(), FavoritesFragment(), CartFragment(), AccountFragment())

        main_frame_inst.adapter = ViewPagerAdapter(this, list)
        main_frame_inst.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                setBar(position)
            }
        })

        when (mSharedPreferences.getInt(SHOPPING)) {
            2 -> {
                setBar(2)
                mSharedPreferences.storeInt(SHOPPING, 0)
            }
            1 -> {
                startActivity(Intent(this, ViewActivity::class.java))
            }
            0 -> {
                setBar(0)
            }
        }

        when (intent.getStringExtra("dest")) {
            "fav" -> {
                setBar(1)
            }
            "cart" -> {
                setBar(2)
            }
            "user" -> {
                setBar(3)
            }
        }


        bottom_bar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    setBar(0)
                    true
                }
                R.id.page_2 -> {
                    setBar(1)
                    true
                }
                R.id.page_3 -> {
                    setBar(2)
                    true
                }
                R.id.page_4 -> {
                    setBar(3)
                    true
                }
                else -> {
                    false
                }
            }

        }

        bottom_bar.setOnItemReselectedListener {
            when (it.itemId) {
                R.id.page_1 -> {
                    if (mSharedPreferences.getInt(IS_LOW_MEMORY_DEVICE) == 1) {
                        recyclerView.scrollToPosition(0)
                    } else {
                        recyclerView.smoothScrollToPosition(0)
                    }
                }
            }
        }

        realtimeBlurView.setOnClickListener {

        }

        help_button.setOnClickListener {
            val balloon = Balloon.Builder(this)
                .setWidthRatio(1.0f)
                .setHeight(BalloonSizeSpec.WRAP)
                .setText(getString(R.string.help))
                .setTextColorResource(R.color.black)
                .setTextSize(17f)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .setPadding(12)
                .setMarginHorizontal(12)
                .setCornerRadius(8f)
                .setBackgroundColorResource(R.color.color_3)
                .setBalloonAnimation(BalloonAnimation.FADE)
                .build()

            balloon.showAlignTop(help_button)
        }

        icon_back.setOnClickListener {
            setBar(0)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appLinkIntent = intent
        val appLinkAction = appLinkIntent.action
        val appLinkData = appLinkIntent.data
    }

    private fun setBar(bt: Int) {

        val anim = AlphaAnimation(0.0f, 1.0f).apply {
            duration = 500
            repeatMode = Animation.ABSOLUTE
        }
        if (mSharedPreferences.getInt(IS_LOW_MEMORY_DEVICE) == 0) {
            title_main.animation = anim
        } else {
            realtimeBlurView.visibility = View.GONE
            bottom_bar.setBackgroundColor(window.navigationBarColor)
        }

        main_frame_inst.currentItem = bt

        when (bt) {
            0 -> {
                bottom_bar.menu.findItem(R.id.page_1).isChecked = true
                title_main.apply {
                    text = getString(R.string.app_title)
                    visibility = View.VISIBLE
                    help_button.visibility = View.GONE
                }
            }
            1 -> {
                bottom_bar.menu.findItem(R.id.page_2).isChecked = true
                title_main.apply {
                    text = ""
                    visibility = View.GONE
                    help_button.visibility = View.VISIBLE
                }
            }
            2 -> {
                bottom_bar.menu.findItem(R.id.page_3).isChecked = true
                title_main.apply {
                    text = ""
                    visibility = View.GONE
                    help_button.visibility = View.VISIBLE
                }
            }
            3 -> {
                bottom_bar.menu.findItem(R.id.page_4).isChecked = true
                val gName = GoogleSignIn.getLastSignedInAccount(this)?.displayName
                val textNameAccount = findViewById<TextView>(R.id.title_main)
                textNameAccount.visibility = View.VISIBLE
                help_button.visibility = View.GONE
                if (gName == null) {
                    textNameAccount.apply {
                        animation = anim
                        text = "Faça login primeiro"
                    }
                } else {
                    setTitle()
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    private fun setTitle() {
        val textNameAccount = findViewById<TextView>(R.id.title_main)
        Firebase.database.reference.child("usuarios")
            .child(GoogleSignIn.getLastSignedInAccount(this)?.id.toString())
            .child("nome").get().addOnSuccessListener {
                if (it.exists()) {
                    textNameAccount?.apply {
                        text = it.value.toString()
                    }
                } else {
                    textNameAccount?.apply {
                        text = GoogleSignIn.getLastSignedInAccount(this.context)?.displayName
                    }
                }
            }
    }

    private fun createShorcut() {

        if (InstantApps.isInstantApp(this)) {
            startActivity(Intent(this, InstantMainActivity::class.java))
            finishAffinity()
        } else {

            val sM = getSystemService(ShortcutManager::class.java)
            val intent1 = Intent(applicationContext, MainActivity::class.java)
            intent1.action = Intent.ACTION_VIEW
            intent1.putExtra("dest", "user")
            val shortcut1 = ShortcutInfo.Builder(this, "user")
                .setIntent(intent1)
                .setShortLabel(getString(R.string.shortcut_user))
                .setLongLabel("Ver Perfil")
                .setShortLabel("Perfil")
                .setDisabledMessage("Você precisa fazer o login primeiro")
                .setIcon(Icon.createWithResource(this, R.drawable.user_full))
                .build()

            val intent2 = Intent(applicationContext, PedidosActivity::class.java)
            intent2.action = Intent.ACTION_VIEW
            val shortcut2 = ShortcutInfo.Builder(this, "pedidos")
                .setIntent(intent2)
                .setShortLabel(getString(R.string.shortcut_ped))
                .setLongLabel("Ver Pedidos")
                .setShortLabel("Pedidos")
                .setDisabledMessage("Você precisa fazer o login primeiro")
                .setIcon(Icon.createWithResource(this, R.drawable.ic_check_circle))
                .build()

            sM.dynamicShortcuts = listOf(shortcut1, shortcut2)
        }
    }

    private fun createNotificationChannel() {
        val name = "Notificações"
        val descriptionText = "Notificações gerais do app"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(Constants.CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}