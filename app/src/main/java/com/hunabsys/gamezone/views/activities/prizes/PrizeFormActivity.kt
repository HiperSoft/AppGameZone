package com.hunabsys.gamezone.views.activities.prizes
/* ktlint-disable no-wildcard-imports */

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.datamodels.GameMachine
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.ScannerActivity
import com.hunabsys.gamezone.views.adapters.PrizeStepperAdapter
import com.hunabsys.gamezone.views.fragments.SimpleFieldFragment
import com.hunabsys.gamezone.views.fragments.TakePhotoFragment
import kotlinx.android.synthetic.main.activity_prize_form.*
import kotlinx.android.synthetic.main.partial_machine_folio_bar.view.*
import kotlinx.android.synthetic.main.partial_stepper.*
import java.util.*
import kotlin.collections.ArrayList

class PrizeFormActivity : BaseActivity(), IPrizeConstants,
        SimpleFieldFragment.OnFragmentInteractionListener,
        TakePhotoFragment.OnFragmentInteractionListener {

    private val tag = PrizeFormActivity::class.java.simpleName

    companion object {

        @SuppressLint("UseSparseArrays")
        private var quantities = HashMap<Int, Int>()
        private var encodedImages = ArrayList<String>(2)

        /**
         * Use this factory method to create a new instance of
         * this activity using the provided parameters.
         *
         * @param context Previous activity context.
         * @param machineFolio String containing route-machine folio.
         * @return A new intent for activity PrizeFormActivity.
         */
        fun getStartIntent(context: Context, machineFolio: String): Intent {
            val intent = Intent(context, PrizeFormActivity::class.java)
            intent.putExtra(ScannerActivity.EXTRA_RESULT_CODE, machineFolio)
            return intent
        }

        fun getQuantities(): HashMap<Int, Int> {
            return quantities
        }

        fun getEncodedImages(): ArrayList<String> {
            return encodedImages
        }

        fun getRealFund(activity: PrizeFormActivity): String {
            val realFund = activity.getGameMachine().realFund
            return UtilHelper().formatCurrency(realFund).toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prize_form)

        encodedImages = ArrayList(2)

        showGameMachineFolio()
        setUpStepper()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IPrizeConstants.REQUEST_CODE_SUMMARY) {
            if (resultCode == IPrizeConstants.RESULT_CODE_SUCCESS) {
                // If the Prize was successfully saved in PrizeSummary, close form
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
        // For SimpleFieldFragment & TakePhotoFragment
        quantities[fragmentNumber] = value
        Log.d(tag, "Quantities: $quantities")
    }

    override fun onFragmentImageInteraction(value: String, fragmentNumber: Int) {
        // For TakePhotoFragment
        encodedImages.add(fragmentNumber - 1, value)
        Log.d(tag, "Images $encodedImages")
    }

    fun getGameMachine(): GameMachine {
        val machineId = getMachineFolio()

        // Discard route folio and separator
        val machineFolio = machineId.substring(machineId.length - 3, machineId.length)

        Log.e(tag, "Game Machine folio: $machineFolio")
        Log.e(tag, "Game Machine ID: $machineId")
        return GameMachineDao().findByFolio(machineFolio)
    }

    fun goToSummary() {
        val machineFolio = getMachineFolio()
        val intent = PrizeSummaryActivity.getStartIntent(this, machineFolio)
        startActivityForResult(intent, IPrizeConstants.REQUEST_CODE_SUMMARY)
        AnimationHelper().enterTransition(this)
    }

    private fun getMachineFolio(): String {
        return this.intent.extras.getString(ScannerActivity.EXTRA_RESULT_CODE)
    }

    private fun showGameMachineFolio() {
        val textView = prize_form_game_machine_bar.game_machine_bar_text_folio
        textView.text = getMachineFolio()
    }

    private fun setUpStepper() {
        stepper_layout.adapter = PrizeStepperAdapter(this, this.supportFragmentManager)
    }

    private fun closeActivity() {
        finish()
        AnimationHelper().exitTransition(this)
    }
}