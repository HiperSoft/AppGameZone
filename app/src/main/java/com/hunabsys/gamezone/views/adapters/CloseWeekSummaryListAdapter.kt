package com.hunabsys.gamezone.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.pojos.ItemCloseWeekSummary

class CloseWeekSummaryListAdapter(context: Context,
                                  private val values: List<ItemCloseWeekSummary>) :
        BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): ItemCloseWeekSummary {
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
        val viewHolder: CloseWeekSummaryRowHolder

        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_close_week_summary, parent, false)
            viewHolder = CloseWeekSummaryRowHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as CloseWeekSummaryRowHolder
        }

        val closeWeek = getItem(position)
        setViews(viewHolder, closeWeek)

        return view
    }

    private fun setViews(viewHolder: CloseWeekSummaryRowHolder, item: ItemCloseWeekSummary?) {
        if (item != null) {

            viewHolder.textLabel.text = item.machine

            if (item.status == "0" || item.status == "1") {
                viewHolder.imageStated.setImageResource(R.mipmap.ic_check)
            } else {
                viewHolder.imageStated.setImageResource(R.mipmap.ic_cancel)
            }
        }
    }
}

private class CloseWeekSummaryRowHolder(row: View?) {
    val textLabel: TextView =
            row?.findViewById(R.id.item_close_week_summary_text_label) as TextView
    val imageStated: ImageView =
            row?.findViewById(R.id.item_close_week_summary_image_status) as ImageView
}