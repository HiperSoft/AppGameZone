package com.hunabsys.gamezone.views.activities.expenses

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.ScannerActivity
import com.hunabsys.gamezone.views.adapters.ExpenseStepperAdapter
import com.hunabsys.gamezone.views.fragments.SimpleFieldFragment
import com.hunabsys.gamezone.views.fragments.SimpleTextFragment
import kotlinx.android.synthetic.main.activity_expense_form.*
import kotlinx.android.synthetic.main.partial_machine_folio_bar.view.*
import kotlinx.android.synthetic.main.partial_stepper.*

class ExpenseFormActivity : BaseActivity(), IExpenseConstants,
        SimpleTextFragment.OnFragmentInteractionListener,
        SimpleFieldFragment.OnFragmentInteractionListener {

    private val tag = ExpenseFormActivity::class.java.simpleName

    companion object {
        private var concept: String = ""
        private var amount: Int = 0
        private var pointOfSaleFolio = ""

        fun getStartIntent(context: Context, routeFolio: String, pointOfSaleFolio: String): Intent {
            val intent = Intent(context, ExpenseFormActivity::class.java)
            intent.putExtra(ScannerActivity.EXTRA_RESULT_CODE, routeFolio)
            intent.putExtra(IExpenseConstants.EXTRA_POS_FOLIO, pointOfSaleFolio)
            return intent
        }

        fun getConcept(): String {
            return concept
        }

        fun getAmount(): Int {
            return amount
        }

        fun getPointOfSaleFolio(): String {
            return pointOfSaleFolio
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_form)

        concept = ""

        getPointOfSale()
        showRouteFolio()
        setUpStepper()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IExpenseConstants.REQUEST_CODE_SUMMARY) {
            if (resultCode == IExpenseConstants.RESULT_CODE_SUCCESS) {
                // If the Expense was successfully saved in ExpenseSummary, close form
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

    override fun onFragmentTextInteraction(value: String) {
        concept = value
        Log.d(tag, "Concept: $concept")
    }

    override fun onFragmentInteraction(value: Int, fragmentNumber: Int) {
        amount = value
        Log.d(tag, "Quantity: $amount")
    }

    fun goToSummary() {
        val routeFolio = getRouteFolio()
        val intent = ExpenseSummaryActivity.getStartIntent(this, routeFolio)
        startActivityForResult(intent, IExpenseConstants.REQUEST_CODE_SUMMARY)
        AnimationHelper().enterTransition(this)
    }

    private fun getPointOfSale(): String {
        pointOfSaleFolio = this.intent.extras.getString(IExpenseConstants.EXTRA_POS_FOLIO)
        return pointOfSaleFolio
    }

    private fun getRouteFolio(): String {
        return this.intent.extras.getString(ScannerActivity.EXTRA_RESULT_CODE)
    }

    private fun showRouteFolio() {
        val textView = expense_form_route_bar.game_machine_bar_text_folio
        textView.text = getRouteFolio()
    }

    private fun setUpStepper() {
        stepper_layout.adapter = ExpenseStepperAdapter(this, this.supportFragmentManager)
    }

    private fun closeActivity() {
        finish()
        AnimationHelper().exitTransition(this)
    }
}
