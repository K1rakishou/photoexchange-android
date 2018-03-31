package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.PhotoState

/**
 * Created by kirakishou on 3/18/2018.
 */
class MyPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader
) : BaseAdapter<MyPhotosAdapterItem>(context) {

    private val QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX = 0
    private val OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX = 1
    private val UPPER_PROGRESS_INDEX = 2
    private val MY_PHOTO_VIEW_INDEX = 3

    private val photosProgressMap = mutableMapOf<Long, Int>()

    init {
        items.add(QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX, MyPhotosAdapterItem.EmptyItem())
        items.add(OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX, MyPhotosAdapterItem.EmptyItem())
        items.add(UPPER_PROGRESS_INDEX, MyPhotosAdapterItem.EmptyItem())
    }

    fun updatePhotoState(photoId: Long, photoState: PhotoState) {
        checkInited()

        val photoIndex = items
            .indexOfFirst {
                if (it !is MyPhotosAdapterItem.MyPhotoItem) {
                    return@indexOfFirst false
                }

                return@indexOfFirst it.myPhoto.id == photoId
            }

        if (photoIndex == -1) {
            return
        }

        (items.getOrNull(photoIndex) as? MyPhotosAdapterItem.MyPhotoItem)?.myPhoto?.apply {
            this.photoState = photoState
            notifyItemChanged(photoIndex)
        }
    }

    fun updatePhotoProgress(photoId: Long, newProgress: Int) {
        checkInited()

        val photoIndex = items
            .indexOfFirst {
                if (it !is MyPhotosAdapterItem.MyPhotoItem) {
                    return@indexOfFirst false
                }

                return@indexOfFirst it.myPhoto.id == photoId
            }

        if (photoIndex == -1) {
            return
        }

        photosProgressMap[photoId] = newProgress
        notifyItemChanged(photoIndex)
    }

    fun showObtainCurrentLocationNotification() {
        if (items[OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX] is MyPhotosAdapterItem.ObtainCurrentLocationItem) {
            return
        }

        items[OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX] = MyPhotosAdapterItem.ObtainCurrentLocationItem()
        notifyItemChanged(OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX)
    }

    fun hideObtainCurrentLocationNotification() {
        if (items[OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX] is MyPhotosAdapterItem.EmptyItem) {
            return
        }

        items[OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX] = MyPhotosAdapterItem.EmptyItem()
        notifyItemChanged(OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX)
    }

    fun showQueuedUpPhotosCountNotification(queuedUpPhotosCount: Int) {
        if (queuedUpPhotosCount > 0) {
            items[QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX] = MyPhotosAdapterItem.QueuedUpPhotosItem(queuedUpPhotosCount)
            notifyItemChanged(QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX)
        } else {
            hideQueuedUpPhotosCountNotification()
        }
    }

    fun updateQueuedUpPhotosCountNotification(queuedUpPhotosCount: Int) {
        if (queuedUpPhotosCount > 0) {
            if (items[QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX] !is MyPhotosAdapterItem.QueuedUpPhotosItem) {
                showQueuedUpPhotosCountNotification(queuedUpPhotosCount)
            } else {
                if ((items[QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX] as MyPhotosAdapterItem.QueuedUpPhotosItem).count != queuedUpPhotosCount) {
                    (items[QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX] as MyPhotosAdapterItem.QueuedUpPhotosItem).count = queuedUpPhotosCount
                    notifyItemChanged(QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX)
                }
            }
        } else {
            hideQueuedUpPhotosCountNotification()
        }
    }

    fun hideQueuedUpPhotosCountNotification() {
        if (items[QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX] is MyPhotosAdapterItem.EmptyItem) {
            return
        }

        items[QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX] = MyPhotosAdapterItem.EmptyItem()
        notifyItemChanged(QUEUED_UP_ITEMS_COUNT_NOTIFICATION_INDEX)
    }

    override fun add(index: Int, item: MyPhotosAdapterItem) {
        val correctedIndex = MY_PHOTO_VIEW_INDEX + index
        super.add(correctedIndex, item)
    }

    override fun remove(index: Int) {
        val correctedIndex = MY_PHOTO_VIEW_INDEX + index

        super.remove(correctedIndex)
    }

    override fun clear() {
        hideObtainCurrentLocationNotification()
        hideQueuedUpPhotosCountNotification()

        items.removeAll { it.getType() == AdapterItemType.VIEW_MY_PHOTO }
        notifyDataSetChanged()
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.EMPTY, R.layout.adapter_item_empty, EmptyViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_MY_PHOTO, R.layout.adapter_item_my_photo, MyPhotoViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION, R.layout.adapter_item_obtain_current_location,
                ObtainCurrentLocationViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_QUEUED_UP_PHOTOS_NOTIFICATION,
                R.layout.adapter_item_queued_up_photos_count_notification, QueuedUpPhotosNotificationViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyPhotoViewHolder -> {
                val myPhoto = (items.getOrNull(position) as? MyPhotosAdapterItem.MyPhotoItem)?.myPhoto
                    ?: return

                when (myPhoto.photoState) {
                    PhotoState.PHOTO_QUEUED_UP,
                    PhotoState.PHOTO_UPLOADING -> {
                        holder.uploadingMessageHolderView.visibility = View.VISIBLE

                        myPhoto.photoTempFile?.let { photoFile ->
                            imageLoader.loadImageFromDiskInto(photoFile, holder.photoView)
                        }

                        if (photosProgressMap.containsKey(myPhoto.id)) {
                            holder.loadingProgress.progress = photosProgressMap[myPhoto.id]!!
                        }
                    }
                    PhotoState.PHOTO_UPLOADED -> {
                        holder.uploadingMessageHolderView.visibility = View.GONE

                        myPhoto.photoName?.let { photoName ->
                            imageLoader.loadImageFromNetInto(photoName, ImageLoader.PhotoSize.Small, holder.photoView)
                        }

                        photosProgressMap.remove(myPhoto.id)
                    }

                    PhotoState.PHOTO_TAKEN -> {
                        throw IllegalStateException("photo with state PHOTO_TAKEN should not be here!")
                    }
                }
            }

            is QueuedUpPhotosNotificationViewHolder -> {
                val count = (items.getOrNull(position) as? MyPhotosAdapterItem.QueuedUpPhotosItem)?.count ?: 0
                if (count > 0) {
                    holder.queuedUpPhotoCountTextView.text = String.format("Photos in the queue: %d", count)
                }
            }

            is ProgressViewHolder -> {

            }

            is EmptyViewHolder -> {

            }

            is ObtainCurrentLocationViewHolder -> {

            }
        }
    }

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ObtainCurrentLocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar = itemView.findViewById<ProgressBar>(R.id.progressbar)
    }

    class QueuedUpPhotosNotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val queuedUpPhotoCountTextView = itemView.findViewById<TextView>(R.id.queued_up_photos_count_textview)
        val cancelAllQueuedUpPhotosButton = itemView.findViewById<AppCompatButton>(R.id.cancel_all_queued_up_photos_button)
    }

    class MyPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val uploadingMessageHolderView = itemView.findViewById<CardView>(R.id.uploading_message_holder)
        val loadingProgress = itemView.findViewById<ProgressBar>(R.id.loading_progress)
        val cancelPhotoUploading = itemView.findViewById<AppCompatButton>(R.id.cancel_photo_uploading_button)
    }
}