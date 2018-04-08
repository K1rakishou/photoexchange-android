package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import timber.log.Timber

/**
 * Created by kirakishou on 3/18/2018.
 */
class MyPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader
) : BaseAdapter<MyPhotosAdapterItem>(context) {

    private val HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX = 0
    private val HEADER_PROGRESS_INDEX = 1

    private val headerItems = arrayListOf<MyPhotosAdapterItem>()
    private val queuedUpItems = arrayListOf<MyPhotosAdapterItem>()
    private val failedToUploadItems = arrayListOf<MyPhotosAdapterItem>()
    private val uploadedItems = arrayListOf<MyPhotosAdapterItem>()

    private val duplicatesCheckerSet = hashSetOf<Long>()
    private val photosProgressMap = hashMapOf<Long, Int>()

    init {
        headerItems.add(HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX, MyPhotosAdapterItem.EmptyItem())
        headerItems.add(HEADER_PROGRESS_INDEX, MyPhotosAdapterItem.EmptyItem())
    }

    fun updatePhotoState(photoId: Long, photoState: PhotoState) {
        checkInited()

        updateAdapterItemById(photoId) { oldPhoto ->
            oldPhoto.also { it.photoState = photoState }
        }
    }

    fun updatePhotoProgress(photoId: Long, newProgress: Int) {
        checkInited()

        if (!isPhotoAlreadyAdded(photoId)) {
            return
        }

        val photoIndex = getPhotoGlobalIndexById(photoId)

        photosProgressMap[photoId] = newProgress
        notifyItemChanged(photoIndex)
    }

    fun showObtainCurrentLocationNotification() {
    }

    fun hideObtainCurrentLocationNotification() {
    }

    fun addMyPhotos(photos: List<MyPhoto>) {
        for (photo in photos) {
            addMyPhoto(photo)
        }
    }

    fun addMyPhoto(photo: MyPhoto) {
        if (!duplicatesCheckerSet.add(photo.id)) {
            return
        }

        when (photo.photoState) {
            PhotoState.PHOTO_QUEUED_UP,
            PhotoState.PHOTO_UPLOADING -> {
                addQueuedUpAdapterItem(headerItems.size, photo)
            }
            PhotoState.FAILED_TO_UPLOAD -> {
                addFailedToUploadAdapterItem(headerItems.size + queuedUpItems.size, photo)
            }
            PhotoState.PHOTO_UPLOADED -> {
                addUploadedAdapterItem(headerItems.size + queuedUpItems.size + failedToUploadItems.size, photo)
            }

            PhotoState.PHOTO_TAKEN -> {
                //Do nothing
            }
        }
    }

    private fun addQueuedUpAdapterItem(index: Int, photo: MyPhoto) {
        queuedUpItems.add(0, MyPhotosAdapterItem.MyPhotoItem(photo))
        notifyItemInserted(index)
    }

    private fun addFailedToUploadAdapterItem(index: Int, photo: MyPhoto) {
        failedToUploadItems.add(0, MyPhotosAdapterItem.FailedToUploadItem(photo))
        notifyItemInserted(index)
    }

    private fun addUploadedAdapterItem(index: Int, photo: MyPhoto) {
        uploadedItems.add(0, MyPhotosAdapterItem.MyPhotoItem(photo))
        notifyItemInserted(index)
    }

    private fun isPhotoAlreadyAdded(photoId: Long): Boolean {
        return duplicatesCheckerSet.contains(photoId)
    }

    private fun isPhotoAlreadyAdded(myPhoto: MyPhoto): Boolean {
        return isPhotoAlreadyAdded(myPhoto.id)
    }

    fun removePhotoById(photoId: Long) {
        if (!isPhotoAlreadyAdded(photoId)) {
            return
        }

        var globalIndex = headerItems.size
        var localIndex = -1

        duplicatesCheckerSet.remove(photoId)

        for ((index, adapterItem) in queuedUpItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.MyPhotoItem
            if (adapterItem.myPhoto.id == photoId) {
                localIndex = index
                break
            }

            ++globalIndex
        }

        if (localIndex != -1) {
            queuedUpItems.removeAt(localIndex)
            notifyItemRemoved(globalIndex)
            return
        }

        for ((index, adapterItem) in failedToUploadItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.FailedToUploadItem
            if (adapterItem.failedToUploadPhoto.id == photoId) {
                localIndex = index
                break
            }

            ++globalIndex
        }

        if (localIndex != -1) {
            failedToUploadItems.removeAt(localIndex)
            notifyItemRemoved(globalIndex)
            return
        }

        for ((index, adapterItem) in uploadedItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.MyPhotoItem
            if (adapterItem.myPhoto.id == photoId) {
                localIndex = index
                break
            }

            ++globalIndex
        }

        if (localIndex != -1) {
            uploadedItems.removeAt(localIndex)
            notifyItemRemoved(globalIndex)
        }
    }

    private fun getPhotoGlobalIndexById(photoId: Long): Int {
        if (!isPhotoAlreadyAdded(photoId)) {
            return -1
        }

        var index = headerItems.size

        for (adapterItem in queuedUpItems) {
            adapterItem as MyPhotosAdapterItem.MyPhotoItem
            if (adapterItem.myPhoto.id == photoId) {
                return index
            }

            ++index
        }

        for (adapterItem in failedToUploadItems) {
            adapterItem as MyPhotosAdapterItem.FailedToUploadItem
            if (adapterItem.failedToUploadPhoto.id == photoId) {
                return index
            }

            ++index
        }

        for (adapterItem in uploadedItems) {
            adapterItem as MyPhotosAdapterItem.MyPhotoItem
            if (adapterItem.myPhoto.id == photoId) {
                return index
            }

            ++index
        }

        return -1
    }

    private fun getAdapterItemByIndex(index: Int): MyPhotosAdapterItem? {
        val headerItemsRange = IntRange(0, headerItems.size - 1)
        val queuedUpItemsRange = IntRange(headerItemsRange.endInclusive, headerItemsRange.endInclusive + queuedUpItems.size)
        val failedToUploadItemsRange = IntRange(queuedUpItemsRange.endInclusive, queuedUpItemsRange.endInclusive + failedToUploadItems.size)
        val uploadedItemsRange = IntRange(failedToUploadItemsRange.endInclusive, failedToUploadItemsRange.endInclusive + uploadedItems.size)

        return when (index) {
            in headerItemsRange -> {
                headerItems[index]
            }
            in queuedUpItemsRange -> {
                queuedUpItems[index - headerItems.size]
            }
            in failedToUploadItemsRange -> {
                failedToUploadItems[index - (headerItems.size + queuedUpItems.size)]
            }
            in uploadedItemsRange -> {
                uploadedItems[index - (headerItems.size + queuedUpItems.size + failedToUploadItems.size)]
            }
            else -> null
        }
    }

    private fun updateAdapterItemById(photoId: Long, updateFunction: (photo: MyPhoto) -> MyPhoto) {
        var currentIndex = headerItems.size

        if (isPhotoAlreadyAdded(photoId)) {
            return
        }

        for ((localIndex, adapterItem) in queuedUpItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.MyPhotoItem
            if (adapterItem.myPhoto.id == photoId) {
                val updatedAdapterItem = updateFunction(adapterItem.myPhoto)
                queuedUpItems[localIndex] = MyPhotosAdapterItem.MyPhotoItem(updatedAdapterItem)
                notifyItemChanged(currentIndex)
                return
            }

            ++currentIndex
        }

        for ((localIndex, adapterItem) in failedToUploadItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.FailedToUploadItem
            if (adapterItem.failedToUploadPhoto.id == photoId) {
                val updatedAdapterItem = updateFunction(adapterItem.failedToUploadPhoto)
                failedToUploadItems[localIndex] = MyPhotosAdapterItem.FailedToUploadItem(updatedAdapterItem)
                notifyItemChanged(currentIndex)
                return
            }

            ++currentIndex
        }

        for ((localIndex, adapterItem) in uploadedItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.MyPhotoItem
            if (adapterItem.myPhoto.id == photoId) {
                val updatedAdapterItem = updateFunction(adapterItem.myPhoto)
                uploadedItems[localIndex] = MyPhotosAdapterItem.MyPhotoItem(updatedAdapterItem)
                notifyItemChanged(currentIndex)
                return
            }

            ++currentIndex
        }
    }

    fun clear() {
        hideObtainCurrentLocationNotification()

        queuedUpItems.removeAll { it.getType() == AdapterItemType.VIEW_MY_PHOTO }
        failedToUploadItems.removeAll { it.getType() == AdapterItemType.VIEW_MY_PHOTO }
        uploadedItems.removeAll { it.getType() == AdapterItemType.VIEW_MY_PHOTO }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return getAdapterItemByIndex(position)!!.getType().type
    }

    override fun getItemCount(): Int {
        return headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.EMPTY, R.layout.adapter_item_empty, EmptyViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_MY_PHOTO, R.layout.adapter_item_my_photo, MyPhotoViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION,
                R.layout.adapter_item_obtain_current_location, ObtainCurrentLocationViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_FAILED_TO_UPLOAD,
                R.layout.adapter_failed_to_upload_photo, FailedToUploadPhotoViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyPhotoViewHolder -> {
                val myPhoto = (getAdapterItemByIndex(position) as? MyPhotosAdapterItem.MyPhotoItem)?.myPhoto
                    ?: return

                holder.photoidTextView.text = myPhoto.id.toString()

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

            is FailedToUploadPhotoViewHolder -> {
                val failedPhoto = (getAdapterItemByIndex(position) as? MyPhotosAdapterItem.FailedToUploadItem)?.failedToUploadPhoto
                    ?: return

                require(failedPhoto.photoState == PhotoState.FAILED_TO_UPLOAD)

                failedPhoto.photoTempFile?.let { photoFile ->
                    imageLoader.loadImageFromDiskInto(photoFile, holder.photoView)
                }

                holder.deleteFailedToUploadPhotoButton.setOnClickListener {
                    Timber.e("deleteFailedToUploadPhotoButton")
                }

                holder.retryToUploadFailedPhoto.setOnClickListener {
                    Timber.e("retryToUploadFailedPhoto")
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

    class FailedToUploadPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val deleteFailedToUploadPhotoButton = itemView.findViewById<AppCompatButton>(R.id.delete_failed_to_upload_photo)
        val retryToUploadFailedPhoto = itemView.findViewById<AppCompatButton>(R.id.retry_to_upload_failed_photo)
    }

    class MyPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoidTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val uploadingMessageHolderView = itemView.findViewById<CardView>(R.id.uploading_message_holder)
        val loadingProgress = itemView.findViewById<ProgressBar>(R.id.loading_progress)
        val photoUploadingStateIndicator = itemView.findViewById<View>(R.id.photo_uploading_state_indicator)
    }
}