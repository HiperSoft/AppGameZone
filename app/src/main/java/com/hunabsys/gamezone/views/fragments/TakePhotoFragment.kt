package com.hunabsys.gamezone.views.fragments

/* ktlint-disable no-wildcard-imports */
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.facebook.common.util.UriUtil
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.rollbar.android.Rollbar
import com.stepstone.stepper.BlockingStep
import com.stepstone.stepper.StepperLayout
import com.stepstone.stepper.VerificationError
import kotlinx.android.synthetic.main.fragment_take_photo.view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

const val CAMERA_REQUEST_CODE = 0

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [TakePhotoFragment.OnFragmentInteractionListener] interface to handle interaction events.
 * Use the [TakePhotoFragment.newInstance] factory method to create an instance of this fragment.
 */
open class TakePhotoFragment : Fragment(), BlockingStep {

    private val classTag = TakePhotoFragment::class.java.simpleName
    private var listener: OnFragmentInteractionListener? = null
    private var layoutView: View? = null
    private var editValue: EditText? = null
    private var lastValue: String = ""
    private var currencyField: Boolean = false
    private var imageFilePath: String? = null

    var validatedCode: Int = 0
    var title: String? = null
    var fragmentNumber: Int = -1
    var imageFilePath2: String? = null

    var imageChanges: String? = null
    var imageFileBase64: String? = null

    companion object {

        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CURRENCY_FIELD = "EXTRA_CURRENCY_FIELD"
        const val EXTRA_FRAGMENT_NUMBER = "EXTRA_FRAGMENT_NUMBER"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param title Title of the fragment.
         * @param position The fragment number (position in Stepper).
         * @return A new instance of fragment TakePhotoFragment.
         */
        fun newInstance(title: String, currency: Boolean, position: Int): TakePhotoFragment {
            val fragment = TakePhotoFragment()
            val args = Bundle()
            args.putString(EXTRA_TITLE, title)
            args.putBoolean(EXTRA_CURRENCY_FIELD, currency)
            args.putInt(EXTRA_FRAGMENT_NUMBER, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        validatedCode = 0

        try {
            if (arguments != null) {
                title = arguments!!.getString(EXTRA_TITLE)
                currencyField = arguments!!.getBoolean(EXTRA_CURRENCY_FIELD)
                fragmentNumber = arguments!!.getInt(EXTRA_FRAGMENT_NUMBER)
            }
        } catch (ex: Exception) {
            Log.e(classTag, "Attempting to get Extras", ex)
            Rollbar.instance().error(ex, tag)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_take_photo, container, false)

        val textTitle = view?.take_photo_text_label
        val buttonTakePhoto = view?.take_photo_button_photo

        layoutView = view?.take_photo_layout
        editValue = setUpField(view)

        setTitle(textTitle)
        setCamera(buttonTakePhoto, layoutView)

        if (fragmentNumber == 1 && !imageFilePath.isNullOrEmpty()) {
            val imageUri = Uri.Builder()
                    .scheme(UriUtil.LOCAL_FILE_SCHEME)
                    .path(imageFilePath)
                    .build()
            view!!.fragment_image_preview.setImageURI(imageUri)
        }
        if (fragmentNumber == 2 && !imageFilePath2.isNullOrEmpty()) {
            val imageUri = Uri.Builder()
                    .scheme(UriUtil.LOCAL_FILE_SCHEME)
                    .path(imageFilePath2)
                    .build()
            view!!.fragment_image_preview.setImageURI(imageUri)
        }

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
        try {
            if (editValue != null) {
                if (editValue!!.text.isNotEmpty()) {
                    val strValue = editValue?.text.toString()
                    val value = UtilHelper().formatCurrencyToInt(strValue)

                    if (currencyField) {
                        UtilHelper().truncateCents(editValue)
                    }

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

    override fun onCompleteClicked(callback: StepperLayout.OnCompleteClickedCallback?) {
        // Nothing to do here
    }

    fun setThousandsTextChangedListener(field: EditText?) {
        field?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                try {
                    var text = editable.toString()

                    text = checkIfContainsNegativeSimbol(text, field)

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

    fun checkIfContainsNegativeSimbol(text: String, field: EditText?): String {
        var value = text
        if (text.length == 1) {
            if (text.contains("-")) {
                value = text.replace("-", "")
                field?.setText(value)
                field?.setSelection(field.text.length) // Place cursor at the end
            }
        }

        return value
    }

    fun setTitle(textView: TextView?) {
        if (title != null) {
            textView?.text = title
        }
    }

    fun setCamera(button: Button?, view: View?) {
        button?.setOnClickListener {
            try {
                val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (callCameraIntent.resolveActivity(activity?.packageManager) != null) {
                    val authorities = activity?.packageName + ".fileprovider"
                    val imageFile = createImageFile()
                    val imageUri = FileProvider.getUriForFile(activity!!.applicationContext,
                            authorities, imageFile)
                    callCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
                }
            } catch (ex: IOException) {
                val snack = Snackbar.make(view as View,
                        getString(R.string.default_no_photos), Snackbar.LENGTH_SHORT)
                UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_WARNING)
                Rollbar.instance().info(ex, tag)
            }
        }
    }

    private fun setUpField(view: View): EditText {
        val editCurrency = view.take_photo_edit_value_currency
        val editThousands = view.take_photo_edit_value
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    validatedCode = resultCode

                    var filepath = imageFilePath2

                    if (fragmentNumber == 1) {
                        filepath = imageFilePath
                    }
                    val imgUri = Uri.Builder()
                            .scheme(UriUtil.LOCAL_FILE_SCHEME)
                            .path(filepath)
                            .build()

                    if (filepath != null) {
                        imageFileBase64 = onChangeImageFile(filepath)
                    }

                    if (data != null) {
                        StorageAccess().deleteLatestImageFile()
                    }

                    view!!.fragment_image_preview.setImageURI(imgUri)

                    val snack = Snackbar.make(view as View,
                            getString(R.string.message_saved_photo),
                            Snackbar.LENGTH_SHORT)

                    UtilHelper().showSnackBar(context!!, snack, UtilHelper.SNACK_TYPE_SUCCESS)
                }
            }
        }
    }

    private fun onChangeImageFile(img: String?): String? {
        return try {
            val bitmap = BitmapFactory.decodeFile(img)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 30, outputStream)
            val byteArray = outputStream.toByteArray()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (ex: FileNotFoundException) {
            Log.e(tag, "Attempting to convert bitmap to base 64", ex)
            Rollbar.instance().error(ex, tag)
            null
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(Date())
        val imageFileName: String = "JPEG_" + timeStamp + "_"
        val storageDir: File = activity!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!storageDir.exists()) storageDir.mkdirs()
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        if (fragmentNumber == 1) {
            imageFilePath = imageFile.absolutePath
        } else {
            imageFilePath2 = imageFile.absolutePath
        }

        return imageFile
    }

    private fun onValueChanged(value: Int) {
        if (listener != null) {
            listener!!.onFragmentInteraction(value, fragmentNumber)
        }
    }

    fun onImageChanged(value: String) {
        if (listener != null) {
            listener!!.onFragmentImageInteraction(value, fragmentNumber)
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
        fun onFragmentImageInteraction(value: String, fragmentNumber: Int)
    }
}
