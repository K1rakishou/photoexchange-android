package com.kirakishou.photoexchange.model

import com.google.gson.annotations.SerializedName

data class TestEntity(@SerializedName("test")
                      val testField: String)