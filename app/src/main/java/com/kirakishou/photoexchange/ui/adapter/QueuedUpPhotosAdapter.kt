package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseAdapter
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import io.reactivex.subjects.PublishSubject
import java.io.File

/**
 * Created by kirakishou on 11/26/2017.
 */
class QueuedUpPhotosAdapter(
        private val context: Context,
        private val cancelButtonSubject: PublishSubject<TakenPhoto>
) : BaseAdapter<TakenPhoto>(context) {

    private val messages = arrayOf(
            "No photos to upload",
            "All photos has been uploaded"
    )

    @Volatile
    private var messageType = -1

    private var buttonsEnabled = true

    fun setButtonsEnabled(enabled: Boolean) {
        buttonsEnabled = enabled
        notifyDataSetChanged()
    }

    fun addMessage(messageType: Int) {
        checkInited()

        if (items.isEmpty() || items.first().getType() != AdapterItemType.VIEW_MESSAGE.ordinal) {
            this.messageType = messageType

            items.add(0, AdapterItem(AdapterItemType.VIEW_MESSAGE))
            notifyItemInserted(0)
        }
    }

    fun removeMessage() {
        checkInited()

        if (items.isNotEmpty() && items.first().getType() == AdapterItemType.VIEW_MESSAGE.ordinal) {
            items.removeAt(0)
            notifyItemRemoved(0)
        }
    }

    fun addQueuedUpPhotos(queuedUpPhotosList: List<TakenPhoto>) {
        checkInited()

        val converted = queuedUpPhotosList
                .map { takenPhoto -> AdapterItem(takenPhoto, AdapterItemType.VIEW_QUEUED_UP_PHOTO) }

        items.addAll(converted)
        notifyItemRangeInserted(0, converted.size)
    }

    fun removeQueuedUpPhoto(id: Long) {
        checkInited()

        val index = items.asSequence()
                .filter { it.getType() == AdapterItemType.VIEW_QUEUED_UP_PHOTO.ordinal }
                .indexOfFirst { it.value.get().id == id }

        check(index != -1)

        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun removeQueuedUpPhotos(ids: List<Long>) {
        for (id in ids) {
            removeQueuedUpPhoto(id)
        }
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
                BaseAdapterInfo(AdapterItemType.VIEW_QUEUED_UP_PHOTO, R.layout.adapter_item_photo_queued_up, QueuedUpPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_MESSAGE, R.layout.adapter_item_message, MessageViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is QueuedUpPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()
                    holder.progressBar.isIndeterminate = true

                    Glide.with(context)
                            .load(File(item.photoFilePath))
                            .apply(RequestOptions().centerCrop())
                            .into(holder.photoView)

                    holder.cancelUploadingButton.isEnabled = buttonsEnabled

                    if (buttonsEnabled) {
                        holder.cancelUploadingButton.setOnClickListener {
                            cancelButtonSubject.onNext(item)
                        }
                    } else {
                        holder.cancelUploadingButton.setOnClickListener(null)
                    }
                }
            }
        }
    }

    class QueuedUpPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.image_view)
        lateinit var photoView: ImageView

        @BindView(R.id.loading_indicator)
        lateinit var progressBar: ProgressBar

        @BindView(R.id.cancel_uploading_button)
        lateinit var cancelUploadingButton: AppCompatButton

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.message)
        lateinit var messageTv: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    companion object {
        val MESSAGE_TYPE_NO_PHOTOS_TO_UPLOAD = 0
        val MESSAGE_TYPE_ALL_PHOTOS_UPLOADED = 1
    }
}