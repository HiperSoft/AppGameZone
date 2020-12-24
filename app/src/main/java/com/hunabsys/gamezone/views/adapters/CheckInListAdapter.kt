package com.hunabsys.gamezone.views.adapters

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.CheckInDao
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.datamodels.CheckIn
import io.realm.RealmResults
import java.text.SimpleDateFormat
import java.util.*

class CheckInListAdapter(private val context: Context,
                         private val values: RealmResults<CheckIn>) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): CheckIn? {
        return values[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    override fun getViewTypeCount(): Int {
        return count
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
        val view: View?
        val viewHolder: CheckInRowHolder
        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_check_in, parent, false)
            viewHolder = CheckInRowHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as CheckInRowHolder
        }

        val checkIn = getItem(position)
        setViews(viewHolder, checkIn)

        if (checkIn != null) {
            view?.setOnClickListener {
                showStatus(view, checkIn.isSynchronized)
            }
        }
        return view
    }

    private fun showStatus(view: View, isSynchronized: Boolean) {
        val snack: Snackbar

        if (isSynchronized) {
            snack = Snackbar.make(view,
                    context.getString(R.string.check_in_synced_visit),
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context, snack, UtilHelper.SNACK_TYPE_SUCCESS)
        } else {
            snack = Snackbar.make(view,
                    context.getString(R.string.check_in_some_visits_failed),
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context, snack, UtilHelper.SNACK_TYPE_WARNING)
        }
    }

    private fun setViews(viewHolder: CheckInRowHolder, checkIn: CheckIn?) {
        if (checkIn != null) {
            val pointOfSale = PointOfSaleDao().findById(checkIn.pointOfSaleId)
            val pointOfSaleName = pointOfSale?.name ?: ""

            viewHolder.textPointOfSale.text = pointOfSaleName

            val date = CheckInDao().findCopyById(checkIn.id).createdAt
            val checkInDate = SimpleDateFormat("dd/MMMM/yyyy hh:mm aaa",
                    Locale.getDefault()).format(date)
            viewHolder.textDate.text = checkInDate

            if (checkIn.isSynchronized) {
                viewHolder.imageSynchronized.setImageResource(R.drawable.circle_indicator_green)
            } else {
                viewHolder.imageSynchronized.setImageResource(R.drawable.circle_indicator_red)
            }
        }
    }

    private class CheckInRowHolder(row: View?) {
        val textPointOfSale: TextView =
                row?.findViewById(R.id.item_check_in_text_name) as TextView

        val textDate: TextView =
                row?.findViewById(R.id.item_check_in_text_date) as TextView

        val imageSynchronized: ImageView =
                row?.findViewById(R.id.item_check_in_image_status) as ImageView
    }
}