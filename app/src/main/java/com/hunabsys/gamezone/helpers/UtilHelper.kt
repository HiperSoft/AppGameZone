package com.hunabsys.gamezone.helpers

import android.content.Context
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.rollbar.android.Rollbar
import java.text.NumberFormat

const val SNACK_COLOR_SUCCESS = R.color.spring_green
const val SNACK_COLOR_WARNING = R.color.supernova
const val SNACK_COLOR_ERROR = R.color.bittersweet
const val SNACK_COLOR_DEFAULT = R.color.charcoal_grey

/**
 * Utility helper class.
 * Created by Silvia Valdez on 1/25/18.
 */
class UtilHelper {

    private var alertDialog: AlertDialog? = null

    companion object {
        const val SNACK_TYPE_SUCCESS = 1
        const val SNACK_TYPE_WARNING = 2
        const val SNACK_TYPE_ERROR = 3
        const val SNACK_TYPE_DEFAULT = 4
    }

    fun formatCurrency(currencyValue: Double): String? {
        val currencyFormat = NumberFormat.getCurrencyInstance()
        currencyFormat.maximumFractionDigits = 0
        return currencyFormat.format(currencyValue)
    }

    fun formatCurrencyToInt(value: String): Int {
        val currency = value
                .replace("$", "")
                .replace(",", "")
                .toDouble()
        return Math.round(currency).toInt()
    }

    fun truncateCents(editText: EditText?) {
        var value = editText?.text.toString()
        val length = value.length
        val positions = 2

        // Check whether or not the string contains at least four characters
        if (length > positions) {
            value = value.substring(0, length - positions) + "00"
        }

        editText?.setText(value)
    }

    fun showView(view: View, show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        view.visibility = visibility
    }

    fun showSnackBar(context: Context, snack: Snackbar, type: Int) {
        val color = when (type) {
            SNACK_TYPE_SUCCESS -> SNACK_COLOR_SUCCESS
            SNACK_TYPE_WARNING -> SNACK_COLOR_WARNING
            SNACK_TYPE_ERROR -> SNACK_COLOR_ERROR
            SNACK_TYPE_DEFAULT -> SNACK_COLOR_DEFAULT

            else -> SNACK_COLOR_DEFAULT
        }
        snack.view.setBackgroundColor(context.getColor(color))
        snack.show()
    }

    fun showExitDialog(activity: AppCompatActivity) {
        val title = activity.getString(R.string.action_exit)
        val message = activity.getString(R.string.message_unsaved_changes)
        UtilHelper().showExitAlert(activity, title, message)
    }

    private fun showExitAlert(activity: AppCompatActivity, title: String, message: String) {
        if (alertDialog != null) {
            alertDialog?.cancel()
        }

        alertDialog = AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.action_exit) { dialog, which ->
                    activity.finish()
                    AnimationHelper().exitTransition(activity)
                }
                .setNegativeButton(R.string.action_cancel) { dialog, which -> dialog.dismiss() }.show()
    }

    fun showStorageAvailableAlert(activity: AppCompatActivity) {
        if (alertDialog != null) {
            alertDialog?.cancel()
        }

        alertDialog = AlertDialog.Builder(activity)
                .setMessage(R.string.error_storage_available)
                .setNegativeButton(R.string.action_cancel) { dialog, which -> dialog.dismiss() }.show()
    }

    fun searchError(ex: Exception) {
        when {
            ex.toString().contains("Index 0 out of range [0..0)") ->
                HttpClientService.userRoutesUnsigned = true
            else -> Rollbar.instance().error(ex)
        }
    }

    fun showCorruptInformationAlert(activity: AppCompatActivity) {
        if (alertDialog != null) {
            alertDialog?.cancel()
        }

        val message = activity.getString(R.string.error_corrupt_information_message)

        alertDialog = AlertDialog.Builder(activity)
                .setMessage(message)
                .setPositiveButton(R.string.action_exit) { dialog, which ->
                    activity.finish()
                    AnimationHelper().exitTransition(activity)
                }.show()
    }
}