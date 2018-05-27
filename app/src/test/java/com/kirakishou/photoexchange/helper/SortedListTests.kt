package com.kirakishou.photoexchange.helper

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class SortedListTests {
    class Item(val id: Long) {
        override fun toString(): String {
            return "$id"
        }
    }

    @Test
    fun testInsertInSortedOrder() {
        val sortedList = SortedList<Item>({ it.id }, true)

        sortedList.add(Item(0))
        sortedList.add(Item(1))
        sortedList.add(Item(2))
        sortedList.add(Item(3))
        sortedList.add(Item(4))
        sortedList.add(Item(5))
        sortedList.add(Item(6))
        sortedList.add(Item(7))
        sortedList.add(Item(8))
        sortedList.add(Item(9))
        sortedList.add(Item(10))

        assertEquals(0, sortedList[0].id)
        assertEquals(1, sortedList[1].id)
        assertEquals(2, sortedList[2].id)
        assertEquals(3, sortedList[3].id)
        assertEquals(4, sortedList[4].id)
        assertEquals(5, sortedList[5].id)
        assertEquals(6, sortedList[6].id)
        assertEquals(7, sortedList[7].id)
        assertEquals(8, sortedList[8].id)
        assertEquals(9, sortedList[9].id)
        assertEquals(10, sortedList[10].id)
    }

    @Test
    fun testInsertInSortedOrderWithDuplicates() {
        val sortedList = SortedList<Item>({ it.id }, true)

        sortedList.add(Item(0))
        sortedList.add(Item(1))
        sortedList.add(Item(1))
        sortedList.add(Item(2))
        sortedList.add(Item(2))
        sortedList.add(Item(3))
        sortedList.add(Item(4))
        sortedList.add(Item(4))
        sortedList.add(Item(4))
        sortedList.add(Item(5))

        assertEquals(0, sortedList[0].id)
        assertEquals(1, sortedList[1].id)
        assertEquals(1, sortedList[2].id)
        assertEquals(2, sortedList[3].id)
        assertEquals(2, sortedList[4].id)
        assertEquals(3, sortedList[5].id)
        assertEquals(4, sortedList[6].id)
        assertEquals(4, sortedList[7].id)
        assertEquals(4, sortedList[8].id)
        assertEquals(5, sortedList[9].id)
    }

    @Test
    fun testInsertInSortedOrderReversed() {
        val sortedList = SortedList<Item>({ it.id }, true)

        sortedList.add(Item(10))
        sortedList.add(Item(9))
        sortedList.add(Item(8))
        sortedList.add(Item(7))
        sortedList.add(Item(6))
        sortedList.add(Item(5))
        sortedList.add(Item(4))
        sortedList.add(Item(3))
        sortedList.add(Item(2))
        sortedList.add(Item(1))
        sortedList.add(Item(0))

        assertEquals(0, sortedList[0].id)
        assertEquals(1, sortedList[1].id)
        assertEquals(2, sortedList[2].id)
        assertEquals(3, sortedList[3].id)
        assertEquals(4, sortedList[4].id)
        assertEquals(5, sortedList[5].id)
        assertEquals(6, sortedList[6].id)
        assertEquals(7, sortedList[7].id)
        assertEquals(8, sortedList[8].id)
        assertEquals(9, sortedList[9].id)
        assertEquals(10, sortedList[10].id)
    }

    @Test
    fun testInsertRandomOrder() {
        val sortedList = SortedList<Item>({ it.id }, true)

        sortedList.add(Item(155))
        sortedList.add(Item(12))
        sortedList.add(Item(-512))
        sortedList.add(Item(0))
        sortedList.add(Item(0))
        sortedList.add(Item(-1))
        sortedList.add(Item(1))
        sortedList.add(Item(-1))
        sortedList.add(Item(-1))
        sortedList.add(Item(345))
        sortedList.add(Item(5))
        sortedList.add(Item(-5))

        assertEquals(-512, sortedList[0].id)
        assertEquals(-5, sortedList[1].id)
        assertEquals(-1, sortedList[2].id)
        assertEquals(-1, sortedList[3].id)
        assertEquals(-1, sortedList[4].id)
        assertEquals(0, sortedList[5].id)
        assertEquals(0, sortedList[6].id)
        assertEquals(1, sortedList[7].id)
        assertEquals(5, sortedList[8].id)
        assertEquals(12, sortedList[9].id)
        assertEquals(155, sortedList[10].id)
        assertEquals(345, sortedList[11].id)
    }

    @Test
    fun testInsertRandomValues() {
        val sortedList = SortedList<Item>({ it.id }, true)
        val random = Random()

        repeat(1000) {
            val value = random.nextLong()
            sortedList.add(Item(value))
        }

        for (i in 1 until sortedList.size() - 1) {
            if (sortedList[i - 1].id > sortedList[i].id) {
                throw IllegalStateException("Bad order")
            }
        }
    }

    @Test
    fun testRemoveElements() {
        val sortedList = SortedList<Item>({ it.id }, true)

        sortedList.add(Item(0))
        sortedList.add(Item(1))
        sortedList.add(Item(2))
        sortedList.add(Item(3))

        assertEquals(true, sortedList.remove(Item(2)))
        assertEquals(true, sortedList.remove(Item(3)))
        assertEquals(true, sortedList.remove(Item(0)))
        assertEquals(true, sortedList.remove(Item(1)))

        assertEquals(0, sortedList.size())
    }

    @Test
    fun testRemoveNonExistentElement() {
        val sortedList = SortedList<Item>({ it.id }, true)

        sortedList.add(Item(0))
        sortedList.add(Item(1))
        sortedList.add(Item(2))
        sortedList.add(Item(3))

        assertEquals(false, sortedList.remove(Item(5)))
        assertEquals(false, sortedList.remove(Item(2345)))
        assertEquals(false, sortedList.remove(Item(-23245)))
    }

    @Test
    fun testInsertRemoveRandomElements() {
        val elementsCount = 1000
        val sortedList = SortedList<Item>({ it.id }, true)
        val array = arrayOfNulls<Long>(elementsCount)
        val random = Random()

        for (i in 0 until elementsCount) {
            array[i] = random.nextLong()
            sortedList.add(Item(array[i]!!))
        }

        for (element in array) {
            assertEquals(true, sortedList.remove(Item(element!!)))
        }

        assertEquals(0, sortedList.size())
    }
}