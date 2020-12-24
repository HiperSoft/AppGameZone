package com.hunabsys.gamezone.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.pojos.ItemExpenseSummary

/**
 * Adapter for Expense' ListView.
 * Created by Jonathan Hernandez on 13/08/18.
 */
class ExpenseSummaryListAdapter(context: Context,
                                private val values: List<ItemExpenseSummary>) :
        BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): ItemExpenseSummary? {
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
        val viewHolder: ExpenseSummaryRowHolder
        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_expense_summary, parent, false)
            viewHolder = ExpenseSummaryRowHolder(view)
        } else {
            view = convertView
            viewHolder = view.tag as ExpenseSummaryRowHolder
        }

        val expense = getItem(position)
        setViews(viewHolder, expense, position)
        return view
    }

    private fun setViews(viewHolder: ExpenseSummaryRowHolder, item: ItemExpenseSummary?,
                         position: Int) {
        if (item != null) {
            viewHolder.textLabel.text = item.label
            if (position == 2) {
                viewHolder.textValue.text = UtilHelper().formatCurrency(item.value.toDouble())
            } else {
                viewHolder.textValue.text = item.value
            }
        }
    }
}

private class ExpenseSummaryRowHolder(row: View?) {
    val textLabel: TextView =
            row?.findViewById(R.id.item_expense_summary_text_label) as TextView
    val textValue: TextView =
            row?.findViewById(R.id.item_expense_summary_text_value) as TextView
}