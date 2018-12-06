package com.kirakishou.photoexchange.helper.extension

fun <T, K> List<T>.filterDuplicatesWith(otherList: List<T>, keySelector: (T) -> K): List<T> {
  if (otherList.isEmpty()) {
    return this
  }

  if (this.isEmpty()) {
    return otherList
  }

  val set = otherList.map { keySelector(it) }.toSet()
  val resultList = otherList.toMutableList()

  for (element in this) {
    val key = keySelector(element)

    if (!set.contains(key)) {
      resultList += element
    }
  }

  return resultList
}