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
import io.reactivex.subjects.PublishSubject

class GalleryPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val photoSize: ImageLoader.PhotoSize,
    private val adapterButtonClickSubject: PublishSubject<GalleryPhotosAdapterButtonClickEvent>
) : BaseAdapter<GalleryPhotosAdapterItem>(context) {

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

    fun favouritePhoto(photoName: String, isFavourited: Boolean, favouritesCount: Long): Boolean {
        val photoIndex = items.indexOfFirst { adapterItem ->
            if (adapterItem !is GalleryPhotosAdapterItem.GalleryPhotoItem) {
                return@indexOfFirst false
            }

            return@indexOfFirst adapterItem.photo.photoName == photoName
        }

        if (photoIndex == -1) {
            return false
        }

        if ((items[photoIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).photo.galleryPhotoInfo == null) {
            return false
        }

        if ((items[photoIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).photo.galleryPhotoInfo!!.isFavourited == isFavourited) {
            return false
        }

        (items[photoIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).photo.galleryPhotoInfo!!.isFavourited = isFavourited
        (items[photoIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).photo.favouritesCount = favouritesCount

        notifyItemChanged(photoIndex)
        return true
    }

    fun reportPhoto(photoName: String, isReported: Boolean): Boolean {
        val photoIndex = items.indexOfFirst { adapterItem ->
            if (adapterItem !is GalleryPhotosAdapterItem.GalleryPhotoItem) {
                return@indexOfFirst false
            }

            return@indexOfFirst adapterItem.photo.photoName == photoName
        }

        if (photoIndex == -1) {
            return false
        }

        if ((items[photoIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).photo.galleryPhotoInfo == null) {
            return false
        }

        if ((items[photoIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).photo.galleryPhotoInfo!!.isReported == isReported) {
            return false
        }

        (items[photoIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).photo.galleryPhotoInfo!!.isReported = isReported
        notifyItemChanged(photoIndex)
        return true
    }

    fun addAll(photos: List<GalleryPhoto>) {
        val lastIndex = items.size

        items.addAll(photos.map { galleryPhoto -> GalleryPhotosAdapterItem.GalleryPhotoItem(galleryPhoto) })
        notifyItemRangeInserted(lastIndex, photos.size)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].getType().type
    }

    override fun getItemCount(): Int = items.size

    override fun doGetBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.VIEW_GALLERY_PHOTO, R.layout.adapter_item_gallery_photo, GalleryPhotoViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GalleryPhotoViewHolder -> {
                val item = items[position] as GalleryPhotosAdapterItem.GalleryPhotoItem

                if (item.photo.galleryPhotoInfo != null) {
                    holder.photoButtonsHolder.visibility = View.VISIBLE
                    holder.favouriteButton.isClickable = true
                    holder.reportButton.isClickable = true

                    holder.favouriteButton.setOnClickListener {
                        adapterButtonClickSubject.onNext(GalleryPhotosAdapterButtonClickEvent.FavouriteClicked(item.photo.photoName))
                    }

                    holder.reportButton.setOnClickListener {
                        adapterButtonClickSubject.onNext(GalleryPhotosAdapterButtonClickEvent.ReportClicked(item.photo.photoName))
                    }

                    if (item.photo.galleryPhotoInfo!!.isFavourited) {
                        holder.favouriteIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_favorite))
                    } else {
                        holder.favouriteIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_favorite_border))
                    }

                    if (item.photo.galleryPhotoInfo!!.isReported) {
                        holder.reportIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_reported))
                    } else {
                        holder.reportIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_report_border))
                    }
                } else {
                    holder.photoButtonsHolder.visibility = View.GONE
                    holder.favouriteButton.isClickable = false
                    holder.reportButton.isClickable = false
                }

                holder.favouritesCount.text = item.photo.favouritesCount.toString()

                imageLoader.loadPhotoFromNetInto(item.photo.photoName, photoSize, holder.photoView)
            }
            is ProgressViewHolder -> {
                holder.progressBar.isIndeterminate = true
            }
            else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
        }
    }

    sealed class GalleryPhotosAdapterButtonClickEvent {
        class FavouriteClicked(val photoName: String) : GalleryPhotosAdapterButtonClickEvent()
        class ReportClicked(val photoName: String) : GalleryPhotosAdapterButtonClickEvent()
    }

    companion object {
        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val progressBar = itemView.findViewById<ProgressBar>(R.id.progressbar)
        }

        class GalleryPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
            val favouriteButton = itemView.findViewById<LinearLayout>(R.id.favourite_button)
            val reportButton = itemView.findViewById<LinearLayout>(R.id.report_button)
            val favouritesCount = itemView.findViewById<TextView>(R.id.favourites_count_text_view)
            val photoButtonsHolder = itemView.findViewById<LinearLayout>(R.id.photo_buttons_holder)
            val favouriteIcon = itemView.findViewById<ImageView>(R.id.favourite_icon)
            val reportIcon = itemView.findViewById<ImageView>(R.id.report_icon)
        }
    }
}