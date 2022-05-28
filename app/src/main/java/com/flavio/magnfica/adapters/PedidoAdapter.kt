package com.flavio.magnfica.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.Shimmer
import com.flavio.magnfica.R
import com.flavio.magnfica.database.Estoque
import com.flavio.magnfica.database.Favorites
import com.flavio.magnfica.database.Pedido
import com.flavio.magnfica.ui.activities.PedidosActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_cart.view.size_cart
import kotlinx.android.synthetic.main.item_cart.view.title_cart
import kotlinx.android.synthetic.main.item_pedido.view.*
import org.imaginativeworld.whynotimagecarousel.ImageCarousel
import org.imaginativeworld.whynotimagecarousel.model.CarouselItem
import java.text.NumberFormat
import java.util.*

class PedidoAdapter(private val onItemClicked: (Pedido) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Pedido> = ArrayList()
    private lateinit var mDatabse: FirebaseDatabase

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return LiveViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_pedido, parent, false)
        )
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LiveViewHolder -> {
                holder.bind(items[position], onItemClicked)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setDataSet(lives: MutableList<Pedido>) {
        this.items = lives
        mDatabse = Firebase.database
//        for(live in lives) {
//            mDatabse.reference.child("blusas").child(live.key).setValue(live)
//        }
    }


    class LiveViewHolder constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        //        private val liveTitle = itemView.title_main
        private val liveImage = itemView.image_ped
        private val liveTitle = itemView.title_cart
        private val liveItems = itemView.items_ped
        private val entregue = itemView.size_cart
        private val preco = itemView.preco_ped
        private val liveButton = itemView.button_cancel_ped
        private val a = mutableListOf<CarouselItem>()


        fun bind(item: Pedido, onItemClicked: (Pedido) -> Unit) {
            // This is the placeholder for the imageView

//            if (item.ref.isNotBlank()) {
//                liveTitle.text = item.title
//                loadImage(item, liveImage)
//            }
            val buttonCancel = itemView.findViewById<Button>(R.id.button_cancel_ped)

            buttonCancel.setOnClickListener {
                MaterialAlertDialogBuilder(itemView.context).setTitle("Você tem certeza disso?")
                    .setMessage("Se você cancelar o seu pedido ele ficará disponível na loja e poderá ser comprado por outras pessoas.")
                    .setPositiveButton("Sim") { _, _ -> process(item) }
                    .setNegativeButton("Não") { dialog, _ -> dialog.cancel() }
                    .show()
            }

            val itemsText: MutableList<String> = mutableListOf()
            item.items.onEach {
                itemsText.add(it.title)
            }

            preco.text = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(item.total)
            liveItems.text = "Itens: $itemsText."
            loadImage(item, liveImage)
            liveTitle.text = "Pedido ${item.id}"
            entregue.text = if (item.entregue) {
                liveButton.visibility = View.GONE
                "Pedido recebido"
            } else {
                liveButton.visibility = View.VISIBLE
                "Ainda não recebido"
            }

            itemView.setOnClickListener {
                onItemClicked(item)
            }
        }

        private fun loadImage(p: Pedido, liveImage: ImageCarousel) {
            val carousel = liveImage
            itemView.findViewTreeLifecycleOwner()?.let { carousel.registerLifecycle(it) }

            p.items.onEach { it ->
                FirebaseStorage.getInstance().reference.child(it.ref).downloadUrl.addOnSuccessListener {
                    a.add(CarouselItem(it.toString()))
                    carousel.setData(a)
                }
            }
        }

        private fun process(items: Pedido) {
            val database = Firebase.database.reference
            val l = mutableListOf<Favorites>()
            database.child("blusas").get().addOnSuccessListener { it ->
                for (snap in it.children) {
                    val i = snap.getValue(Favorites::class.java)
                    if (i != null) {
                        l.add(i)
                    }
                }
                items.items.onEach {
                    val fav = l.find { l ->
                        l.title == it.title
                    }
                    val estoque: MutableList<Estoque>? = fav?.estoque

                    val estOp = estoque?.find { e ->
                        e.tam == it.sel.tam
                    }

                    estoque?.remove(estOp)
                    estOp?.qt = estOp!!.qt.plus(it.sel.qt)
                    estoque.add(estOp)

                    fav.estoque = estoque
                    l.remove(fav)
                    l.add(fav)
                }
                database.child("blusas").setValue(l)
                database.child("pedidos")
                    .child(GoogleSignIn.getLastSignedInAccount(itemView.context)?.id.toString())
                    .child("pedido ${items.id}").removeValue().addOnSuccessListener {
                        Toast.makeText(itemView.context, "Pedido cancelado", Toast.LENGTH_SHORT).show()
                        (itemView.context as Activity).apply {
                            finish()
                            startActivity(Intent(this, PedidosActivity::class.java))
                        }

                    }
            }
        }
    }
}