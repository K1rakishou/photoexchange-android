package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseAdapter
import com.kirakishou.photoexchange.mvvm.model.AdapterItemType
import com.kirakishou.photoexchange.mvvm.model.SentPhoto
import timber.log.Timber

/**
 * Created by kirakishou on 11/7/2017.
 */
class SentPhotosAdapter(
        private val context: Context
) : BaseAdapter<SentPhoto>(context) {

    fun updateType(photoId: Long, photoName: String) {
        var found = false

        for ((idx, item) in items.withIndex()) {
            if (item.getType() != AdapterItemType.VIEW_PROGRESSBAR.ordinal) {
                continue
            }

            if (!item.value.isPresent()) {
                continue
            }

            if (item.value.get().photoId == photoId) {
                found = true

                item.setType(AdapterItemType.VIEW_ITEM)
                item.value.get().photoName = photoName
                notifyItemChanged(idx)
                break
            }
        }

        if (!found) {
            Timber.e("adapter does not contain photo with id $photoId")
        }
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
                BaseAdapterInfo(AdapterItemType.VIEW_ITEM, R.layout.adapter_item_sent_photo, SentPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PROGRESSBAR, R.layout.adapter_item_progress, ProgressBarViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()

                    Glide.with(context)
                            .load(item.photoName)
                            .apply(RequestOptions().centerCrop())
                            .into(holder.sentPhoto)
                }
            }

            is ProgressBarViewHolder -> {
                holder.progressBar.isIndeterminate = true
            }
        }
    }

    class SentPhotoViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

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
}