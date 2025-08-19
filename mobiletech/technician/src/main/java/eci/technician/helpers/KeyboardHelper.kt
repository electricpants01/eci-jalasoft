package eci.technician.helpers

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText


object KeyboardHelper {

    fun showKeyboard(mEtSearch: View) {
        mEtSearch.requestFocus()
        val imm: InputMethodManager = mEtSearch.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
    }

    fun hideSoftKeyboard(mEtSearch: View) {
        mEtSearch.clearFocus()
        val imm: InputMethodManager = mEtSearch.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mEtSearch.windowToken, 0)
    }
}