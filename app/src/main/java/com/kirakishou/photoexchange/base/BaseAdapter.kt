package com.kirakishou.photoexchange.base

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kirakishou.photoexchange.mvvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mvvm.model.other.AdapterItemType

/**
 * Created by kirakishou on 11/7/2017.
 */

abstract class BaseAdapter<T>(mContext: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    protected lateinit var handler: Handler
    protected val items = mutableListOf<AdapterItem<T>>()
    private val layoutInflater = LayoutInflater.from(mContext)
    private var baseAdapterInfo = mutableListOf<BaseAdapterInfo>()
    private var isInited = false

    fun init() {
        handler = Handler(Looper.getMainLooper())
        baseAdapterInfo = getBaseAdapterInfo()

        isInited = true
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        super.onDetachedFromRecyclerView(recyclerView)

        baseAdapterInfo.clear()
        handler.removeCallbacksAndMessages(null)
    }

    protected fun checkInited() {
        if (!isInited) {
            throw IllegalStateException("Must call BaseAdapter.init() first!")
        }
    }

    open fun add(item: AdapterItem<T>) {
        checkInited()

        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    open fun addAll(items: List<AdapterItem<T>>) {
        checkInited()

        this.items.addAll(items)
        notifyDataSetChanged()
    }

    open fun remove(position: Int) {
        checkInited()

        items.removeAt(position)
        notifyItemRemoved(position)
    }

    open fun clear() {
        checkInited()

        items.clear()
        notifyDataSetChanged()
    }

    fun runOnAdapterHandler(func: () -> Unit) {
        handler.post(func)
    }

    fun runOnAdapterHandlerWithDelay(delayInMs: Long, func: () -> Unit) {
        handler.postDelayed({ func() }, delayInMs)
    }

    override fun getItemViewType(position: Int) = items[position].getType()
    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        for (adapterInfo in baseAdapterInfo) {
            if (adapterInfo.viewType.ordinal == viewType) {
                val view = layoutInflater.inflate(adapterInfo.layoutId, parent, false)
                return adapterInfo.viewHolderClazz.getDeclaredConstructor(View::class.java).newInstance(view) as RecyclerView.ViewHolder
            }
        }

        throw IllegalStateException("viewType $viewType not found!")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onViewHolderBound(holder, position)
    }

    abstract fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo>
    abstract fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int)

    inner class BaseAdapterInfo(val viewType: AdapterItemType,
                                val layoutId: Int,
                                val viewHolderClazz: Class<*>)


}