package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto

class GalleryPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val columnsCount: Int
) : BaseAdapter<GalleryPhotosAdapterItem>(context) {

    private val IMAGES_PER_COLUMN = 5
    private val items = arrayListOf<GalleryPhotosAdapterItem>()

    fun addProgressFooter() {
        if (items.isNotEmpty() && items.last().getType() == AdapterItemType.VIEW_PROGRESS) {
            return
        }

        val lastIndex = items.lastIndex

        items.add(GalleryPhotosAdapterItem.ProgressItem())
        notifyItemInserted(lastIndex)
    }

    fun removeProgressFooter() {
        if (items.isEmpty() || items.last().getType() != AdapterItemType.VIEW_PROGRESS) {
            return
        }

        val lastIndex = items.lastIndex

        items.removeAt(lastIndex)
        notifyItemRemoved(lastIndex)
    }

    fun addAll(photos: List<GalleryPhoto>) {
        val lastIndex = items.size

        items.addAll(photos.map { GalleryPhotosAdapterItem.GalleryPhotoItem(it) })
        notifyItemRangeInserted(lastIndex, photos.size)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].getType().type
    }

    override fun getItemCount(): Int = items.size

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.VIEW_GALLERY_PHOTO, R.layout.adapter_item_gallery_photo, GalleryPhotoViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GalleryPhotoViewHolder -> {
                val item = items[position] as GalleryPhotosAdapterItem.GalleryPhotoItem

                //TODO: optimize so it won't ddos the server when user scrolls recyclerview very fast
                imageLoader.loadImageFromNetInto(item.photo.photoName, ImageLoader.PhotoSize.Small, holder.photoView)
            }
            is ProgressViewHolder -> {
                holder.progressBar.isIndeterminate = true
            }
            else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
        }
    }

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar = itemView.findViewById<ProgressBar>(R.id.progressbar)
    }

    class GalleryPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val favouriteButton = itemView.findViewById<LinearLayout>(R.id.favourite_button)
        val reportButton = itemView.findViewById<ImageView>(R.id.report_button)
        val favouritesCount = itemView.findViewById<TextView>(R.id.favourites_count_text_view)
    }
}