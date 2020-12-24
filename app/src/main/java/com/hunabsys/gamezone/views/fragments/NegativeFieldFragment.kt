package com.hunabsys.gamezone.views.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.facebook.common.util.UriUtil
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.rollbar.android.Rollbar
import com.stepstone.stepper.Step
import com.stepstone.stepper.StepperLayout
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.fragment_negative_field.view.*

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [NegativeFieldFragment.OnFragmentInteractionListener] interface to handle interaction events.
 * Use the [NegativeFieldFragment.newInstance] factory method to  create an instance of this
 * fragment.
 */
class NegativeFieldFragment : TakePhotoFragment(), Step {

    private val classTag = NegativeFieldFragment::class.java.simpleName

    private var listener: OnFragmentInteractionListener? = null

    private var layoutView: View? = null
    private var editValue: EditText? = null

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param title Parameter 1.
         * @param position Parameter 2.
         * @return A new instance of fragment NegativeFieldFragment.
         */
        fun newInstance(title: String, position: Int): NegativeFieldFragment {
            val fragment = NegativeFieldFragment()
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
        val view = inflater.inflate(R.layout.fragment_negative_field,
                container,
                false)

        layoutView = view?.negative_field_layout as View
        editValue = view.negative_field_edit_value

        val textView = view.negative_field_text_label
        val buttonPhoto = view.negative_field_button_photo
        val buttonNegative = view.negative_field_button_negative

        setTitle(textView)
        setCamera(buttonPhoto, layoutView)
        setListenerToNegativeButton(buttonNegative)
        setThousandsTextChangedListener(editValue)

        if (fragmentNumber == 2 && !imageFilePath2.isNullOrEmpty()) {
            val imageUri = Uri.Builder()
                    .scheme(UriUtil.LOCAL_FILE_SCHEME)
                    .path(imageFilePath2)
                    .build()
            view.fragment_image_preview.setImageURI(imageUri)
        }
        return view
    }

    override fun onError(error: VerificationError) {
        Log.e(classTag, error.toString())
    }

    override fun onNextClicked(callback: StepperLayout.OnNextClickedCallback?) {
        try {
            if (editValue != null) {
                if (editValue!!.text.isNotEmpty()
                        && editValue!!.text.toString() != "-") {
                    val strValue = editValue?.text.toString()
                    val value = UtilHelper().formatCurrencyToInt(strValue)

                    onValueChanged(value)

                    if (!imageChanges.isNullOrEmpty()) {
                        imageChanges = imageFileBase64
                    }
                    nextForm(callback)
                } else {
                    val snack = Snackbar.make(layoutView as View,
                            R.string.error_incomplete_step,
                            Snackbar.LENGTH_SHORT)
                    UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
                }
            }
        } catch (ex: Exception) {
            Log.e(classTag, "Attempting to get editValue value", ex)
            Rollbar.instance().error(ex, tag)
        }
    }

    private fun nextForm(callback: StepperLayout.OnNextClickedCallback?) {
        if (validatedCode != CAMERA_REQUEST_CODE) {
            if (imageFileBase64!!.isNotEmpty()) {
                if (imageChanges !== imageFileBase64) {
                    onImageChanged(imageFileBase64!!)
                    this.imageChanges = imageFileBase64
                }
                callback?.goToNextStep()
            } else {
                val snack = Snackbar.make(layoutView as View,
                        R.string.error_incomplete_step_photo,
                        Snackbar.LENGTH_SHORT)
                UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
            }
        } else {
            val snack = Snackbar.make(layoutView as View,
                    R.string.error_incomplete_step_photo,
                    Snackbar.LENGTH_SHORT)
            UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
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

    private fun setListenerToNegativeButton(button: Button) {
        button.setOnClickListener {
            val negativeSymbol = getString(R.string.sale_revisions_negative)
            var quantity = editValue?.text.toString()

            // Toggle negative symbol
            if (quantity.isNotEmpty()) {
                quantity = if (quantity.contains(negativeSymbol)) {
                    quantity.replace(negativeSymbol, "")
                } else {
                    negativeSymbol + quantity
                }
                editValue?.setText(quantity)
            }
        }
    }

    private fun onValueChanged(value: Int) {
        if (listener != null) {
            listener!!.onFragmentInteraction(value, fragmentNumber)
        }
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
