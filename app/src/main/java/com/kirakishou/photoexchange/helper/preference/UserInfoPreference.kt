package com.kirakishou.photoexchange.helper.preference

import android.content.SharedPreferences
import com.kirakishou.fixmypc.fixmypcapp.helper.extension.edit
import com.kirakishou.photoexchange.mvvm.model.Fickle

/**
 * Created by kirakishou on 11/4/2017.
 */
class UserInfoPreference(private val sharedPreferences: SharedPreferences) : BasePreference {

    var userId: Fickle<String> = Fickle.empty()

    private val thisPrefPrefix = "AccountInfoPreference"
    private val userIdSharedPrefKey = "${thisPrefPrefix}_user_id"

    override fun save() {
        sharedPreferences.edit {
            if (userId.isPresent()) {
                it.putString(userIdSharedPrefKey, userId.get())
            }

            it.commit()
        }
    }

    override fun load() {
        userId = Fickle.of(sharedPreferences.getString(userIdSharedPrefKey, null))
    }

    override fun clear() {
        sharedPreferences.edit {
            it.remove(userIdSharedPrefKey)

            userId = Fickle.empty()
        }
    }
}