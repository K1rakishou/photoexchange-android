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
import com.kirakishou.photoexchange.mwvm.model.other.UploadedPhoto
import io.reactivex.subjects.PublishSubject

/**
 * Created by kirakishou on 11/7/2017.
 */
class UploadedPhotosAdapter(
        private val context: Context,
        private val retryButtonSubject: PublishSubject<UploadedPhoto>,
        private val noPhotosUploadedMessage: String
) : BaseAdapter<UploadedPhoto>(context) {

    fun addFirst(item: AdapterItem<UploadedPhoto>) {
        items.add(0, item)
        notifyItemInserted(0)
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
                BaseAdapterInfo(AdapterItemType.VIEW_ITEM, R.layout.adapter_item_sent_photo, SentPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PROGRESSBAR, R.layout.adapter_item_progress, ProgressBarViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_FAILED_TO_UPLOAD, R.layout.adapter_item_upload_photo_error, PhotoUploadErrorViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_MESSAGE, R.layout.adapter_item_message, MessageViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PHOTO_UPLOADING, R.layout.adapter_item_photo_uploading, PhotoUploadingViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()

                    val fullPath = "${PhotoExchangeApplication.baseUrl}v1/api/get_photo/${item.photoName}"

                    //TODO: do image loading via ImageLoader class
                    Glide.with(context)
                            .load(fullPath)
                            .apply(RequestOptions().centerCrop())
                            .into(holder.sentPhoto)
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
        }
    }

    class SentPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.sent_photo)
        lateinit var sentPhoto: ImageView

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
}


















