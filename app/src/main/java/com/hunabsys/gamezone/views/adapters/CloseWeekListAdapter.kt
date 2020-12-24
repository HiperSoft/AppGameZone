package com.hunabsys.gamezone.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.pojos.ItemCloseWeek

/**
 * Adapter for FinishWeek' ListView.
 * Created by Jonathan Hernandez on 04/09/18.
 */
class CloseWeekListAdapter(context: Context,
                           private val values: List<ItemCloseWeek>) :
        BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): ItemCloseWeek {
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
        val viewHolder: CloseWeekRowHolder
        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_close_week, parent, false)
            viewHolder = CloseWeekRowHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as CloseWeekRowHolder
        }

        val closeWeek = getItem(position)
        setViews(viewHolder, closeWeek)

        return view
    }

    private fun setViews(viewHolder: CloseWeekRowHolder, item: ItemCloseWeek?) {
        if (item != null) {
            viewHolder.textLabel.text = item.label
            viewHolder.textValue.text = UtilHelper().formatCurrency(item.value.toDouble())
        }
    }
}

private class CloseWeekRowHolder(row: View?) {
    val textLabel: TextView =
            row?.findViewById(R.id.item_close_week_text_label) as TextView
    val textValue: TextView =
            row?.findViewById(R.id.item_close_week_text_value) as TextView
}