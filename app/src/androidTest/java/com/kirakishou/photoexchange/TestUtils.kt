package com.kirakishou.photoexchange

import android.view.View
import androidx.annotation.IdRes
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import org.hamcrest.Matcher


object TestUtils {

  fun <T : View> performActionOnViewWithId(@IdRes id: Int): ViewAction {
    return object : ViewAction {
      override fun getConstraints(): Matcher<View>? {
        return null
      }

      override fun getDescription(): String {
        return "Perform custom action on view"
      }

      override fun perform(uiController: androidx.test.espresso.UiController, view: View) {
        click().perform(uiController, view.findViewById<T>(id))
      }
    }
  }

}