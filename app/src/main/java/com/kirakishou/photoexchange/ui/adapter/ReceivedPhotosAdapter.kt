package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.PhotoAnswer

class ReceivedPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader
) : BaseAdapter<ReceivedPhotosAdapterItem>(context) {

    val items = arrayListOf<ReceivedPhotosAdapterItem>()

    fun addPhotoAnswers(photoAnswers: List<PhotoAnswer>) {
        for (photoAnswer in photoAnswers) {
            addPhotoAnswer(photoAnswer)
        }
    }

    fun addPhotoAnswer(photoAnswer: PhotoAnswer) {
        items.add(0, ReceivedPhotosAdapterItem.ReceivedPhotoItem(photoAnswer))
        notifyItemInserted(0)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].getType().type
    }

    override fun getItemCount(): Int = items.size

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return arrayListOf(
            BaseAdapterInfo(AdapterItemType.VIEW_RECEIVED_PHOTO, R.layout.adapter_item_photo_answer, PhotoAnswerViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is PhotoAnswerViewHolder -> {
                val photoAnswer = (items[position] as? ReceivedPhotosAdapterItem.ReceivedPhotoItem)?.photoAnswer
                    ?: return

                imageLoader.loadImageFromNetInto(photoAnswer.photoAnswerName, ImageLoader.PhotoSize.Small, holder.photoView)
                holder.photoIdTextView.text = photoAnswer.id?.toString() ?: "null"
            }
            else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
        }
    }

    class PhotoAnswerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val photoIdTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
    }
}