package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import io.reactivex.subjects.Subject

/**
 * Created by kirakishou on 3/18/2018.
 */
class UploadedPhotosAdapter(
  private val context: Context,
  private val imageLoader: ImageLoader,
  private val adapterButtonsClickSubject: Subject<UploadedPhotosAdapterButtonClick>
) : BaseAdapter<UploadedPhotosAdapterItem>(context) {


  private val headerItems = arrayListOf<UploadedPhotosAdapterItem>()
  private val queuedUpItems = arrayListOf<UploadedPhotosAdapterItem>()
  private val failedToUploadItems = arrayListOf<UploadedPhotosAdapterItem>()
  private val uploadedItems = arrayListOf<UploadedPhotosAdapterItem>()
  private val uploadedWithReceiverInfoItems = arrayListOf<UploadedPhotosAdapterItem>()
  private var footerItems = arrayListOf<UploadedPhotosAdapterItem>()

  private val duplicatesCheckerSet = hashSetOf<Long>()
  private val photosProgressMap = hashMapOf<Long, Int>()

  fun getQueuedUpAndFailedPhotosCount(): Int {
    return queuedUpItems.size + failedToUploadItems.size
  }

  fun getUploadedPhotosCount(): Int {
    return uploadedItems.size + uploadedWithReceiverInfoItems.size
  }

  fun updateUploadedPhotoSetReceiverInfo(takenPhotoName: String) {
    val uploadedItemIndex = uploadedItems.indexOfFirst { (it as UploadedPhotosAdapterItem.UploadedPhotoItem).uploadedPhoto.photoName == takenPhotoName }
    if (uploadedItemIndex == -1) {
      return
    }

    val uploadedPhoto = (uploadedItems[uploadedItemIndex] as UploadedPhotosAdapterItem.UploadedPhotoItem).uploadedPhoto
    uploadedPhoto.hasReceiverInfo = true

    uploadedItems.removeAt(uploadedItemIndex)
    uploadedWithReceiverInfoItems.add(0, UploadedPhotosAdapterItem.UploadedPhotoItem(uploadedPhoto))

    notifyItemRemoved(headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItemIndex)
    notifyItemInserted(headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size)
  }

  fun updateAllPhotosState(newPhotoState: PhotoState) {
    for (photoIndex in 0 until queuedUpItems.size) {
      val item = queuedUpItems[photoIndex] as UploadedPhotosAdapterItem.TakenPhotoItem
      val failedToUploadItem = UploadedPhotosAdapterItem.FailedToUploadItem(item.takenPhoto)
      failedToUploadItem.failedToUploadPhoto.photoState = newPhotoState

      failedToUploadItems.add(0, failedToUploadItem)
    }

    notifyItemRangeRemoved(headerItems.size, queuedUpItems.size)
    notifyItemRangeChanged(headerItems.size + queuedUpItems.size, queuedUpItems.size)

    queuedUpItems.clear()
  }

  fun updatePhotoProgress(photoId: Long, newProgress: Int) {
    if (!isPhotoAlreadyAdded(photoId)) {
      return
    }

    val photoIndex = getPhotoGlobalIndexById(photoId)

    photosProgressMap[photoId] = newProgress
    notifyItemChanged(photoIndex)
  }

  fun clearFooter(removeFooter: Boolean = true) {
    if (footerItems.isEmpty()) {
      return
    }

    footerItems.clear()

    if (removeFooter) {
      notifyItemRemoved(headerItems.size + queuedUpItems.size + failedToUploadItems.size +
        uploadedItems.size + uploadedWithReceiverInfoItems.size)
    }
  }

  fun showProgressFooter() {
    val isFooterNotEmpty = footerItems.isNotEmpty()
    clearFooter(false)

    if (isFooterNotEmpty) {
      footerItems.add(UploadedPhotosAdapterItem.ProgressItem())
      notifyItemChanged(headerItems.size + queuedUpItems.size + failedToUploadItems.size +
        uploadedItems.size + uploadedWithReceiverInfoItems.size + footerItems.size)
    } else {
      footerItems.add(UploadedPhotosAdapterItem.ProgressItem())
      notifyItemInserted(headerItems.size + queuedUpItems.size + failedToUploadItems.size +
        uploadedItems.size + uploadedWithReceiverInfoItems.size + footerItems.size)
    }
  }

  fun showMessageFooter(message: String) {
    val isFooterNotEmpty = footerItems.isNotEmpty()
    clearFooter(false)

    if (isFooterNotEmpty) {
      footerItems.add(UploadedPhotosAdapterItem.MessageItem(message))
      notifyItemChanged(headerItems.size + queuedUpItems.size + failedToUploadItems.size +
        uploadedItems.size + uploadedWithReceiverInfoItems.size + footerItems.size)
    } else {
      footerItems.add(UploadedPhotosAdapterItem.MessageItem(message))
      notifyItemInserted(headerItems.size + queuedUpItems.size + failedToUploadItems.size +
        uploadedItems.size + uploadedWithReceiverInfoItems.size + footerItems.size)
    }
  }

  fun addTakenPhotos(photos: List<TakenPhoto>) {
    for (photo in photos) {
      addTakenPhoto(photo)
    }
  }

  fun addTakenPhoto(photo: TakenPhoto) {
    if (isPhotoAlreadyAdded(photo)) {
      return
    }

    duplicatesCheckerSet.add(photo.id)

    when (photo.photoState) {
      PhotoState.PHOTO_QUEUED_UP,
      PhotoState.PHOTO_UPLOADING -> {
        queuedUpItems.add(0, UploadedPhotosAdapterItem.TakenPhotoItem(photo))
        notifyItemInserted(headerItems.size)
      }
      PhotoState.FAILED_TO_UPLOAD -> {
        failedToUploadItems.add(0, UploadedPhotosAdapterItem.FailedToUploadItem(photo))
        notifyItemInserted(headerItems.size + queuedUpItems.size)
      }
      PhotoState.PHOTO_TAKEN -> {
        //Do nothing
      }
      else -> throw IllegalArgumentException("Unknown photoState: ${photo.photoState}")
    }
  }

  fun addUploadedPhotos(photos: List<UploadedPhoto>) {
    val filteredReceivedPhotos = photos
      .filter { photo -> !isPhotoAlreadyAdded(photo) }
      .map { photo -> UploadedPhotosAdapterItem.UploadedPhotoItem(photo) }

    filteredReceivedPhotos.forEach { photo -> duplicatesCheckerSet.add(photo.uploadedPhoto.photoId) }

    val photosWithoutReceiverInfo = filteredReceivedPhotos.filter { !it.uploadedPhoto.hasReceiverInfo }
    val photosWithReceiverInfo = filteredReceivedPhotos.filter { it.uploadedPhoto.hasReceiverInfo }

    uploadedItems.addAll(photosWithoutReceiverInfo)
    notifyItemRangeInserted(headerItems.size + queuedUpItems.size + failedToUploadItems.size, photosWithoutReceiverInfo.size)

    uploadedWithReceiverInfoItems.addAll(photosWithReceiverInfo)
    notifyItemRangeInserted(headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size, photosWithReceiverInfo.size)
  }

  fun removePhotoById(photoId: Long) {
    if (!isPhotoAlreadyAdded(photoId)) {
      return
    }

    duplicatesCheckerSet.remove(photoId)

    var globalIndex = headerItems.size
    var localIndex = -1

    for ((index, adapterItem) in queuedUpItems.withIndex()) {
      adapterItem as UploadedPhotosAdapterItem.TakenPhotoItem
      if (adapterItem.takenPhoto.id == photoId) {
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
      adapterItem as UploadedPhotosAdapterItem.FailedToUploadItem
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
      adapterItem as UploadedPhotosAdapterItem.UploadedPhotoItem
      if (adapterItem.uploadedPhoto.photoId == photoId) {
        localIndex = index
        break
      }

      ++globalIndex
    }

    if (localIndex != -1) {
      uploadedItems.removeAt(localIndex)
      notifyItemRemoved(globalIndex)
    }

    for ((index, adapterItem) in uploadedWithReceiverInfoItems.withIndex()) {
      adapterItem as UploadedPhotosAdapterItem.UploadedPhotoItem
      if (adapterItem.uploadedPhoto.photoId == photoId) {
        localIndex = index
        break
      }

      ++globalIndex
    }

    if (localIndex != -1) {
      uploadedWithReceiverInfoItems.removeAt(localIndex)
      notifyItemRemoved(globalIndex)
    }
  }

  private fun getPhotoGlobalIndexById(photoId: Long): Int {
    if (!isPhotoAlreadyAdded(photoId)) {
      return -1
    }

    var index = headerItems.size

    for (adapterItem in queuedUpItems) {
      adapterItem as UploadedPhotosAdapterItem.TakenPhotoItem
      if (adapterItem.takenPhoto.id == photoId) {
        return index
      }

      ++index
    }

    for (adapterItem in failedToUploadItems) {
      adapterItem as UploadedPhotosAdapterItem.FailedToUploadItem
      if (adapterItem.failedToUploadPhoto.id == photoId) {
        return index
      }

      ++index
    }

    for (adapterItem in uploadedItems) {
      adapterItem as UploadedPhotosAdapterItem.UploadedPhotoItem
      if (adapterItem.uploadedPhoto.photoId == photoId) {
        return index
      }

      ++index
    }

    for (adapterItem in uploadedWithReceiverInfoItems) {
      adapterItem as UploadedPhotosAdapterItem.UploadedPhotoItem
      if (adapterItem.uploadedPhoto.photoId == photoId) {
        return index
      }

      ++index
    }

    return -1
  }

  private fun getAdapterItemByIndex(index: Int): UploadedPhotosAdapterItem? {
    val headerItemsRange = IntRange(0, headerItems.size - 1)
    val queuedUpItemsRange = IntRange(headerItemsRange.endInclusive, headerItemsRange.endInclusive + queuedUpItems.size)
    val failedToUploadItemsRange = IntRange(queuedUpItemsRange.endInclusive, queuedUpItemsRange.endInclusive + failedToUploadItems.size)
    val uploadedItemsRange = IntRange(failedToUploadItemsRange.endInclusive, failedToUploadItemsRange.endInclusive + uploadedItems.size)
    val uploadedWithAnswerItemsRange = IntRange(uploadedItemsRange.endInclusive, uploadedItemsRange.endInclusive + uploadedWithReceiverInfoItems.size)
    val footerItemsRange = IntRange(uploadedWithAnswerItemsRange.endInclusive, uploadedWithAnswerItemsRange.endInclusive + footerItems.size)

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
      in uploadedWithAnswerItemsRange -> {
        uploadedWithReceiverInfoItems[index - (headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size)]
      }
      in footerItemsRange -> {
        footerItems[index - (headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size + uploadedWithReceiverInfoItems.size)]
      }
      else -> null
    }
  }

  private fun isPhotoAlreadyAdded(photoId: Long): Boolean {
    return duplicatesCheckerSet.contains(photoId)
  }

  private fun isPhotoAlreadyAdded(takenPhoto: TakenPhoto): Boolean {
    return isPhotoAlreadyAdded(takenPhoto.id)
  }

  private fun isPhotoAlreadyAdded(uploadedPhoto: UploadedPhoto): Boolean {
    return isPhotoAlreadyAdded(uploadedPhoto.photoId)
  }

  fun clear() {
    headerItems.clear()
    queuedUpItems.clear()
    failedToUploadItems.clear()
    uploadedItems.clear()
    uploadedWithReceiverInfoItems.clear()
    footerItems.clear()

    duplicatesCheckerSet.clear()
    photosProgressMap.clear()

    notifyDataSetChanged()
  }

  override fun getItemViewType(position: Int): Int {
    return getAdapterItemByIndex(position)!!.getType().type
  }

  override fun getItemCount(): Int {
    return headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size + uploadedWithReceiverInfoItems.size + footerItems.size
  }

  override fun doGetBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
    return mutableListOf(
      BaseAdapterInfo(AdapterItemType.EMPTY, R.layout.adapter_item_empty, EmptyViewHolder::class.java),
      BaseAdapterInfo(AdapterItemType.VIEW_TAKEN_PHOTO, R.layout.adapter_item_taken_photo, TakenPhotoViewHolder::class.java),
      BaseAdapterInfo(AdapterItemType.VIEW_UPLOADED_PHOTO, R.layout.adapter_item_uploaded_photo, UploadedPhotoViewHolder::class.java),
      BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java),
      BaseAdapterInfo(AdapterItemType.VIEW_FAILED_TO_UPLOAD, R.layout.adapter_failed_to_upload_photo, FailedToUploadPhotoViewHolder::class.java),
      BaseAdapterInfo(AdapterItemType.VIEW_MESSAGE, R.layout.adapter_item_message, MessageViewHolder::class.java)
    )
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    when (holder) {
      is TakenPhotoViewHolder -> {
        val takenPhoto = (getAdapterItemByIndex(position) as? UploadedPhotosAdapterItem.TakenPhotoItem)?.takenPhoto
          ?: return

        holder.photoidTextView.text = takenPhoto.id.toString()

        when (takenPhoto.photoState) {
          PhotoState.PHOTO_QUEUED_UP,
          PhotoState.PHOTO_UPLOADING,
          PhotoState.FAILED_TO_UPLOAD -> {
            if (takenPhoto.photoState == PhotoState.PHOTO_QUEUED_UP || takenPhoto.photoState == PhotoState.PHOTO_UPLOADING) {
              holder.photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_uploading_color))
            } else {
              holder.photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_failed_to_upload_color))
            }

            holder.uploadingMessageHolderView.visibility = View.VISIBLE

            takenPhoto.photoTempFile?.let { photoFile ->
              imageLoader.loadPhotoFromDiskInto(photoFile, holder.photoView)
            }

            if (photosProgressMap.containsKey(takenPhoto.id)) {
              if (holder.loadingProgress.isIndeterminate) {
                holder.loadingProgress.isIndeterminate = false
              }

              holder.loadingProgress.progress = photosProgressMap[takenPhoto.id]!!
            }
          }
          PhotoState.PHOTO_TAKEN -> {
            throw IllegalStateException("uploadedPhoto with state PHOTO_TAKEN should not be here!")
          }
        }
      }
      is UploadedPhotoViewHolder -> {
        val uploadedPhoto = (getAdapterItemByIndex(position) as? UploadedPhotosAdapterItem.UploadedPhotoItem)?.uploadedPhoto
          ?: return

        val drawable = if (!uploadedPhoto.hasReceiverInfo) {
          context.getDrawable(R.drawable.ic_done)
        } else {
          context.getDrawable(R.drawable.ic_done_all)
        }

        val color = if (!uploadedPhoto.hasReceiverInfo) {
          ColorDrawable(context.resources.getColor(R.color.photo_state_uploaded_color))
        } else {
          ColorDrawable(context.resources.getColor(R.color.photo_state_exchanged_color))
        }

        holder.photoidTextView.text = uploadedPhoto.photoId.toString()
        holder.receivedIconImageView.setImageDrawable(drawable)
        holder.photoUploadingStateIndicator.background = color
        holder.receivedIconImageView.visibility = View.VISIBLE

        uploadedPhoto.photoName.let { photoName ->
          imageLoader.loadPhotoFromNetInto(photoName, holder.photoView)
        }

        photosProgressMap.remove(uploadedPhoto.photoId)
      }
      is FailedToUploadPhotoViewHolder -> {
        val failedPhoto = (getAdapterItemByIndex(position) as? UploadedPhotosAdapterItem.FailedToUploadItem)?.failedToUploadPhoto
          ?: return

        require(failedPhoto.photoState == PhotoState.FAILED_TO_UPLOAD)

        failedPhoto.photoTempFile?.let { photoFile ->
          imageLoader.loadPhotoFromDiskInto(photoFile, holder.photoView)
        }

        holder.deleteFailedToUploadPhotoButton.setOnClickListener {
          adapterButtonsClickSubject.onNext(UploadedPhotosAdapterButtonClick.DeleteButtonClick(failedPhoto))
        }

        holder.retryToUploadFailedPhoto.setOnClickListener {
          adapterButtonsClickSubject.onNext(UploadedPhotosAdapterButtonClick.RetryButtonClick(failedPhoto))
        }
      }
      is MessageViewHolder -> {
        val messageItem = (getAdapterItemByIndex(position) as? UploadedPhotosAdapterItem.MessageItem)
          ?: return

        holder.message.text = messageItem.message
      }
      is ProgressViewHolder -> {
        //Do nothing
      }
      is EmptyViewHolder -> {
        //Do nothing
      }
      else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
    }
  }

  sealed class UploadedPhotosAdapterButtonClick {
    class DeleteButtonClick(val photo: TakenPhoto) : UploadedPhotosAdapterButtonClick()
    class RetryButtonClick(val photo: TakenPhoto) : UploadedPhotosAdapterButtonClick()
  }

  companion object {
    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val progressBar = itemView.findViewById<ProgressBar>(R.id.progressbar)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val message = itemView.findViewById<TextView>(R.id.message)
    }

    class FailedToUploadPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
      val deleteFailedToUploadPhotoButton = itemView.findViewById<AppCompatButton>(R.id.delete_failed_to_upload_photo)
      val retryToUploadFailedPhoto = itemView.findViewById<AppCompatButton>(R.id.retry_to_upload_failed_photo)
    }

    class UploadedPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val photoidTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
      val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
      val photoUploadingStateIndicator = itemView.findViewById<View>(R.id.photo_uploading_state_indicator)
      val receivedIconImageView = itemView.findViewById<ImageView>(R.id.received_icon_image_view)
    }

    class TakenPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
      val photoidTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
      val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
      val uploadingMessageHolderView = itemView.findViewById<androidx.cardview.widget.CardView>(R.id.uploading_message_holder)
      val loadingProgress = itemView.findViewById<ProgressBar>(R.id.loading_progress)
      val photoUploadingStateIndicator = itemView.findViewById<View>(R.id.photo_uploading_state_indicator)
    }
  }
}