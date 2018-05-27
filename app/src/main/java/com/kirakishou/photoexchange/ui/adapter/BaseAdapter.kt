package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.annotation.CallSuper
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

    private val layoutInflater = LayoutInflater.from(context)
    private val baseAdapterInfo by lazy { doGetBaseAdapterInfo() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        for (adapterInfo in baseAdapterInfo) {
            if (adapterInfo.viewType.ordinal == viewType) {
                val view = layoutInflater.inflate(adapterInfo.layoutId, parent, false)
                return adapterInfo.viewHolderClazz.getDeclaredConstructor(View::class.java).newInstance(view) as RecyclerView.ViewHolder
            }
        }

        throw IllegalStateException("viewType $viewType not found!")
    }

    abstract fun doGetBaseAdapterInfo(): MutableList<BaseAdapterInfo>

    inner class BaseAdapterInfo(val viewType: AdapterItemType,
                                val layoutId: Int,
                                val viewHolderClazz: Class<*>)
}