package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created by kirakishou on 3/18/2018.
 */
abstract class BaseAdapter<T : BaseAdapterItem>(
    context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    protected val items = mutableListOf<T>()
    private val layoutInflater = LayoutInflater.from(context)
    private var baseAdapterInfo = mutableListOf<BaseAdapterInfo>()
    private var isInited = false

    fun init() {
        baseAdapterInfo = getBaseAdapterInfo()

        isInited = true
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)

        baseAdapterInfo.clear()
    }

    protected fun checkInited() {
        if (!isInited) {
            throw IllegalStateException("Must call BaseAdapter.init() first!")
        }
    }

    open fun add(item: T) {
        checkInited()

        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    open fun add(index: Int, item: T) {
        checkInited()

        items.add(index, item)
        notifyItemInserted(index)
    }

    open fun addAll(items: List<T>) {
        checkInited()

        this.items.addAll(items)
        notifyDataSetChanged()
    }

    open fun remove(index: Int) {
        checkInited()

        items.removeAt(index)
        notifyItemRemoved(index)
    }

    open fun clear() {
        checkInited()

        items.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int) = items[position].getType().type
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

    abstract fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo>

    inner class BaseAdapterInfo(val viewType: AdapterItemType,
                                val layoutId: Int,
                                val viewHolderClazz: Class<*>)
}