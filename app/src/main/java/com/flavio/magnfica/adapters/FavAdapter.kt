package com.flavio.magnfica.adapters

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
import com.flavio.magnfica.database.Favorites
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.item_fav.view.*

class FavAdapter(private val onItemClicked: (Favorites, ImageView) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: List<Favorites> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return LiveViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_fav, parent, false)
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

    fun setDataSet(lives: List<Favorites>) {
        this.items = lives
        notifyDataSetChanged()
    }

    class LiveViewHolder constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {
        //        private val liveTitle = itemView.title_main
        private val liveImage = itemView.image_fav
        private val liveTitle = itemView.title_fav
        private val livePreco = itemView.preco_fav

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

        fun bind(photo: Favorites, onItemClicked: (Favorites, ImageView) -> Unit) {
            // This is the placeholder for the imageView

            loadImage(photo, liveImage)

            liveTitle.text = photo.title
            livePreco.text = itemView.resources.getString(R.string.preco)

            itemView.setOnClickListener {
                onItemClicked(photo, liveImage)
            }
        }

        private fun loadImage(photo: Favorites, liveImage: ImageView) {
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