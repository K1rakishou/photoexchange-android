package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class GalleryPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val columnsCount: Int
) : BaseAdapter<GalleryPhotosAdapterItem>(context) {

    private val compositeDisposable = CompositeDisposable()
    private val IMAGES_PER_COLUMN = 5
    private val items = arrayListOf<GalleryPhotosAdapterItem>()

    override fun init() {
        super.init()

        compositeDisposable += imageLoader.getImageLoadingQueueObservable()
            .subscribeOn(AndroidSchedulers.mainThread())
            .buffer(1, TimeUnit.SECONDS, Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { imageInfoList ->
                for (imageInfo in imageInfoList.takeLast(columnsCount * IMAGES_PER_COLUMN)) {
                    val view = imageInfo.view.get()
                        ?: continue

                    imageLoader.loadImageFromNetInto(imageInfo.photoName, imageInfo.photoSize, view)
                }
            }
            .subscribe()
    }

    override fun cleanUp() {
        super.cleanUp()

        compositeDisposable.clear()
    }

    fun addAll(photos: List<GalleryPhoto>) {
        items.addAll(photos.map { GalleryPhotosAdapterItem.GalleryPhotoItem(it) })
        notifyItemRangeInserted(items.size, photos.size)
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].getType().type
    }

    override fun getItemCount(): Int = items.size

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.VIEW_GALLERY_PHOTO, R.layout.adapter_item_gallery_photo, GalleryPhotoViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is GalleryPhotoViewHolder -> {
                val item = items[position] as GalleryPhotosAdapterItem.GalleryPhotoItem

                holder.photoIdTextView.text = item.photo.remoteId.toString()
                imageLoader.loadImageFromNetAsync(item.photo.photoName, ImageLoader.PhotoSize.Small, holder.photoView)
            }
            else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
        }
    }

    class GalleryPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val galleryPhotoHolder = itemView.findViewById<LinearLayout>(R.id.gallery_photo_holder)
        val photoIdTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
    }
}