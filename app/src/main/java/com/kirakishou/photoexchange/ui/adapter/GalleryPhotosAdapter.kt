package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto

class GalleryPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader
) : BaseAdapter<GalleryPhotosAdapterItem>(context) {

    private val items = arrayListOf<GalleryPhotosAdapterItem>()

    fun addAll(photos: List<GalleryPhoto>) {
        items.addAll(photos.map { GalleryPhotosAdapterItem.GalleryPhotoItem(it) })
        notifyItemRangeInserted(items.size, photos.size)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].getType().type
    }

    override fun getItemCount(): Int = items.size

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.VIEW_GALLERY_PHOTO, R.layout.adapter_item_gallery_photo, GalleryPhotoViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GalleryPhotoViewHolder -> {
                val item = items[position] as GalleryPhotosAdapterItem.GalleryPhotoItem

                holder.photoIdTextView.text = item.photo.remoteId.toString()
                imageLoader.loadImageFromNetInto(item.photo.photoName, ImageLoader.PhotoSize.Small, holder.photoView)
            }
            else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
        }
    }

    class GalleryPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val galleryPhotoHolder = itemView.findViewById<LinearLayout>(R.id.gallery_photo_holder)
        val photoIdTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
    }
}