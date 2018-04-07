package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import io.reactivex.subjects.Subject

/**
 * Created by kirakishou on 3/18/2018.
 */
class MyPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader
) : BaseAdapter<MyPhotosAdapterItem>(context) {

    private val OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX = 0
    private val UPPER_PROGRESS_INDEX = 1
    private val MY_PHOTO_VIEW_INDEX = 2

    private val photosProgressMap = mutableMapOf<Long, Int>()

    init {
        items.add(OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX, MyPhotosAdapterItem.EmptyItem())
        items.add(UPPER_PROGRESS_INDEX, MyPhotosAdapterItem.EmptyItem())
    }

    fun updatePhotoState(photoId: Long, photoState: PhotoState) {
        checkInited()

        val photoIndex = getPhotoIndex(photoId)
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

        val photoIndex = getPhotoIndex(photoId)
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

    fun addMyPhotos(photos: List<MyPhoto>) {
        for (photo in photos) {
            addMyPhoto(photo)
        }
    }

    fun addMyPhoto(photo: MyPhoto) {
        if (isPhotoAlreadyAdded(photo)) {
            return
        }

        val correctedIndex = MY_PHOTO_VIEW_INDEX

        when (photo.photoState) {
            PhotoState.PHOTO_QUEUED_UP -> {
                addQueuedUpPhoto(correctedIndex, photo)
            }
            PhotoState.PHOTO_UPLOADED -> {
                addUploadedPhoto(correctedIndex, photo)
            }

            PhotoState.PHOTO_TAKEN,
            PhotoState.PHOTO_UPLOADING,
            PhotoState.FAILED_TO_UPLOAD -> throw IllegalArgumentException("Unsupported photo state ${photo.photoState}")
        }
    }

    private fun addQueuedUpPhoto(index: Int, photo: MyPhoto) {
        items.add(index, MyPhotosAdapterItem.MyPhotoItem(photo))
        notifyItemInserted(index)
    }

    private fun addUploadedPhoto(startIndex: Int, photo: MyPhoto) {
        for (currentIndex in items.size downTo startIndex) {
            if (items.getOrNull(currentIndex) !is MyPhotosAdapterItem.MyPhotoItem) {
                continue
            }

            val currentPhotoStateInt = (items.getOrNull(currentIndex) as? MyPhotosAdapterItem.MyPhotoItem)?.myPhoto?.photoState?.state
                ?: return

            val currentPhotoState = PhotoState.from(currentPhotoStateInt)
            if (currentPhotoState != PhotoState.PHOTO_UPLOADED) {
                continue
            }

            items.add(currentIndex, MyPhotosAdapterItem.MyPhotoItem(photo))
        }
    }

    fun removePhotoByIndex(index: Int) {
        val correctedIndex = MY_PHOTO_VIEW_INDEX + index
        super.remove(correctedIndex)
    }

    fun removePhotoById(photoId: Long) {
        val photoIndex = getPhotoIndex(photoId)
        if (photoIndex == -1) {
            return
        }

        items.removeAt(photoIndex)
        notifyItemRemoved(photoIndex)
    }

    private fun isPhotoAlreadyAdded(myPhoto: MyPhoto): Boolean {
        return getPhotoIndex(myPhoto.id) != -1
    }

    private fun getPhotoIndex(photoId: Long): Int {
        return items.indexOfFirst {
            if (it !is MyPhotosAdapterItem.MyPhotoItem) {
                return@indexOfFirst false
            }

            return@indexOfFirst it.myPhoto.id == photoId
        }
    }

    override fun clear() {
        hideObtainCurrentLocationNotification()

        items.removeAll { it.getType() == AdapterItemType.VIEW_MY_PHOTO }
        notifyDataSetChanged()
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.EMPTY, R.layout.adapter_item_empty, EmptyViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_MY_PHOTO, R.layout.adapter_item_my_photo, MyPhotoViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION, R.layout.adapter_item_obtain_current_location, ObtainCurrentLocationViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyPhotoViewHolder -> {
                val myPhoto = (items.getOrNull(position) as? MyPhotosAdapterItem.MyPhotoItem)?.myPhoto
                    ?: return

                when (myPhoto.photoState) {
                    PhotoState.PHOTO_QUEUED_UP,
                    PhotoState.PHOTO_UPLOADING,
                    PhotoState.FAILED_TO_UPLOAD -> {
                        if (myPhoto.photoState == PhotoState.PHOTO_QUEUED_UP || myPhoto.photoState == PhotoState.PHOTO_UPLOADING) {
                            holder.photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_uploading_color))
                        } else {
                            holder.photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_failed_to_upload_color))
                        }

                        holder.uploadingMessageHolderView.visibility = View.VISIBLE

                        myPhoto.photoTempFile?.let { photoFile ->
                            imageLoader.loadImageFromDiskInto(photoFile, holder.photoView)
                        }

                        if (photosProgressMap.containsKey(myPhoto.id)) {
                            holder.loadingProgress.progress = photosProgressMap[myPhoto.id]!!
                        }
                    }
                    PhotoState.PHOTO_UPLOADED -> {
                        holder.photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_uploaded_color))
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

            is ProgressViewHolder -> {
                //Do nothing
            }

            is EmptyViewHolder -> {
                //Do nothing
            }

            is ObtainCurrentLocationViewHolder -> {
                //Do nothing
            }
        }
    }

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ObtainCurrentLocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar = itemView.findViewById<ProgressBar>(R.id.progressbar)
    }

    class MyPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val uploadingMessageHolderView = itemView.findViewById<CardView>(R.id.uploading_message_holder)
        val loadingProgress = itemView.findViewById<ProgressBar>(R.id.loading_progress)
        val photoUploadingStateIndicator = itemView.findViewById<View>(R.id.photo_uploading_state_indicator)
    }
}