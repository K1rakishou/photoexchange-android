package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
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

        items.add(0, ReceivedPhotosAdapterItem.ReceivedPhotoItem(receivedPhoto))
        notifyItemInserted(0)
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

                holder.clickView.setOnClickListener {
                    clicksSubject.onNext(ReceivedPhotosAdapterClickEvent.ShowMap(LonLat(receivedPhoto.lon, receivedPhoto.lat)))
                }

                holder.clickView.setOnLongClickListener {
                    clicksSubject.onNext(ReceivedPhotosAdapterClickEvent.ShowPhoto(receivedPhoto.receivedPhotoName))
                    return@setOnLongClickListener true
                }

                imageLoader.loadImageFromNetInto(receivedPhoto.receivedPhotoName, ImageLoader.PhotoSize.Small, holder.photoView)
                holder.photoIdTextView.text = receivedPhoto.photoId.toString()
            }
            is ProgressViewHolder -> {
                //do nothing
            }
            else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
        }
    }

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar = itemView.findViewById<ProgressBar>(R.id.progressbar)
    }

    class PhotoAnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val photoIdTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
        val clickView = itemView.findViewById<ConstraintLayout>(R.id.click_view)
    }

    sealed class ReceivedPhotosAdapterClickEvent {
        class ShowPhoto(val photoName: String) : ReceivedPhotosAdapterClickEvent()
        class ShowMap(val location: LonLat) : ReceivedPhotosAdapterClickEvent()
    }
}