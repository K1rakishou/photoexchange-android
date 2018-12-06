package com.kirakishou.photoexchange.helper.extension

import org.junit.Assert.*
import org.junit.Test

class ListExtensionsKtTest {

  @Test
  fun `should just return second list when first list is empty`() {
    val originalList = emptyList<Int>()
    val checkList = listOf(1, 2, 3, 4)

    val result = originalList.filterDuplicatesWith(checkList) { it }
    assertEquals(4, result.size)

    assertEquals(1, result[0])
    assertEquals(2, result[1])
    assertEquals(3, result[2])
    assertEquals(4, result[3])
  }

  @Test
  fun `should just return first list when second list is empty`() {
    val originalList = listOf(1, 2, 3, 4)
    val checkList = emptyList<Int>()

    val result = originalList.filterDuplicatesWith(checkList) { it }
    assertEquals(4, result.size)

    assertEquals(1, result[0])
    assertEquals(2, result[1])
    assertEquals(3, result[2])
    assertEquals(4, result[3])
  }

  @Test
  fun `should return empty list when all elements have duplicates`() {
    val originalList = listOf(1, 2, 3, 4)
    val checkList = listOf(1, 2, 3, 4)

    val result = originalList.filterDuplicatesWith(checkList) { it }
    assertEquals(4, result.size)

    assertEquals(1, result[0])
    assertEquals(2, result[1])
    assertEquals(3, result[2])
    assertEquals(4, result[3])
  }

  @Test
  fun `should return elements that not exist in original list when first list has unique elements`() {
    val originalList = listOf(1, 2, 3, 4, 5, 6)
    val checkList = listOf(1, 2, 3, 4)

    val result = originalList.filterDuplicatesWith(checkList) { it }
    assertEquals(6, result.size)

    assertEquals(1, result[0])
    assertEquals(2, result[1])
    assertEquals(3, result[2])
    assertEquals(4, result[3])
    assertEquals(5, result[4])
    assertEquals(6, result[5])
  }

  @Test
  fun `should return elements that not exist in original list when second list has unique elements`() {
    val originalList = listOf(1, 2, 3, 4)
    val checkList = listOf(1, 2, 3, 4, 5, 6)

    val result = originalList.filterDuplicatesWith(checkList) { it }
    assertEquals(6, result.size)

    assertEquals(1, result[0])
    assertEquals(2, result[1])
    assertEquals(3, result[2])
    assertEquals(4, result[3])
    assertEquals(5, result[4])
    assertEquals(6, result[5])
  }

  @Test
  fun `should concatenate both list when they have no duplicate elements`() {
    val originalList = listOf(1, 2, 3, 4)
    val checkList = listOf(5, 6, 7, 8)

    val result = originalList.filterDuplicatesWith(checkList) { it }
    assertEquals(8, result.size)

    assertEquals(5, result[0])
    assertEquals(6, result[1])
    assertEquals(7, result[2])
    assertEquals(8, result[3])
    assertEquals(1, result[4])
    assertEquals(2, result[5])
    assertEquals(3, result[6])
    assertEquals(4, result[7])
  }
}