package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.CardView
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
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by kirakishou on 11/17/2017.
 */
class ReceivedPhotosAdapter(
        private val context: Context,
        private val imageLoader: ImageLoader,
        private val photoAnswerClickSubject: PublishSubject<PhotoAnswer>,
        private val photoAnswerLongClickSubject: PublishSubject<PhotoAnswer>
) : BaseAdapter<PhotoAnswer>(context) {

    private val selector = ReceivedPhotosIdSelectorFunction()
    private val duplicatesCheckerSet = mutableSetOf<Long>()

    private val messages = arrayOf(
            context.getString(R.string.error_while_looking_for_photo),
            context.getString(R.string.upload_more_photos)
    )

    @Volatile
    private var messageType = -1

    private fun isDuplicate(item: AdapterItem<PhotoAnswer>): Boolean {
        if (item.getType() != AdapterItemType.VIEW_ITEM.ordinal) {
            return false
        }

        val photoAnswer = item.value.get()
        val id = selector.select(photoAnswer)

        return !duplicatesCheckerSet.add(id)
    }

    fun addFirst(item: AdapterItem<PhotoAnswer>) {
        if (isDuplicate(item)) {
            return
        }

        if (items.isEmpty() || items.first().getType() == AdapterItemType.VIEW_ITEM.ordinal) {
            items.add(0, item)
            notifyItemInserted(0)
        } else if (items.size >= 1) {
            items.add(1, item)
            notifyItemInserted(1)
        }
    }

    override fun add(item: AdapterItem<PhotoAnswer>) {
        if (isDuplicate(item)) {
            return
        }

        super.add(item)
    }

    override fun addAll(items: List<AdapterItem<PhotoAnswer>>) {
        super.addAll(items.filter { isDuplicate(it) })
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

    fun addLookingForPhotoIndicator() {
        checkInited()

        if (items.isEmpty() || items.first().getType() != AdapterItemType.VIEW_LOOKING_FOR_PHOTO.ordinal) {
            items.add(0, AdapterItem(AdapterItemType.VIEW_LOOKING_FOR_PHOTO))
            notifyItemInserted(0)
        }
    }

    fun removeLookingForPhotoIndicator() {
        checkInited()

        if (items.isNotEmpty() && items.first().getType() == AdapterItemType.VIEW_LOOKING_FOR_PHOTO.ordinal) {
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

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
                BaseAdapterInfo(AdapterItemType.VIEW_ITEM, R.layout.adapter_item_received_photos, ReceivedPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PROGRESSBAR, R.layout.adapter_item_progress, ProgressBarViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_LOOKING_FOR_PHOTO, R.layout.adapter_item_looking_for_photo, LookingForPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_MESSAGE, R.layout.adapter_item_message, MessageViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ReceivedPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()

                    imageLoader.loadImageFromNetInto(item.photoName, ImageLoader.PhotoSize.Small, holder.photoView)

                    holder.clickView.setOnClickListener {
                        photoAnswerClickSubject.onNext(item)
                    }

                    holder.clickView.setOnLongClickListener {
                        photoAnswerLongClickSubject.onNext(item)
                        return@setOnLongClickListener true
                    }
                }
            }

            is ProgressBarViewHolder -> {
            }

            is LookingForPhotoViewHolder -> {
            }

            is MessageViewHolder -> {
                if (messageType != -1) {
                    val message = messages[messageType]
                    holder.messageTv.text = message
                }
            }
        }
    }

    class ReceivedPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.click_view)
        lateinit var clickView: CardView

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

    class LookingForPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.loading_indicator)
        lateinit var progressBar: ProgressBar

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

    inner class ReceivedPhotosIdSelectorFunction {
        fun select(item: PhotoAnswer): Long = item.photoRemoteId
    }

    companion object {
        val MESSAGE_TYPE_ERROR = 0
        val MESSAGE_TYPE_UPLOAD_MORE_PHOTOS = 1
    }
}