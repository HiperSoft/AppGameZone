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
import com.hunabsys.gamezone.models.daos.ExpenseDao
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.models.datamodels.Expense
import io.realm.RealmResults
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for Expenses' Listview.
 * Created by Jonathan Hernandez on 06/08/2018.
 */
class ExpensesListAdapter(private val context: Context,
                          private val values: RealmResults<Expense>) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): Expense? {
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
        val viewHolder: ExpensesRowHolder
        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_expense, parent, false)
            viewHolder = ExpensesRowHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ExpensesRowHolder
        }

        val expense = getItem(position)
        setViews(viewHolder, expense)

        if (expense != null) {
            view?.setOnClickListener {
                showStatus(view, expense.isSynchronized)
            }
        }
        return view
    }

    private fun showStatus(view: View, isSynchronized: Boolean) {
        val snack: Snackbar

        if (isSynchronized) {
            snack = Snackbar.make(view, context.getString(R.string.expense_synced),
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context, snack, UtilHelper.SNACK_TYPE_SUCCESS)
        } else {
            snack = Snackbar.make(view, context.getString(R.string.expenses_some_failed),
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context, snack, UtilHelper.SNACK_TYPE_WARNING)
        }
    }

    private fun setViews(viewHolder: ExpensesRowHolder, expense: Expense?) {
        if (expense != null) {
            val routeCode = RouteDao().findAll().first()?.code
            viewHolder.textRoute.text = routeCode

            val pointOfSale = PointOfSaleDao().findById(expense.pointOfSaleId)
            val pointOfSaleName = pointOfSale?.name ?: ""

            viewHolder.textPointOfSale.text = pointOfSaleName

            val date = ExpenseDao().findCopyById(expense.id).createdAt
            val expenseDate = SimpleDateFormat("dd/MMM/yyy",
                    Locale.getDefault()).format(date)
            viewHolder.textDate.text = expenseDate

            val expenseConcept = ExpenseDao().findCopyById(expense.id).concept
            viewHolder.textConcept.text = expenseConcept

            val expenseAmount = ExpenseDao().findCopyById(expense.id).amount
            viewHolder.textAmount.text = UtilHelper().formatCurrency(expenseAmount)

            if (expense.isSynchronized) {
                viewHolder.imageSynchronized.setImageResource(R.drawable.circle_indicator_green)
            } else {
                viewHolder.imageSynchronized.setImageResource(R.drawable.circle_indicator_red)
            }
        }
    }
}

private class ExpensesRowHolder(row: View?) {
    val textRoute: TextView = row?.findViewById(R.id.item_expense_text_route) as TextView
    val textPointOfSale: TextView = row?.findViewById(R.id.item_expense_text_point) as TextView
    val textDate: TextView = row?.findViewById(R.id.item_expense_text_date) as TextView
    val textConcept: TextView = row?.findViewById(R.id.item_expense_text_concept_value) as TextView
    val textAmount: TextView = row?.findViewById(R.id.item_expense_text_amount_value) as TextView
    val imageSynchronized: ImageView = row?.findViewById(R.id.item_expense_image_status)
            as ImageView
}