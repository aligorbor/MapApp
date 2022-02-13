package ru.geekbrains.android2.mapapp.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.showSnackBar(
    text: String,
    actionText: String,
    action: (View) -> Unit,
    length: Int = Snackbar.LENGTH_INDEFINITE
) {
    Snackbar.make(this, text, length).setAction(actionText, action).show()
}

fun View.showSnackBar(
    resIdText: Int,
    resIdActionText: Int,
    action: (View) -> Unit,
    length: Int = Snackbar.LENGTH_INDEFINITE
) {
    this.showSnackBar(
        this.context.getString(resIdText),
        this.context.getString(resIdActionText),
        action,
        length
    )
}

fun View.showSnackBar(
    text: String,
    actionText: String = "Ok",
    length: Int = Snackbar.LENGTH_INDEFINITE
) {
    this.showSnackBar(text, actionText, {}, length)
}

fun View.showSnackBar(
    resIdText: Int,
    actionText: String = "Ok",
    length: Int = Snackbar.LENGTH_INDEFINITE
) {
    this.showSnackBar(this.context.getString(resIdText), actionText, {}, length)
}