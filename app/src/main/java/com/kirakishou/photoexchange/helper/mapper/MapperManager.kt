package com.kirakishou.photoexchange.helper.mapper

/**
 * Created by kirakishou on 9/12/2017.
 */
class MapperManager {
    private val mapperCache = hashMapOf<Class<*>, Mapper>()

    inline fun <reified T : Mapper> get(): T {
        return getMapper(T::class.java)
    }

    @Suppress("UNCHECKED_CAST")
    @PublishedApi
    internal fun <T : Mapper> getMapper(mapperClass: Class<*>): T {
        if (mapperClass.isAssignableFrom(Mapper::class.java)) {
            throw IllegalArgumentException("mapperClass's superclass is not Mapper (${mapperClass::class.java})")
        }

        val mapperFromCache = mapperCache[mapperClass]
        if (mapperFromCache != null) {
            return mapperFromCache as T
        }

        val mapperInstance = mapperClass.getDeclaredConstructor().newInstance() as T
        mapperCache[mapperClass] = mapperInstance

        return mapperInstance
    }
}