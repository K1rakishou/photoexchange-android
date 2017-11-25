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
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseAdapter
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import io.reactivex.subjects.PublishSubject
import java.io.File

/**
 * Created by kirakishou on 11/7/2017.
 */
class UploadedPhotosAdapter(
        private val context: Context,
        private val retryButtonSubject: PublishSubject<TakenPhoto>
) : BaseAdapter<TakenPhoto>(context) {

    private val selector = UploadedPhotosIdSelectorFunction()
    private val duplicatesCheckerSet = mutableSetOf<Long>()

    private val noPhotosUploadedMessage: String = context.getString(R.string.no_photos_uploaded)

    private fun isDuplicate(item: AdapterItem<TakenPhoto>): Boolean {
        if (item.getType() != AdapterItemType.VIEW_ITEM.ordinal) {
            return false
        }

        val photoAnswer = item.value.get()
        val id = selector.select(photoAnswer)

        return !duplicatesCheckerSet.add(id)
    }

    fun addQueuedUpPhotos(queuedUpPhotosList: List<TakenPhoto>) {
        val index = when {
            items.isEmpty() -> 0
            items.first().getType() == AdapterItemType.VIEW_ITEM.ordinal -> 0
            items.first().getType() != AdapterItemType.VIEW_ITEM.ordinal -> 1
            else -> 1
        }

        val converted = queuedUpPhotosList
                .map { takenPhoto -> AdapterItem(takenPhoto, AdapterItemType.VIEW_QUEUED_UP_PHOTO) }
                .filter { convertedPhoto -> !isDuplicate(convertedPhoto) }

        items.addAll(index, converted)
        notifyItemRangeInserted(index, converted.size)
    }

    fun addFirst(item: AdapterItem<TakenPhoto>) {
        if (isDuplicate(item)) {
            return
        }

        items.add(0, item)
        notifyItemInserted(0)
    }

    override fun add(item: AdapterItem<TakenPhoto>) {
        if (isDuplicate(item)) {
            return
        }

        super.add(item)
    }

    override fun addAll(items: List<AdapterItem<TakenPhoto>>) {
        super.addAll(items.filter { isDuplicate(it) })
    }

    fun addPhotoUploadingIndicator() {
        checkInited()

        if (items.isEmpty() || items.first().getType() != AdapterItemType.VIEW_PHOTO_UPLOADING.ordinal) {
            items.add(AdapterItem(AdapterItemType.VIEW_PHOTO_UPLOADING))
            notifyItemInserted(items.lastIndex)
        }
    }

    fun removePhotoUploadingIndicator() {
        checkInited()

        if (items.isNotEmpty() && items.first().getType() == AdapterItemType.VIEW_PHOTO_UPLOADING.ordinal) {
            items.removeAt(0)
            notifyItemRemoved(0)
        }
    }

    fun addProgressFooter() {
        checkInited()

        if (items.isEmpty() || items.last().getType() != AdapterItemType.VIEW_PROGRESSBAR.ordinal) {
            items.add(AdapterItem(AdapterItemType.VIEW_PROGRESSBAR))
            notifyItemInserted(items.lastIndex)
        }
    }

    fun removeProgressFooter() {
        checkInited()

        if (items.isNotEmpty() && items.last().getType() == AdapterItemType.VIEW_PROGRESSBAR.ordinal) {
            val index = items.lastIndex

            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addMessageFooter() {
        checkInited()

        if (items.isEmpty() || items.last().getType() != AdapterItemType.VIEW_MESSAGE.ordinal) {
            items.add(AdapterItem(AdapterItemType.VIEW_MESSAGE))
            notifyItemInserted(items.lastIndex)
        }
    }

    fun removeMessageFooter() {
        checkInited()

        if (items.isNotEmpty() && items.last().getType() == AdapterItemType.VIEW_MESSAGE.ordinal) {
            val index = items.lastIndex

            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
                BaseAdapterInfo(AdapterItemType.VIEW_ITEM, R.layout.adapter_item_sent_photos, SentPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PROGRESSBAR, R.layout.adapter_item_progress, ProgressBarViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_FAILED_TO_UPLOAD, R.layout.adapter_item_upload_photo_error, PhotoUploadErrorViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_MESSAGE, R.layout.adapter_item_message, MessageViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PHOTO_UPLOADING, R.layout.adapter_item_photo_uploading, PhotoUploadingViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_QUEUED_UP_PHOTO, R.layout.adapter_item_photo_queued_up, QueuedUpPhotoViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()
                    val fullPath = "${PhotoExchangeApplication.baseUrl}v1/api/get_photo/${item.photoName}/s"

                    //TODO: do image loading via ImageLoader class
                    Glide.with(context)
                            .load(fullPath)
                            .apply(RequestOptions().centerCrop())
                            .into(holder.photoView)
                }
            }

            is ProgressBarViewHolder -> {
                holder.progressBar.isIndeterminate = true
            }

            is PhotoUploadErrorViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()

                    retryButtonSubject.onNext(item)
                }
            }

            is MessageViewHolder -> {
                holder.messageTv.text = noPhotosUploadedMessage
            }

            is PhotoUploadingViewHolder -> {
                holder.progressBar.isIndeterminate = true
            }

            is QueuedUpPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()
                    holder.progressBar.isIndeterminate = true

                    Glide.with(context)
                            .load(File(item.photoFilePath))
                            .apply(RequestOptions().centerCrop())
                            .into(holder.photoView)
                }
            }
        }
    }

    class SentPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.photo)
        lateinit var photoView: ImageView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class ProgressBarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.progressbar)
        lateinit var progressBar: ProgressBar

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class PhotoUploadErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.retry_button)
        lateinit var retryButton: AppCompatButton

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

    class PhotoUploadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.loading_indicator)
        lateinit var progressBar: ProgressBar

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class QueuedUpPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.image_view)
        lateinit var photoView: ImageView

        @BindView(R.id.loading_indicator)
        lateinit var progressBar: ProgressBar

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    inner class UploadedPhotosIdSelectorFunction {
        fun select(item: TakenPhoto): Long = item.id
    }
}



















