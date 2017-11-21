package com.kirakishou.photoexchange.mwvm.model.dto

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 11/21/2017.
 */
data class PhotoAnswerAllFound(
        val photoAnswer: PhotoAnswer,
        val allFound: Boolean
)