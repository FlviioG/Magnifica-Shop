package com.flavio.magnfica.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.facebook.shimmer.Shimmer
import com.facebook.shimmer.ShimmerDrawable
import com.flavio.magnfica.R
import com.flavio.magnfica.database.Cart
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_checkout.view.*

class CheckoutAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Cart> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return LiveViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_checkout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is LiveViewHolder -> {
                holder.bind(items[position])
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
        private val liveImage = itemView.image_chkout
        private val liveTitle = itemView.title_chkout
        private val liveSel = itemView.sel_chkout
        private val livePreco = itemView.preco_chkout

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

        fun bind(item: Cart) {
            // This is the placeholder for the imageView
            liveTitle.text = item.title
            livePreco.text =  itemView.resources.getString(R.string.preco)
            liveSel.text = "Tamanho: " + item.sel.tam + ", Quantidade: " + item.sel.qt

            val shimmerDrawable = ShimmerDrawable().apply {
                setShimmer(shimmer)
            }

            val requestOptions = RequestOptions()
                .placeholder(shimmerDrawable)
                .error(R.drawable.ic_launcher_background)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)

            Glide.with(itemView.context)
                .setDefaultRequestOptions(requestOptions)
                .load(FirebaseStorage.getInstance().reference.child(item.ref))
                .into(liveImage)
        }
    }
}