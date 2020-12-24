package com.hunabsys.gamezone.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.pojos.ItemPrizeSummary

/**
 * Adapter for Prize' ListView.
 * Created by Jonathan Hernandez on 26/7/18.
 */
class PrizeSummaryListAdapter(context: Context,
                              private val values: List<ItemPrizeSummary>) :
        BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): ItemPrizeSummary? {
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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        val view: View?
        val viewHolder: PrizeSummaryRowHolder
        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_prize_summary, parent,
                    false)
            viewHolder = PrizeSummaryRowHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as PrizeSummaryRowHolder
        }

        val prize = getItem(position)
        setViews(viewHolder, prize, position)

        return view
    }

    private fun setViews(viewHolder: PrizeSummaryRowHolder,
                         item: ItemPrizeSummary?,
                         position: Int) {
        if (item != null) {
            val quantity = if (position <= 2) {
                String.format("%,d", item.value)
            } else {
                UtilHelper().formatCurrency(item.value.toDouble()).toString()
            }
            viewHolder.textLabel.text = item.label

            viewHolder.textValue.text = quantity
        }
    }
}

private class PrizeSummaryRowHolder(row: View?) {

    val textLabel: TextView =
            row?.findViewById(R.id.item_prize_summary_text_label) as TextView
    val textValue: TextView =
            row?.findViewById(R.id.item_prize_summary_text_value) as TextView
}
