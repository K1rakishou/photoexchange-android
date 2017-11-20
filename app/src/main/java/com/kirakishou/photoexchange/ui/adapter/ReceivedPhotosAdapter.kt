package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
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
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 11/17/2017.
 */
class ReceivedPhotosAdapter(
        private val context: Context
) : BaseAdapter<PhotoAnswer>(context) {

    private val selector = IdSelectorFunctionImpl()
    private val duplicatesCheckerSet = mutableSetOf<Long>()

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

        items.add(0, item)
        notifyItemInserted(0)
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

    override fun add(item: AdapterItem<PhotoAnswer>) {
        if (isDuplicate(item)) {
            return
        }

        super.add(item)
    }

    override fun addAll(items: List<AdapterItem<PhotoAnswer>>) {
        super.addAll(items.filter { isDuplicate(it) })
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
                BaseAdapterInfo(AdapterItemType.VIEW_ITEM, R.layout.adapter_item_photo, ReceivedPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PROGRESSBAR, R.layout.adapter_item_progress, ProgressBarViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_LOOKING_FOR_PHOTO, R.layout.adapter_item_looking_for_photo, LookingForPhotoViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ReceivedPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()
                    val fullPath = "${PhotoExchangeApplication.baseUrl}v1/api/get_photo/${item.photoName}"

                    holder.photoId.text = item.photoRemoteId.toString()

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

            is LookingForPhotoViewHolder -> {
                holder.progressBar.isIndeterminate = true
            }
        }
    }

    class ReceivedPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.photo_id)
        lateinit var photoId: TextView

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

    interface IdSelectorFunction {
        fun select(item: PhotoAnswer): Long
    }

    inner class IdSelectorFunctionImpl : IdSelectorFunction {
        override fun select(item: PhotoAnswer): Long = item.photoRemoteId
    }
}