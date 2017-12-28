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
import com.kirakishou.photoexchange.helper.ImageLoader
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
        private val imageLoader: ImageLoader,
        private val cancelButtonSubject: PublishSubject<TakenPhoto>,
        private val retryButtonSubject: PublishSubject<TakenPhoto>
) : BaseAdapter<TakenPhoto>(context) {

    private val messages = arrayOf(
            "No photos to upload",
            "All photos has been uploaded",
            "Could not upload one or more photos"
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

        val index = items
                .indexOfFirst {
                    (it.getType() == AdapterItemType.VIEW_QUEUED_UP_PHOTO.ordinal ||
                    it.getType() == AdapterItemType.VIEW_FAILED_TO_UPLOAD.ordinal) &&
                    it.value.get().id == id
                }

        if (index == -1) {
            return
        }

        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun containsFailedToUploadPhotos(): Boolean {
        return items.any { it.getType() == AdapterItemType.VIEW_FAILED_TO_UPLOAD.ordinal }
    }

    fun removeQueuedUpPhotos(ids: List<Long>) {
        for (id in ids) {
            removeQueuedUpPhoto(id)
        }
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
                BaseAdapterInfo(AdapterItemType.VIEW_QUEUED_UP_PHOTO, R.layout.adapter_item_photo_queued_up, QueuedUpPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_FAILED_TO_UPLOAD, R.layout.adapter_item_upload_photo_error, PhotoUploadErrorViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_MESSAGE, R.layout.adapter_item_message, MessageViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is QueuedUpPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()

                    holder.progressBar.isIndeterminate = true
                    holder.cancelUploadingButton.isEnabled = buttonsEnabled

                    imageLoader.loadImageFromDiskInto(File(item.photoFilePath), holder.photoView)

                    if (buttonsEnabled) {
                        holder.cancelUploadingButton.setOnClickListener {
                            cancelButtonSubject.onNext(item)
                        }
                    } else {
                        holder.cancelUploadingButton.setOnClickListener(null)
                    }
                }
            }

            is PhotoUploadErrorViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()

                    holder.retryButton.setOnClickListener {
                        retryButtonSubject.onNext(item)
                    }

                    holder.cancelButton.setOnClickListener {
                        cancelButtonSubject.onNext(item)
                    }

                    imageLoader.loadImageFromDiskInto(File(item.photoFilePath), holder.photoView)
                }
            }

            is MessageViewHolder -> {
                check(messageType != -1)
                holder.messageTv.text = messages[messageType]
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

    class PhotoUploadErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.cancel_button)
        lateinit var cancelButton: AppCompatButton

        @BindView(R.id.retry_button)
        lateinit var retryButton: AppCompatButton

        @BindView(R.id.photo)
        lateinit var photoView: ImageView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    companion object {
        val MESSAGE_TYPE_NO_PHOTOS_TO_UPLOAD = 0
        val MESSAGE_TYPE_ALL_PHOTOS_UPLOADED = 1
        val MESSAGE_TYPE_COULD_NOT_UPLOAD_PHOTOS = 2
    }
}