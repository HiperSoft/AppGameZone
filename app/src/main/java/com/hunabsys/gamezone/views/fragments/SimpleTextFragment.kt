package com.hunabsys.gamezone.views.fragments

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.rollbar.android.Rollbar
import com.stepstone.stepper.BlockingStep
import com.stepstone.stepper.StepperLayout
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.fragment_simple_text.view.*

class SimpleTextFragment : Fragment(), BlockingStep {

    private val classTag = SimpleTextFragment::class.java.simpleName

    private var listener: OnFragmentInteractionListener? = null

    private var layoutView: View? = null
    private var editValue: EditText? = null

    private var title: String? = null
    private var valueText: String? = null
    private var fragmentNumber: Int = -1

    companion object {
        private const val EXTRA_TITLE = "EXTRA_TITLE"
        private const val EXTRA_FRAGMENT_NUMBER = "EXTRA_FRAGMENT_NUMBER"

        fun newInstance(title: String, position: Int): SimpleTextFragment {
            val fragment = SimpleTextFragment()
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

        val view = inflater.inflate(R.layout.fragment_simple_text, container, false)

        layoutView = view?.simple_text_layout as View
        editValue = view.simple_text_edit_value

        setTitle(view)

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
            valueText = editValue?.text.toString()

            onTextValueChanged(valueText!!)

            callback?.goToNextStep()
        } else {
            val snack = Snackbar.make(layoutView as View,
                    R.string.error_incomplete_step_text,
                    Snackbar.LENGTH_LONG)
            try {
                UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
            } catch (ex: Exception) {
                Log.e(classTag, "Attempting to show SnackBar", ex)
                Rollbar.instance().error(ex, tag)
            }
        }
    }

    override fun onCompleteClicked(callback: StepperLayout.OnCompleteClickedCallback?) {
        // Nothing to do here
    }

    private fun validateEditText(): Boolean {
        if (editValue != null) {
            return editValue!!.text.isNotEmpty()
        }
        return false
    }

    private fun setTitle(view: View?) {
        if (title != null) {
            view?.simple_text_label?.text = title
        }
    }

    private fun onTextValueChanged(value: String) {
        if (listener != null) {
            listener!!.onFragmentTextInteraction(value)
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

    interface OnFragmentInteractionListener {

        fun onFragmentTextInteraction(value: String)
    }
}