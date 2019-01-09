package com.kirakishou.photoexchange

import android.view.View
import androidx.annotation.IdRes
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description


object TestUtils {

  fun <T : View> performActionOnViewWithId(@IdRes id: Int, action: (T) -> Unit): ViewAction {
    return object : ViewAction {
      override fun getConstraints(): Matcher<View>? {
        return null
      }

      override fun getDescription(): String {
        return "Perform custom action on view"
      }

      override fun perform(uiController: androidx.test.espresso.UiController, view: View) {
        action(view.findViewById(id))
      }
    }
  }

  fun withViewAtPosition(position: Int, itemMatcher: Matcher<View>): Matcher<View> {
    return object : BoundedMatcher<View, RecyclerView>(RecyclerView::class.java) {
      override fun describeTo(description: Description?) {
        itemMatcher.describeTo(description)
      }

      override fun matchesSafely(recyclerView: RecyclerView): Boolean {
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
        return viewHolder != null && itemMatcher.matches(viewHolder.itemView)
      }
    }
  }

}