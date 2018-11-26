package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import io.reactivex.subjects.PublishSubject

class GalleryPhotosAdapter(
  private val context: Context,
  private val imageLoader: ImageLoader,
  private val adapterButtonClickSubject: PublishSubject<GalleryPhotosAdapterButtonClickEvent>
) : BaseAdapter<GalleryPhotosAdapterItem>(context) {

  private val items = arrayListOf<GalleryPhotosAdapterItem>()
  private val duplicatesCheckerSet = hashSetOf<Long>()

  fun clearFooter(removeFooter: Boolean = true) {
    if (items.isEmpty()) {
      return
    }

    if (items.last().getType() != AdapterItemType.VIEW_MESSAGE && items.last().getType() != AdapterItemType.VIEW_PROGRESS) {
      return
    }

    items.removeAt(items.lastIndex)

    if (removeFooter) {
      notifyItemRemoved(items.size)
    }
  }

  fun showProgressFooter() {
    if (items.isNotEmpty() && items.last().getType() == AdapterItemType.VIEW_PROGRESS) {
      return
    }

    val hasFooter = items.lastOrNull()?.getType() == AdapterItemType.VIEW_MESSAGE
    clearFooter(false)
    items.add(GalleryPhotosAdapterItem.ProgressItem())

    if (hasFooter) {
      notifyItemChanged(items.size)
    } else {
      notifyItemInserted(items.size)
    }
  }

  fun showMessageFooter(message: String) {
    if (items.isNotEmpty() && items.last().getType() == AdapterItemType.VIEW_MESSAGE) {
      return
    }

    val hasFooter = items.lastOrNull()?.getType() == AdapterItemType.VIEW_PROGRESS
    clearFooter(false)
    items.add(GalleryPhotosAdapterItem.MessageItem(message))

    if (hasFooter) {
      notifyItemChanged(items.size)
    } else {
      notifyItemInserted(items.size)
    }
  }

  fun addAll(photos: List<GalleryPhoto>) {
    val filteredPhotos = photos
      .filter { photo -> duplicatesCheckerSet.add(photo.galleryPhotoId) }
      .map { galleryPhoto -> GalleryPhotosAdapterItem.GalleryPhotoItem(galleryPhoto, true) }

    val lastIndex = items.size

    items.addAll(filteredPhotos)
    notifyItemRangeInserted(lastIndex, filteredPhotos.size)
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

  fun switchShowMapOrPhoto(photoName: String) {
    val itemIndex = items.indexOfFirst {
      if (it !is GalleryPhotosAdapterItem.GalleryPhotoItem) {
        return@indexOfFirst false
      }

      return@indexOfFirst it.photo.photoName == photoName
    }

    if (itemIndex == -1) {
      return
    }

    val previous = (items[itemIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).showPhoto
    (items[itemIndex] as GalleryPhotosAdapterItem.GalleryPhotoItem).showPhoto = !previous
    notifyItemChanged(itemIndex)
  }

  override fun getItemViewType(position: Int): Int {
    return items[position].getType().type
  }

  override fun getItemCount(): Int = items.size

  override fun doGetBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
    return mutableListOf(
      BaseAdapterInfo(AdapterItemType.VIEW_GALLERY_PHOTO, R.layout.adapter_item_gallery_photo, GalleryPhotoViewHolder::class.java),
      BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java),
      BaseAdapterInfo(AdapterItemType.VIEW_MESSAGE, R.layout.adapter_item_message, MessageViewHolder::class.java)
    )
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder) {
      is GalleryPhotoViewHolder -> {
        val item = items[position] as GalleryPhotosAdapterItem.GalleryPhotoItem
        val showPhoto = (items[position] as? GalleryPhotosAdapterItem.GalleryPhotoItem)?.showPhoto
          ?: return

        holder.clickView.setOnClickListener {
          adapterButtonClickSubject.onNext(GalleryPhotosAdapterButtonClickEvent.SwitchShowMapOrPhoto(item.photo.photoName))
        }

        if (showPhoto) {
          holder.showPhotoHideMap()

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
        } else {
          holder.showMapHidePhoto()
          imageLoader.loadStaticMapImageFromNetInto(item.photo.photoName, holder.staticMapView)
        }

        holder.galleryPhotoId.text = item.photo.galleryPhotoId.toString()
        holder.favouritesCount.text = item.photo.favouritesCount.toString()

        imageLoader.loadPhotoFromNetInto(item.photo.photoName, holder.photoView)
      }
      is MessageViewHolder -> {
        val messageItem = (items[position] as? GalleryPhotosAdapterItem.MessageItem)
          ?: return

        holder.message.text = messageItem.message
      }
      is ProgressViewHolder -> {
        holder.progressBar?.isIndeterminate = true
      }
      else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
    }
  }

  sealed class GalleryPhotosAdapterButtonClickEvent {
    class FavouriteClicked(val photoName: String) : GalleryPhotosAdapterButtonClickEvent()
    class ReportClicked(val photoName: String) : GalleryPhotosAdapterButtonClickEvent()
    class SwitchShowMapOrPhoto(val photoName: String) : GalleryPhotosAdapterButtonClickEvent()
  }

  companion object {
    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val progressBar = itemView.findViewById<ProgressBar>(R.id.progressbar)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val message = itemView.findViewById<TextView>(R.id.message)
    }

    class GalleryPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
      val favouriteButton = itemView.findViewById<LinearLayout>(R.id.favourite_button)
      val reportButton = itemView.findViewById<LinearLayout>(R.id.report_button)
      val favouritesCount = itemView.findViewById<TextView>(R.id.favourites_count_text_view)
      val photoButtonsHolder = itemView.findViewById<LinearLayout>(R.id.photo_buttons_holder)
      val favouriteIcon = itemView.findViewById<ImageView>(R.id.favourite_icon)
      val reportIcon = itemView.findViewById<ImageView>(R.id.report_icon)
      val galleryPhotoId = itemView.findViewById<TextView>(R.id.gallery_photo_id)
      val staticMapView = itemView.findViewById<ImageView>(R.id.static_map_view)
      val clickView = itemView.findViewById<ConstraintLayout>(R.id.click_view)

      fun showPhotoHideMap() {
        staticMapView.visibility = View.GONE
        photoView.visibility = View.VISIBLE
      }

      fun showMapHidePhoto() {
        staticMapView.visibility = View.VISIBLE
        photoView.visibility = View.GONE
      }
    }
  }
}