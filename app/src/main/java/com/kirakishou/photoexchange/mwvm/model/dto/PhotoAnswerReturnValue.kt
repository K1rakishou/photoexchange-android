package com.kirakishou.photoexchange.mwvm.model.dto

import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 11/13/2017.
 */
class PhotoAnswerReturnValue(
        val photoAnswer: PhotoAnswer,
        val allFound: Boolean
) {
}