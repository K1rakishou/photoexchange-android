package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.subjects.PublishSubject

class ReceivedPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val clicksSubject: PublishSubject<ReceivedPhotosAdapterClickEvent>
) : BaseAdapter<ReceivedPhotosAdapterItem>(context) {

    val items = arrayListOf<ReceivedPhotosAdapterItem>()
    val duplicatesChecked = hashSetOf<Long>()

    fun addReceivedPhotos(receivedPhotos: List<ReceivedPhoto>) {
        for (receivedPhoto in receivedPhotos) {
            addReceivedPhoto(receivedPhoto)
        }
    }

    fun addReceivedPhoto(receivedPhoto: ReceivedPhoto) {
       if (!duplicatesChecked.add(receivedPhoto.photoId)) {
           return
       }

        items.add(0, ReceivedPhotosAdapterItem.ReceivedPhotoItem(receivedPhoto, true))
        notifyItemInserted(0)
    }

    fun switchShowMapOrPhoto(photoName: String) {
        val itemIndex = items.indexOfFirst {
            if (it !is ReceivedPhotosAdapterItem.ReceivedPhotoItem) {
                return@indexOfFirst false
            }

            return@indexOfFirst it.receivedPhoto.receivedPhotoName == photoName
        }

        if (itemIndex == -1) {
            return
        }

        val previous = (items[itemIndex] as ReceivedPhotosAdapterItem.ReceivedPhotoItem).showPhoto
        (items[itemIndex] as ReceivedPhotosAdapterItem.ReceivedPhotoItem).showPhoto = !previous
        notifyItemChanged(itemIndex)
    }

    fun showProgressFooter() {
        if (items.isNotEmpty() && items.last().getType() == AdapterItemType.VIEW_PROGRESS) {
            return
        }

        val lastIndex = items.lastIndex

        items.add(ReceivedPhotosAdapterItem.ProgressItem())
        notifyItemInserted(lastIndex)
    }

    fun hideProgressFooter() {
        if (items.isEmpty() || items.last().getType() != AdapterItemType.VIEW_PROGRESS) {
            return
        }

        val lastIndex = items.lastIndex

        items.removeAt(lastIndex)
        notifyItemRemoved(lastIndex)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].getType().type
    }

    override fun getItemCount(): Int = items.size

    override fun doGetBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return arrayListOf(
            BaseAdapterInfo(AdapterItemType.VIEW_RECEIVED_PHOTO, R.layout.adapter_item_photo_answer, PhotoAnswerViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PhotoAnswerViewHolder -> {
                val receivedPhoto = (items[position] as? ReceivedPhotosAdapterItem.ReceivedPhotoItem)?.receivedPhoto
                    ?: return
                val showPhoto = (items[position] as? ReceivedPhotosAdapterItem.ReceivedPhotoItem)?.showPhoto
                    ?: return

                holder.clickView.setOnClickListener {
                    clicksSubject.onNext(ReceivedPhotosAdapterClickEvent.SwitchShowMapOrPhoto(receivedPhoto.receivedPhotoName))
                }

                holder.clickView.setOnLongClickListener {
                    clicksSubject.onNext(ReceivedPhotosAdapterClickEvent.ShowFullPhoto(receivedPhoto.receivedPhotoName))
                    return@setOnLongClickListener true
                }

                holder.photoIdTextView.text = receivedPhoto.photoId.toString()

                if (showPhoto) {
                    holder.showPhotoHideMap()
                    imageLoader.loadPhotoFromNetInto(receivedPhoto.receivedPhotoName, ImageLoader.PhotoSize.Small, holder.photoView)
                } else {
                    holder.showMapHidePhoto()
                    imageLoader.loadStaticMapImageFromNetInto(receivedPhoto.receivedPhotoName, holder.staticMapView)
                }
            }
            is ProgressViewHolder -> {
                //do nothing
            }
            else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
        }
    }

    sealed class ReceivedPhotosAdapterClickEvent {
        class ShowFullPhoto(val photoName: String) : ReceivedPhotosAdapterClickEvent()
        class SwitchShowMapOrPhoto(val photoName: String) : ReceivedPhotosAdapterClickEvent()
    }

    companion object {
        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        class PhotoAnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
            val staticMapView = itemView.findViewById<ImageView>(R.id.static_map_view)
            val photoIdTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
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