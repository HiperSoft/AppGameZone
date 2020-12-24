package com.hunabsys.gamezone.views.activities.salerevisions

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.GpsHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.datamodels.GameMachine
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.ScannerActivity
import com.hunabsys.gamezone.views.activities.salerevisions.ISaleRevisionConstants.Companion.GAME_MACHINE_OUTCOME
import com.hunabsys.gamezone.views.adapters.SaleRevisionStepperAdapter
import com.hunabsys.gamezone.views.fragments.CommissionFragment
import com.hunabsys.gamezone.views.fragments.NegativeFieldFragment
import com.hunabsys.gamezone.views.fragments.SimpleFieldFragment
import com.hunabsys.gamezone.views.fragments.TakePhotoFragment
import kotlinx.android.synthetic.main.activity_sale_revision_form.*
import kotlinx.android.synthetic.main.partial_machine_folio_bar.view.*
import kotlinx.android.synthetic.main.partial_stepper.*
import kotlin.collections.set

class SaleRevisionFormActivity : BaseActivity(), ISaleRevisionConstants,
        CommissionFragment.OnFragmentInteractionListener,
        SimpleFieldFragment.OnFragmentInteractionListener,
        TakePhotoFragment.OnFragmentInteractionListener,
        NegativeFieldFragment.OnFragmentInteractionListener {

    private val tag = SaleRevisionFormActivity::class.java.simpleName

    companion object {

        private var quantities = HashMap<Int, Int>()
        private var imageQuantities = ArrayList<String>(2)

        /**
         * Use this factory method to create a new instance of
         * this activity using the provided parameters.
         *
         * @param context Previous activity context.
         * @param machineFolio String containing route-machine folio.
         * @return A new intent for activity SaleRevisionFormActivity.
         */
        fun getStartIntent(context: Context, machineFolio: String): Intent {
            val intent = Intent(context, SaleRevisionFormActivity::class.java)
            intent.putExtra(ScannerActivity.EXTRA_RESULT_CODE, machineFolio)
            return intent
        }

        fun getQuantities(): HashMap<Int, Int> {
            return quantities
        }

        fun getImageUris(): ArrayList<String> {
            return imageQuantities
        }

        fun getGameMachineOutcome(): String {
            return String.format("%.2f",
                    quantities[GAME_MACHINE_OUTCOME]?.toFloat())
        }

        fun getCommissionPercentage(activity: SaleRevisionFormActivity): String {
            val percentage = activity.getGameMachine().pointsOfSale!!.first()!!.commissionPercentage
            return percentage.toString()
        }

        fun getRealFund(activity: SaleRevisionFormActivity): String {
            val realFund = activity.getGameMachine().realFund
            return UtilHelper().formatCurrency(realFund).toString()
        }

        fun getCurrentLocation(): Location? {
            return GpsHelper.locationGps
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sale_revision_form)

        imageQuantities = ArrayList(2)

        GpsHelper().startLocationService(tag, this)

        showGameMachineFolio()
        setUpStepper()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ISaleRevisionConstants.REQUEST_CODE_SUMMARY) {
            if (resultCode == ISaleRevisionConstants.RESULT_CODE_SUCCESS) {
                // If the SaleRevision was successfully saved in SaleRevisionSummary, close form
                closeActivity()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        UtilHelper().showExitDialog(this)
        return true
    }

    override fun onBackPressed() {
        UtilHelper().showExitDialog(this)
    }

    override fun onFragmentInteraction(value: Int, fragmentNumber: Int) {
        // For NegativeFieldFragment, SimpleFieldFragment & TakePhotoFragment
        quantities[fragmentNumber] = value
        Log.d(tag, "Quantities: $quantities")
    }

    override fun onFragmentInteraction(values: IntArray, fragmentNumber: Int) {
        // For CommissionFragment
        quantities[fragmentNumber] = values[0]
        quantities[ISaleRevisionConstants.COMMISSION_PERCENTAGE] = values[1]
        Log.d(tag, "Quantities: $quantities")
    }

    override fun onFragmentImageInteraction(value: String, fragmentNumber: Int) {
        // For TakePhotoFragment
        imageQuantities.add(fragmentNumber - 1, value)
        Log.d(tag, "Images: $imageQuantities")
    }

    fun getGameMachine(): GameMachine {
        val machineCompleteId = getMachineFolio()

        // Discard route folio and separator
        val machineFolio = machineCompleteId
                .substring(machineCompleteId.length - 3, machineCompleteId.length)

        Log.e(tag, "Game Machine ID: $machineCompleteId")
        Log.e(tag, "Game Machine folio: $machineFolio")
        return GameMachineDao().findByFolio(machineFolio)
    }

    fun goToSummary() {
        val machineFolio = getMachineFolio()
        val intent = SaleRevisionSummaryActivity.getStartIntent(this, machineFolio)
        startActivityForResult(intent, ISaleRevisionConstants.REQUEST_CODE_SUMMARY)
        AnimationHelper().enterTransition(this)
    }

    private fun getMachineFolio(): String {
        return this.intent.extras.getString(ScannerActivity.EXTRA_RESULT_CODE)
    }

    private fun showGameMachineFolio() {
        val textView = sale_revision_form_game_machine_bar.game_machine_bar_text_folio
        textView.text = getMachineFolio()
    }

    private fun setUpStepper() {
        stepper_layout.adapter =
                SaleRevisionStepperAdapter(this, this.supportFragmentManager)
    }

    private fun closeActivity() {
        finish()
        AnimationHelper().exitTransition(this)
    }
}
