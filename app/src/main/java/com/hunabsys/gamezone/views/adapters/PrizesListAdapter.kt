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
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.daos.PrizeDao
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.models.datamodels.Prize
import io.realm.RealmResults
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for Prizes' ListView.
 * Created by Jonathan Hernandez on 24/7/2018.
 */

class PrizesListAdapter(private val context: Context,
                        private val values: RealmResults<Prize>) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return values.size
    }

    override fun getItem(position: Int): Prize? {
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
        val viewHolder: PrizesRowHolder
        if (convertView == null) {
            view = this.inflater.inflate(R.layout.item_prize, parent, false)
            viewHolder = PrizesRowHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as PrizesRowHolder
        }

        val prize = getItem(position)
        setViews(viewHolder, prize)

        if (prize != null) {
            view?.setOnClickListener {
                showStatus(view, prize.hasSynchronizedData)
            }
        }

        return view
    }

    private fun showStatus(view: View, isSynchronized: Boolean) {
        val snack: Snackbar

        if (isSynchronized) {
            snack = Snackbar.make(view, context.getString(R.string.prize_synced),
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context, snack, UtilHelper.SNACK_TYPE_SUCCESS)
        } else {
            snack = Snackbar.make(view, context.getString(R.string.prizes_some_failed),
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context, snack, UtilHelper.SNACK_TYPE_WARNING)
        }
    }

    private fun setViews(viewHolder: PrizesRowHolder, prize: Prize?) {
        if (prize != null) {
            val routeCode = RouteDao().findAll().first()?.code
            val gameMachineFolio = GameMachineDao().findById(prize.gameMachineId).folio

            val gameMachineId = "$routeCode-$gameMachineFolio"
            viewHolder.textGameMachine.text = gameMachineId

            val pointOfSale = PointOfSaleDao().findById(prize.pointOfSaleId)
            val pointOfSaleName = pointOfSale?.name ?: ""

            viewHolder.textPointOfSale.text = pointOfSaleName

            val date = PrizeDao().findCopyById(prize.id).createdAt
            val prizeDate = SimpleDateFormat("dd/MMM/yyy",
                    Locale.getDefault()).format(date)
            viewHolder.textDate.text = prizeDate

            val prizeAmount = PrizeDao().findCopyById(prize.id).prizeAmount
            viewHolder.textAmount.text = UtilHelper().formatCurrency(prizeAmount)

            if (prize.hasSynchronizedData) {
                viewHolder.imageSynchronized.setImageResource(R.drawable.circle_indicator_green)
            } else {
                viewHolder.imageSynchronized.setImageResource(R.drawable.circle_indicator_red)
            }
        }
    }
}

private class PrizesRowHolder(row: View?) {
    val textGameMachine: TextView = row?.findViewById(R.id.item_prize_text_route) as TextView
    val textPointOfSale: TextView = row?.findViewById(R.id.item_prize_text_point) as TextView
    val textDate: TextView = row?.findViewById(R.id.item_prize_text_date) as TextView
    val textAmount: TextView = row?.findViewById(R.id.item_prize_text_amount_value) as TextView
    val imageSynchronized: ImageView = row?.findViewById(R.id.item_prize_image_status) as ImageView
}