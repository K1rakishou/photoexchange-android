package com.kirakishou.photoexchange.helper.preference

import android.content.SharedPreferences
import com.kirakishou.fixmypc.fixmypcapp.helper.extension.edit
import com.kirakishou.photoexchange.mvvm.model.Fickle

/**
 * Created by kirakishou on 11/4/2017.
 */
class UserInfoPreference(private val sharedPreferences: SharedPreferences) : BasePreference {

    private var userId: Fickle<String> = Fickle.empty()

    private val thisPrefPrefix = "AccountInfoPreference"
    private val userIdSharedPrefKey = "${thisPrefPrefix}_user_id"

    fun getUserId(): String {
        return userId.get()
    }

    fun setUserId(newUserId: String) {
        userId = Fickle.of(newUserId)
    }

    override fun save() {
        sharedPreferences.edit {
            if (userId.isPresent()) {
                it.putString(userIdSharedPrefKey, userId.get())
            }

            it.apply()
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

    fun exists(): Boolean {
        return userId.isPresent()
    }
}