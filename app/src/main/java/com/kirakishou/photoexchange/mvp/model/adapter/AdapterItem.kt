package com.kirakishou.photoexchange.mvp.model.adapter

/**
 * Created by kirakishou on 11/7/2017.
 */
open class AdapterItem<Value> {
    private var type: Int = -1
    var value: Value? = null

    constructor(value: Value, type: AdapterItemType) {
        this.type = type.ordinal
        this.value = value
    }

    constructor(type: AdapterItemType) {
        this.type = type.ordinal
    }

    fun setType(type: AdapterItemType) {
        this.type = type.ordinal
    }

    fun getType(): Int = type
}