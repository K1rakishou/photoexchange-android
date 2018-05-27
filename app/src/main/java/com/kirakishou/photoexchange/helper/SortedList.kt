package com.kirakishou.photoexchange.helper

class SortedList<T>(
    private val idSelectorFunction: (element: T) -> Long,
    private val isDescendingOrder: Boolean = true
) {
    private val innerList = mutableListOf<T>()

    private fun tryGetIndex(index: Int, elementId: Long): Int {
        val elementToLeft = innerList.getOrNull(index - 1)
        val elementToRight = innerList.getOrNull(index + 1)
        val currentElementId = idSelectorFunction(innerList[index])

        if (elementToLeft == null && elementToRight == null) {
            val insertToRight = elementId > currentElementId
            return if (insertToRight) {
                index + 1
            } else {
                0
            }
        }

        if (elementToLeft != null && elementToRight == null) {
            val elementToLeftId = idSelectorFunction(elementToLeft)
            if (elementId > currentElementId) {
                if (elementId > elementToLeftId) {
                    return index + 1
                }
            } else {
                return index
            }
        }

        if (elementToLeft == null && elementToRight != null) {
            val elementToRightId = idSelectorFunction(elementToRight)
            if (elementId < currentElementId) {
                return index
            } else {
                if (elementId < elementToRightId) {
                    return index + 1
                }
            }
        }

        if (elementToLeft != null && elementToRight != null) {
            val elementToLeftId = idSelectorFunction(elementToLeft)
            val elementToRightId = idSelectorFunction(elementToRight)

            if (elementId > currentElementId) {
                if (elementId < elementToRightId) {
                    return index + 1
                }
            } else if (elementId < currentElementId) {
                if (elementId > elementToLeftId) {
                    return index
                }
            }
        }

        return -1
    }

    private tailrec fun binarySearchElementIndexForInsertion(elementId: Long, indexLow: Int, indexHigh: Int): Int {
        val index = (indexLow + indexHigh) / 2
        if (index < 0 || index > innerList.size) {
            throw IllegalStateException("Wut?")
        }

        val newIndex = tryGetIndex(index, elementId)
        if (newIndex != -1) {
            return newIndex
        }

        val currentElementId = idSelectorFunction(innerList[index])
        if (currentElementId == elementId) {
            return if (isDescendingOrder) index else index + 1
        }

        return when {
            elementId > currentElementId -> binarySearchElementIndexForInsertion(elementId, index + 1, indexHigh)
            elementId < currentElementId -> binarySearchElementIndexForInsertion(elementId, indexLow, index - 1)
            else -> throw IllegalStateException("Should never happen")
        }
    }

    private tailrec fun binarySearchElementIndex(elementId: Long, indexLow: Int, indexHigh: Int): Int {
        val index = (indexLow + indexHigh) / 2
        if (index < 0 || index > innerList.size) {
            throw IllegalStateException("Wut?")
        }

        val currentElementId = idSelectorFunction(innerList[index])
        if (currentElementId == elementId) {
            return index
        }

        if (indexLow >= indexHigh) {
            return -1
        }

        return when {
            elementId > currentElementId -> binarySearchElementIndex(elementId, index + 1, indexHigh)
            elementId < currentElementId -> binarySearchElementIndex(elementId, indexLow, index - 1)
            else -> throw IllegalStateException("Should never happen")
        }
    }

    fun size(): Int {
        return innerList.size
    }

    fun clear() {
        innerList.clear()
    }

    operator fun get(index: Int): T {
        return innerList[index]
    }

    fun getOrNull(index: Int): T? {
        return innerList.getOrNull(index)
    }

    fun add(element: T): Int {
        val elementId = idSelectorFunction(element)

        if (innerList.isEmpty()) {
            innerList.add(element)
            return 0
        }

        val index = binarySearchElementIndexForInsertion(elementId, 0, innerList.lastIndex)
        innerList.add(index, element)

        return index
    }

    fun remove(element: T): Boolean {
        val elementId = idSelectorFunction(element)
        val elementIndex = binarySearchElementIndex(elementId, 0, innerList.lastIndex)
        val foundElement = innerList.getOrNull(elementIndex)

        if (foundElement != null && idSelectorFunction(foundElement) == elementId) {
            return removeAt(elementIndex)
        }

        return false
    }

    fun removeAt(index: Int): Boolean {
        return innerList.removeAt(index) != null
    }
}