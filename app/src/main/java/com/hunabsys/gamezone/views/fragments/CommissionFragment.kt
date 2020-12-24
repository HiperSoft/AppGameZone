package com.hunabsys.gamezone.views.fragments
/* ktlint-disable no-wildcard-imports */
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.views.activities.salerevisions.SaleRevisionFormActivity
import com.rollbar.android.Rollbar
import com.stepstone.stepper.BlockingStep
import com.stepstone.stepper.StepperLayout
import com.stepstone.stepper.VerificationError
import faranjit.currency.edittext.CurrencyEditText
import kotlinx.android.synthetic.main.fragment_commission.view.*
import kotlinx.android.synthetic.main.partial_calculate_comission.view.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [CommissionFragment.OnFragmentInteractionListener] interface to handle interaction events.
 * Use the [CommissionFragment.newInstance] factory method to create an instance of this fragment.
 */
class CommissionFragment : Fragment(), BlockingStep {

    private val classTag = CommissionFragment::class.java.simpleName

    private var listener: OnFragmentInteractionListener? = null

    private var layoutView: View? = null
    private var editCommission: CurrencyEditText? = null

    private var commissionPercentage: Double = 0.0
    var title: String? = null
    var fragmentNumber: Int = -1

    companion object {
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_FRAGMENT_NUMBER = "EXTRA_FRAGMENT_NUMBER"
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param title Title of the fragment.
         * @param position Position of this fragment in the related Stepper.
         * @return A new instance of fragment CommissionFragment.
         */
        fun newInstance(title: String, position: Int): CommissionFragment {
            val fragment = CommissionFragment()
            val args = Bundle()
            args.putString(EXTRA_TITLE, title)
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
        val view = inflater.inflate(R.layout.fragment_commission, container, false)

        layoutView = view?.commission_layout as View
        editCommission = view.commission_edit_value

        val textTitle = view.commission_text_label
        val buttonCalculator = view.commission_button_calculator

        setTitle(textTitle)
        setCalculator(buttonCalculator)

        return view
    }

    override fun onCompleteClicked(callback: StepperLayout.OnCompleteClickedCallback?) {
        // Nothing to do here
    }

    override fun onNextClicked(callback: StepperLayout.OnNextClickedCallback?) {
        if (editCommission != null) {
            if (editCommission!!.text.isNotEmpty()) {
                val strCommission = editCommission?.text.toString()
                val commission = UtilHelper().formatCurrencyToInt(strCommission)

                UtilHelper().truncateCents(editCommission)
                onValueChanged(commission)

                callback?.goToNextStep()
            } else {
                val snack = Snackbar.make(layoutView as View,
                        R.string.error_incomplete_step,
                        Snackbar.LENGTH_SHORT)
                try {
                    UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
                } catch (ex: Exception) {
                    Log.e(classTag, "Attempting to show SnackBar", ex)
                }
            }
        }
    }

    override fun onSelected() {
    }

    override fun verifyStep(): VerificationError? {
        return null
    }

    override fun onBackClicked(callback: StepperLayout.OnBackClickedCallback?) {
        callback?.goToPrevStep()
    }

    override fun onError(error: VerificationError) {
        Log.e(classTag, error.toString())
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

    private fun setTitle(textView: TextView?) {
        if (title != null) {
            textView?.text = title
        }
    }

    private fun setCalculator(button: ImageButton) {
        button.setOnClickListener {
            showDialog(layoutView)
        }
    }

    private fun setListenersToDialogButtons(popupWindow: PopupWindow, popupView: View) {
        val buttonAccept = popupView.calculate_commission_button_accept
        buttonAccept.setOnClickListener {
            val strCommission = popupView.calculate_commission_edit_percentage.text.toString()
            if (strCommission.isEmpty()) {
                val snack = Snackbar.make(popupView,
                        R.string.error_empty_fields,
                        Snackbar.LENGTH_SHORT)
                UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
            } else {
                commissionPercentage = strCommission.toDouble()
                val strTakenAmount = popupView.calculate_commission_edit_taken.text.toString()
                val takenAmount = UtilHelper().formatCurrencyToInt(strTakenAmount)
                val commission = calculateCommission(takenAmount.toDouble())
                editCommission?.setText(String.format("%.2f", commission.toFloat()))
                popupWindow.dismiss()
            }
        }

        val buttonCancel = popupView.calculate_commission_button_cancel
        buttonCancel.setOnClickListener { popupWindow.dismiss() }
    }

    private fun calculateCommission(takenAmount: Double): Long {
        val commission = takenAmount * (commissionPercentage / 100)
        return Math.round(commission)
    }

    private fun onValueChanged(value: Int) {
        if (listener != null) {
            val values = intArrayOf(value, commissionPercentage.toInt())
            listener!!.onFragmentInteraction(values, fragmentNumber)
        }
    }

    @SuppressLint("InflateParams")
    private fun showDialog(parent: View?) {
        // Popup
        val inflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.partial_calculate_comission, null)
        val parentView = parent?.parent as View

        val popupWindow = PopupWindow(
                popupView,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                true)

        val parentActivity = activity as SaleRevisionFormActivity
        val takenAmount = SaleRevisionFormActivity.getGameMachineOutcome()
        val defaultCommission = SaleRevisionFormActivity.getCommissionPercentage(parentActivity)

        popupView.calculate_commission_edit_taken.setText(takenAmount)

        popupView.calculate_commission_edit_percentage.setText(defaultCommission)
        popupView.calculate_commission_edit_percentage.requestFocus()

        setListenersToDialogButtons(popupWindow, popupView)

        popupWindow.showAtLocation(parentView, Gravity.CENTER, 0, 0)
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

        fun onFragmentInteraction(values: IntArray, fragmentNumber: Int)
    }
}
