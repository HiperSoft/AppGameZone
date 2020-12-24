package com.hunabsys.gamezone.views.fragments

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.views.activities.expenses.ExpenseFormActivity
import com.hunabsys.gamezone.views.activities.prizes.PrizeFormActivity
import com.hunabsys.gamezone.views.activities.salerevisions.SaleRevisionFormActivity
import com.rollbar.android.Rollbar
import com.stepstone.stepper.BlockingStep
import com.stepstone.stepper.StepperLayout
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.fragment_simple_field.view.*
import java.text.NumberFormat
import java.util.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SimpleFieldFragment.OnFragmentInteractionListener] interface to handle interaction events.
 * Use the [SimpleFieldFragment.newInstance] factory method to create an instance of this fragment.
 */
class SimpleFieldFragment : Fragment(), BlockingStep {

    private val classTag = SimpleFieldFragment::class.java.simpleName

    private var listener: OnFragmentInteractionListener? = null

    private var layoutView: View? = null
    private var editValue: EditText? = null

    private var title: String? = null
    private var showBodyText: Boolean = false
    private var currencyField: Boolean = false
    private var fragmentNumber: Int = -1
    private var lastValue: String = ""

    companion object {
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_SHOW_BODY_TEXT = "EXTRA_SHOW_BODY_TEXT"
        private const val EXTRA_FRAGMENT_NUMBER = "EXTRA_FRAGMENT_NUMBER"
        private const val EXTRA_CURRENCY_FIELD = "EXTRA_CURRENCY_FIELD"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param title Title of the fragment.
         * @param showBodyText Is necessary to show the body text?
         * @param currency Is necessary to set the text field type
         * @param position The fragment number (position in Stepper).
         * @return A new instance of fragment SimpleFieldFragment.
         */
        fun newInstance(title: String, showBodyText: Boolean, currency: Boolean, position: Int):
                SimpleFieldFragment {
            val fragment = SimpleFieldFragment()
            val args = Bundle()
            args.putString(EXTRA_TITLE, title)
            args.putBoolean(EXTRA_SHOW_BODY_TEXT, showBodyText)
            args.putBoolean(EXTRA_CURRENCY_FIELD, currency)
            args.putInt(EXTRA_FRAGMENT_NUMBER, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            if (arguments != null) {
                title = arguments!!.getString(EXTRA_TITLE)
                showBodyText = arguments!!.getBoolean(EXTRA_SHOW_BODY_TEXT)
                currencyField = arguments!!.getBoolean(EXTRA_CURRENCY_FIELD)
                fragmentNumber = arguments!!.getInt(EXTRA_FRAGMENT_NUMBER)
            }
        } catch (ex: Exception) {
            Log.e(classTag, "Attempting to get Extras", ex)
            Rollbar.instance().error(ex, tag)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_simple_field, container, false)

        layoutView = view?.simple_field_layout as View
        editValue = setUpField(view)

        setTitle(view)
        setBodyText(view)

        return view
    }

    override fun onSelected() {
    }

    override fun verifyStep(): VerificationError? {
        return null
    }

    override fun onError(error: VerificationError) {
        Log.e(classTag, error.toString())
    }

    override fun onBackClicked(callback: StepperLayout.OnBackClickedCallback?) {
        callback?.goToPrevStep()
    }

    override fun onNextClicked(callback: StepperLayout.OnNextClickedCallback?) {
        val valid = validateEditText()
        if (valid) {
            val strValue = editValue?.text.toString()
            val value = UtilHelper().formatCurrencyToInt(strValue)

            if (currencyField) {
                UtilHelper().truncateCents(editValue)
            }
            onValueChanged(value)

            callback?.goToNextStep()
        } else {
            val snack = Snackbar.make(layoutView as View,
                    R.string.error_incomplete_step,
                    Snackbar.LENGTH_SHORT)

            try {
                UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
            } catch (ex: Exception) {
                Log.e(classTag, "Attempting to show SnackBar", ex)
                Rollbar.instance().error(ex, tag)
            }
        }
    }

    override fun onCompleteClicked(callback: StepperLayout.OnCompleteClickedCallback?) {
        val valid = validateEditText()
        if (valid) {
            val strValue = editValue?.text.toString()
            val value = UtilHelper().formatCurrencyToInt(strValue)

            validateCents()
            onValueChanged(value)

            when (activity) {
                is SaleRevisionFormActivity -> {
                    val saleRevisionFormActivity = activity as SaleRevisionFormActivity
                    saleRevisionFormActivity.goToSummary()
                }
                is PrizeFormActivity -> {
                    val prizeFormActivity = activity as PrizeFormActivity
                    prizeFormActivity.goToSummary()
                }
                is ExpenseFormActivity -> {
                    val expenseFormActivity = activity as ExpenseFormActivity
                    expenseFormActivity.goToSummary()
                }
            }

            callback?.complete()
        } else {
            val snack = Snackbar.make(layoutView as View,
                    R.string.error_incomplete_step,
                    Snackbar.LENGTH_SHORT)

            try {
                UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
            } catch (ex: Exception) {
                Log.e(classTag, "Attempting to show SnackBar", ex)
                Rollbar.instance().error(ex, tag)
            }
        }
    }

    private fun validateCents() {
        if (currencyField) {
            UtilHelper().truncateCents(editValue)
        }
    }

    private fun setUpField(view: View): EditText {
        val editCurrency = view.simple_field_edit_value_currency
        val editThousands = view.simple_field_edit_value

        return when {
            currencyField -> {
                editCurrency.visibility = View.VISIBLE
                editThousands.visibility = View.GONE
                editCurrency
            }
            else -> {
                editThousands.visibility = View.VISIBLE
                editCurrency.visibility = View.GONE
                setThousandsTextChangedListener(editThousands) // For thousands format
                editThousands
            }
        }
    }

    private fun setThousandsTextChangedListener(field: EditText) {
        field.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                try {
                    var text = editable.toString()

                    if (text.isNotEmpty()
                            && text != lastValue) {
                        text = text.replace(",", "")
                        val value = NumberFormat.getIntegerInstance().format(text.toInt())
                        lastValue = value

                        field.setText(value)
                        field.setSelection(field.text.length) // Place cursor at the end
                    }
                } catch (ex: NumberFormatException) {
                    Log.e(classTag,
                            "Attempting to format EditText with thousand separators", ex)
                    Rollbar.instance().error(ex, tag)
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Nothing to do here
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                // Nothing to do here
            }
        })
    }

    private fun validateEditText(): Boolean {
        if (editValue != null) {
            return editValue!!.text.isNotEmpty()
        }
        return false
    }

    private fun setTitle(view: View?) {
        if (title != null) {
            view?.simple_field_text_label?.text = title
        }
    }

    private fun setBodyText(view: View?) {
        if (showBodyText) {
            view?.simple_field_text_body?.visibility = View.VISIBLE

            val realFund = "${getString(R.string.prizes_real_fund)} ${getRealFund()}"
            view?.simple_field_text_body?.text = realFund
        } else {
            view?.simple_field_text_body?.visibility = View.GONE
        }
    }

    private fun getRealFund(): String {
        return when (activity) {
            is PrizeFormActivity -> {
                val parentActivity = activity as PrizeFormActivity
                PrizeFormActivity.getRealFund(parentActivity)
            }
            is SaleRevisionFormActivity -> {
                val parentActivity = activity as SaleRevisionFormActivity
                SaleRevisionFormActivity.getRealFund(parentActivity)
            }
            else -> ({
                val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
                currencyFormat.format(0)
            }).toString()
        }
    }

    private fun onValueChanged(value: Int) {
        if (listener != null) {
            listener!!.onFragmentInteraction(value, fragmentNumber)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson
     * [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {

        fun onFragmentInteraction(value: Int, fragmentNumber: Int)
    }
}