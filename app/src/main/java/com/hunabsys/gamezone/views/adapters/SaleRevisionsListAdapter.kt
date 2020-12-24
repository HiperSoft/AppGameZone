package com.hunabsys.gamezone.views.adapters

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.graphics.Typeface
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.models.daos.SaleRevisionDao
import com.hunabsys.gamezone.models.datamodels.SaleRevision
import io.realm.RealmResults
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for SaleRevisions' ListView.
 * Created by Silvia Valdez on 1/30/18.
 */
private const val MAX_DIFFERENCE = -20

class SaleRevisionsListAdapter(private val context: Context,
                               private val values: RealmResults<SaleRevision>) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): SaleRevision? {
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
        val viewHolder: SaleRevisionsRowHolder
        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_sale_revision, parent, false)
            viewHolder = SaleRevisionsRowHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as SaleRevisionsRowHolder
        }

        val saleRevision = getItem(position)
        setViews(viewHolder, saleRevision)

        if (saleRevision != null) {
            view?.setOnClickListener {
                showStatus(view, saleRevision.hasSynchronizedData)
            }
        }

        return view
    }

    private fun highlightTypeface(textView: TextView) {
        val boldTypeface = Typeface.create(textView.typeface, Typeface.BOLD)
        textView.typeface = boldTypeface
        textView.setTextColor(ContextCompat.getColor(context, R.color.black_pearl))
    }

    private fun showStatus(view: View, isSynchronized: Boolean) {
        val snack: Snackbar

        if (isSynchronized) {
            snack = Snackbar.make(view,
                    context.getString(R.string.sale_revision_synced),
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context, snack, UtilHelper.SNACK_TYPE_SUCCESS)
        } else {
            snack = Snackbar.make(view,
                    context.getString(R.string.sale_revisions_some_failed),
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context, snack, UtilHelper.SNACK_TYPE_WARNING)
        }
    }

    private fun calculateProfit(gameMachineId: Long, entry: Int, outcome: Int): Double {
        val lastEntry = GameMachineDao().findById(gameMachineId).lastEntry
        val lastOutcome = GameMachineDao().findById(gameMachineId).lastOutcome

        return ((entry - lastEntry) - (outcome - lastOutcome))
    }

    private fun calculateDifference(calculated: Double, gameMachineOutcome: Double): Double {
        return gameMachineOutcome - calculated
    }

    private fun setViews(viewHolder: SaleRevisionsRowHolder, saleRevision: SaleRevision?) {
        if (saleRevision != null) {
            val routeCode = RouteDao().findAll().first()?.code
            val gameMachineFolio = GameMachineDao().findById(saleRevision.gameMachineId).folio

            val gameMachineId = "$routeCode-$gameMachineFolio"
            viewHolder.textGameMachine.text = gameMachineId

            val pointOfSale = PointOfSaleDao().findById(saleRevision.pointOfSaleId)
            val pointOfSaleName = pointOfSale?.name ?: ""

            viewHolder.textPointOfSale.text = pointOfSaleName

            viewHolder.textReal.text = UtilHelper().formatCurrency(saleRevision.gameMachineOutcome)

            val calculated = calculateProfit(saleRevision.gameMachineId, saleRevision.entry,
                    saleRevision.outcome)
            viewHolder.textCalculated.text = UtilHelper().formatCurrency(calculated)

            val difference = calculateDifference(calculated, saleRevision.gameMachineOutcome)
            viewHolder.textDifference.text = UtilHelper().formatCurrency(difference)

            val date = SaleRevisionDao().findCopyById(saleRevision.id).createdAt
            val revisionDate = SimpleDateFormat("dd/MMM/yyy",
                    Locale.getDefault()).format(date)
            viewHolder.textDate.text = revisionDate

            if (saleRevision.hasSynchronizedData) {
                viewHolder.imageSynchronized.setImageResource(R.drawable.circle_indicator_green)
            } else {
                viewHolder.imageSynchronized.setImageResource(R.drawable.circle_indicator_red)
            }

            if (difference < MAX_DIFFERENCE) {
                highlightTypeface(viewHolder.textCalculated)
                highlightTypeface(viewHolder.textReal)
                highlightTypeface(viewHolder.textDifference)
            }
        }
    }
}

private class SaleRevisionsRowHolder(row: View?) {
    val textGameMachine: TextView =
            row?.findViewById(R.id.item_sale_revisions_text_machine) as TextView
    val textPointOfSale: TextView =
            row?.findViewById(R.id.item_sale_revisions_text_point) as TextView
    val textCalculated: TextView =
            row?.findViewById(R.id.item_sale_revisions_text_calculated_value) as TextView
    val textReal: TextView =
            row?.findViewById(R.id.item_sale_revisions_text_real_value) as TextView
    val textDifference: TextView =
            row?.findViewById(R.id.item_sale_revisions_text_difference_value) as TextView

    val textDate: TextView = row?.findViewById(R.id.item_sale_revisions_text_date) as TextView

    val imageSynchronized: ImageView =
            row?.findViewById(R.id.item_sale_revisions_image_status) as ImageView
}
