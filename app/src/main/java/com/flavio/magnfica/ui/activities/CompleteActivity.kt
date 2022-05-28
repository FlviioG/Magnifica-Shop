package com.flavio.magnfica.ui.activities

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import com.flavio.magnfica.R
import com.flavio.magnfica.adapters.CartAdapter
import com.flavio.magnfica.database.Cart
import com.flavio.magnfica.infra.Constants.CHANNEL_ID
import com.flavio.magnfica.infra.Constants.ENTREGA
import com.flavio.magnfica.infra.Constants.PEDIDO
import com.flavio.magnfica.infra.SecurityPreferences
import com.flavio.magnfica.ui.instant.InstantMainActivity
import com.google.android.gms.common.wrappers.InstantApps
import kotlinx.android.synthetic.main.activity_complete.*
import java.net.URLEncoder

class CompleteActivity : AppCompatActivity() {
    private lateinit var mSharedPreferences: SecurityPreferences
    private var list: MutableList<Cart> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete)

        if(!InstantApps.isInstantApp(this)) {
            notifyShop()
        }

        mSharedPreferences = SecurityPreferences(this)
        val pedido = mSharedPreferences.getPedido(PEDIDO)

        if (InstantApps.isInstantApp(this)) {
            button_tela_inicial.text = "Instale o app para gerenciar os seus pedidos"
        }

        if (mSharedPreferences.getInt(ENTREGA) == 1) {
            retirar_pedido.text = "Entrar em contato"
        } else {
            retirar_pedido.text = "Como retirar o meu pedido?"
        }
        text_id.text = "ID do Pedido: " + pedido.id

        pedido.items.forEach {
            list.add(it)
        }

        val itemAdapter = CartAdapter { adapter, imageView -> onClick(adapter, imageView) }
        recycler_complete.adapter = itemAdapter
        itemAdapter.setDataSet(list)

        button_tela_inicial.setOnClickListener {
            if (InstantApps.isInstantApp(this)) {
                val postInstall = Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .setPackage("com.flavio.magnifica")

                // The request code is passed to startActivityForResult().
                com.google.android.gms.instantapps.InstantApps.showInstallPrompt(
                    this,
                    postInstall, 1, /* referrer= */ null
                )
            } else {
                this.finishAffinity()
                startActivity(Intent(this, SplashActivity::class.java))
            }
        }

        retirar_pedido.setOnClickListener {
            val url =
                "https://api.whatsapp.com/send?phone=+5577999539603&text=" + URLEncoder.encode(
                    "Oi, gostaria de saber sobre o meu pedido. ID: ${pedido.id}",
                    "UTF-8"
                )
            val i = Intent(Intent.ACTION_VIEW)

            i.data = Uri.parse(url)
            startActivity(i)
        }
    }

    private fun notifyShop() {
        val intent = Intent(this, PedidosActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_IMMUTABLE)
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground))
            .setContentTitle("Sua compra foi concluída com sucesso!")
            .setContentText("Toque para visualizá-la.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Você pode tocar aqui para conferir os detalhes ou até cancelar, se precisar.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLights(ResourcesCompat.getColor(resources, R.color.color_1, theme),3,3)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(1, builder.build())
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (InstantApps.isInstantApp(this)) {
            startActivity(Intent(this, InstantMainActivity::class.java))
        } else {
            startActivity(Intent(this, SplashActivity::class.java))
        }
    }

    private fun onClick(adapter: Cart, imageView: ImageView) {
        if(!InstantApps.isInstantApp(this)) {
            startActivity(Intent(this, PedidosActivity::class.java))
        }
    }
}