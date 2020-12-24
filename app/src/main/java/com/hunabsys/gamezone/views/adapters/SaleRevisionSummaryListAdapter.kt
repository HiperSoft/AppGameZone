package com.hunabsys.gamezone.views.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.pojos.ItemSaleRevisionSummary

/**
 * Adapter for SaleRevisions' ListView.
 * Created by Silvia Valdez on 3/1/18.
 */
class SaleRevisionSummaryListAdapter(context: Context,
                                     private val values: List<ItemSaleRevisionSummary>) :
        BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): ItemSaleRevisionSummary? {
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
        val viewHolder: SaleRevisionSummaryRowHolder
        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_sale_revision_summary, parent,
                    false)
            viewHolder = SaleRevisionSummaryRowHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as SaleRevisionSummaryRowHolder
        }

        val saleRevision = getItem(position)
        setViews(viewHolder, saleRevision, position)

        return view
    }

    private fun setViews(viewHolder: SaleRevisionSummaryRowHolder,
                         item: ItemSaleRevisionSummary?,
                         position: Int) {
        if (item != null) {
            var quantity: String
            viewHolder.textLabel.text = item.label

            if (position <= 2) {
                quantity = String.format("%,d", item.value)
            } else {
                quantity = UtilHelper().formatCurrency(item.value.toDouble()).toString()
            }
            viewHolder.textValue.text = quantity
        }
    }
}

private class SaleRevisionSummaryRowHolder(row: View?) {

    val textLabel: TextView =
            row?.findViewById(R.id.item_sale_revision_summary_text_label) as TextView
    val textValue: TextView =
            row?.findViewById(R.id.item_sale_revision_summary_text_value) as TextView
}
