package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import butterknife.BindView
import butterknife.ButterKnife
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.adapter.AdapterItemType

/**
 * Created by kirakishou on 3/18/2018.
 */
class MyPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader
) : BaseAdapter<MyPhoto>(context) {

    private val photosProgressMap = mutableMapOf<Long, Int>()

    fun updatePhotoState(photoId: Long, photoState: PhotoState) {
        checkInited()

        val photoIndex = items.indexOfFirst { it.value?.id == photoId }
        if (photoIndex == -1) {
            return
        }

        items.getOrNull(photoIndex)?.value?.photoState = photoState
        notifyItemChanged(photoIndex)
    }

    fun updatePhotoProgress(photoId: Long, newProgress: Int) {
        checkInited()

        val photoIndex = items.indexOfFirst { it.value?.id == photoId }
        if (photoIndex == -1) {
            return
        }

        photosProgressMap[photoId] = newProgress
        notifyItemChanged(photoIndex)
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.VIEW_MY_PHOTO, R.layout.adapter_item_my_photo, MyPhotoViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MyPhotoViewHolder -> {
                val myPhoto = items.getOrNull(position)?.value
                    ?: return

                when (myPhoto.photoState) {
                    PhotoState.PHOTO_TO_BE_UPLOADED,
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

                    else -> {
                        //do nothing
                    }
                }
            }
        }
    }

    class MyPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.photo_view)
        lateinit var photoView: ImageView

        @BindView(R.id.uploading_message_holder)
        lateinit var uploadingMessageHolderView: LinearLayout

        @BindView(R.id.loading_progress)
        lateinit var loadingProgress: ProgressBar

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}