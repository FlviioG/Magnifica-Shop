package com.flavio.magnfica.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.flavio.magnfica.R
import com.flavio.magnfica.database.AppDatabase
import com.flavio.magnfica.database.Cart
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_cart.view.*

class CartAdapter(private val onItemClicked: (Cart, ImageView) -> Unit)     :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Cart> = ArrayList()
    private lateinit var mDatabase: AppDatabase
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LiveViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        )
        context = view.itemView.context
        mDatabase = AppDatabase.getDatabase(context)

        return view
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

    fun setDataSet(lives: List<Cart>) {
        this.items = lives
        notifyDataSetChanged()
    }

    class LiveViewHolder constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        //        private val liveTitle = itemView.title_main
        private val liveImage = itemView.image_cart
        private val liveTitle = itemView.title_cart
        private val livePreco = itemView.preco_cart
        private val liveQt = itemView.qt_cart
        private val liveSize = itemView.size_cart

        private val shimmer =
            Shimmer.AlphaHighlightBuilder()// The attributes for a ShimmerDrawable is set by this builder
                .setDuration(1000) // how long the shimmering animation takes to do one full sweep
                .setBaseAlpha(0.7f) //the alpha of the underlying children
                .setHighlightAlpha(0.6f) // the shimmer alpha amount
                .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
                .setAutoStart(true)
                .setHighlightAlpha(1.0f)
                .setBaseAlpha(0.9f)

                .build()

        fun bind(item: Cart, onItemClicked: (Cart, ImageView) -> Unit) {
            // This is the placeholder for the imageView
            loadImage(item, liveImage)

            liveTitle.text = item.title
            liveQt.text = "Quantidade: " + item.sel.qt.toString()
            liveSize.text = "Tamanho: " + item.sel.tam
            livePreco.text = itemView.resources.getString(R.string.preco)

            itemView.setOnClickListener {
                onItemClicked(item, liveImage)
            }
        }

        private fun loadImage(photo: Cart, liveImage: ImageView) {
            val shimmerDrawable = ShimmerDrawable().apply {
                setShimmer(shimmer)
            }

            val requestOptions = RequestOptions()
                .placeholder(shimmerDrawable)
                .error(R.drawable.ic_launcher_background)
                .diskCacheStrategy(DiskCacheStrategy.ALL)

            Glide.with(itemView.context)
                .setDefaultRequestOptions(requestOptions)
                .load(FirebaseStorage.getInstance().reference.child(photo.ref))
                .into(liveImage)
        }
    }
}