package com.hunabsys.gamezone.views.activities.expenses

/* ktlint-disable no-wildcard-imports */
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.support.v4.content.FileProvider
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import com.facebook.common.util.UriUtil
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.helpers.UtilHelper
import com.hunabsys.gamezone.models.daos.EvidenceDao
import com.hunabsys.gamezone.models.daos.ExpenseDao
import com.hunabsys.gamezone.models.daos.GameMachineDao
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.Expense
import com.hunabsys.gamezone.pojos.ItemExpenseSummary
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.adapters.ExpenseSummaryListAdapter
import com.rollbar.android.Rollbar
import kotlinx.android.synthetic.main.activity_expense_summary.*
import kotlinx.android.synthetic.main.partial_machine_folio_bar.view.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ExpenseSummaryActivity : BaseActivity() {

    private val tag = ExpenseSummaryActivity::class.java.simpleName

    private var validatedCode: Int = 0
    private var defaultImage: ImageView? = null
    private var imageFileBase64: String? = null
    private var imageFilePath: String? = null

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this activity using the provided parameters.
         *
         * @param context Previous activity context.
         * @param routeFolio String containing route folio.
         * @return A new intent for activity ExpenseSummaryActivity.
         */
        fun getStartIntent(context: Context, routeFolio: String): Intent {
            val intent = Intent(context, ExpenseSummaryActivity::class.java)
            intent.putExtra(IExpenseConstants.EXTRA_ROUTE_FOLIO, routeFolio)
            return intent
        }

        private const val CAMERA_REQUEST_CODE = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_summary)

        val viewLayout = expense_summary_layout
        val buttonTakePhoto = expense_summary_button_photo
        defaultImage = expense_summary_preview_default_image

        setCamera(buttonTakePhoto, viewLayout)

        showRouteFolio()
        addItemsToListView()
        setListenerForSaveButton()
    }

    override fun onSupportNavigateUp(): Boolean {
        UtilHelper().showExitDialog(this)
        return true
    }

    override fun onBackPressed() {
        UtilHelper().showExitDialog(this)
    }

    private fun showProgress(show: Boolean) {
        UtilHelper().showView(expense_summary_progress, show)
        expense_summary_button_save.isEnabled = !show
    }

    private fun getConcept(): String {
        val concepts = ExpenseFormActivity.getConcept()

        if (!concepts.isEmpty()) {
            return concepts
        }
        return ""
    }

    private fun getQuantity(): Int {
        val quantities = ExpenseFormActivity.getAmount()

        if (quantities != 0) {
            return quantities
        }
        return 0
    }

    private fun getPointOfSaleName(): String {
        val id = ExpenseFormActivity.getPointOfSaleFolio().toLong()
        val pointOfSale = PointOfSaleDao().findById(id)?.name

        return pointOfSale.toString()
    }

    private fun getRouteFolio(): String {
        return this.intent.extras.getString(IExpenseConstants.EXTRA_ROUTE_FOLIO)
    }

    private fun showRouteFolio() {
        val textView = expense_summary_route_bar.game_machine_bar_text_folio
        textView.text = getRouteFolio()
    }

    private fun addItemsToListView() {
        val rows = getRows()
        expense_summary_list.adapter = ExpenseSummaryListAdapter(this, rows)
    }

    private fun getPointOfSaleRow(position: Int): ItemExpenseSummary {
        val labels = resources.getStringArray(R.array.expenses_summary)
        val posName = getPointOfSaleName()
        Log.e(tag, "Name: $posName")
        return ItemExpenseSummary(labels[position], posName)
    }

    private fun getConceptRow(position: Int): ItemExpenseSummary {
        val labels = resources.getStringArray(R.array.expenses_summary)
        val concept = getConcept()
        return ItemExpenseSummary(labels[position], concept)
    }

    private fun getAmountRow(position: Int): ItemExpenseSummary {
        val labels = resources.getStringArray(R.array.expenses_summary)
        val quantity = getQuantity()
        return ItemExpenseSummary(labels[position], quantity.toString())
    }

    private fun getRows(): ArrayList<ItemExpenseSummary> {
        val rows = ArrayList<ItemExpenseSummary>()

        var row = getPointOfSaleRow(IExpenseConstants.POINT_OF_SALE)
        rows.add(row)

        row = getConceptRow(IExpenseConstants.CONCEPT)
        rows.add(row)

        row = getAmountRow(IExpenseConstants.AMOUNT)
        rows.add(row)

        return rows
    }

    private fun setCamera(button: Button, view: View) {
        button.setOnClickListener {
            try {
                val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                if (callCameraIntent.resolveActivity(packageManager) != null) {
                    val imageFile = createImageFile()
                    val authorities = "$packageName.fileprovider"
                    val imageUri = FileProvider.getUriForFile(applicationContext,
                            authorities, imageFile)
                    callCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                    startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
                }
            } catch (ex: IOException) {
                val snack = Snackbar.make(view,
                        getString(R.string.default_no_photos),
                        Snackbar.LENGTH_SHORT)
                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                Rollbar.instance().info(ex, tag)
            }
        }
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(Date())
        val imageFileName: String = "JPEG_" + timeStamp + "_"
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (!storageDir.exists()) storageDir.mkdirs()
        val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
        imageFilePath = imageFile.absolutePath

        return imageFile
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    validatedCode = resultCode

                    val filepath = imageFilePath

                    val imgUri = Uri.Builder()
                            .scheme(UriUtil.LOCAL_FILE_SCHEME)
                            .path(filepath)
                            .build()

                    imageFileBase64 = onChangeImageFile(filepath)

                    if (data != null) {
                        StorageAccess().deleteLatestImageFile()
                    }

                    expense_summary_preview_default_image.visibility = View.GONE
                    expense_summary_preview_image.visibility = View.VISIBLE

                    expense_summary_preview_image.setImageURI(imgUri)

                    val snack = Snackbar.make(expense_summary_layout,
                            getString(R.string.message_saved_photo),
                            Snackbar.LENGTH_SHORT)

                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)
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
            Log.e("Error", "Attempting to convert bitmap to base 64", ex)
            Rollbar.instance().error(ex, tag)
            null
        }
    }

    private fun setExpenseData(expense: Expense): Boolean {
        try {
            expense.concept = getConcept()
            expense.amount = getQuantity().toDouble()
            expense.comments = expense_summary_edit_comment.text.toString()
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to set quantities to expense object", ex)
            Rollbar.instance().error(ex, tag)
            return false
        }
        return true
    }

    private fun setData(expense: Expense): Boolean {
        try {
            val pointOfSale = PointOfSaleDao().findById(ExpenseFormActivity
                    .getPointOfSaleFolio().toLong())

            val weekNumber = GameMachineDao().findAll().max("week")

            if (weekNumber == null || pointOfSale == null) {
                UtilHelper().showCorruptInformationAlert(this)
            } else {
                expense.pointOfSaleId = pointOfSale.id
                expense.week = weekNumber.toInt()
            }

            expense.userId = PreferencesHelper(this).userId
            expense.createdAt = Date()
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to set data to expense object", ex)
            Rollbar.instance().error(ex, tag)
            return false
        }
        return true
    }

    private fun setEvidence(expense: Expense): Boolean {
        try {
            if (imageFileBase64!!.isNotEmpty()) {
                val type = IExpenseConstants.EVIDENCE_TYPE

                val id: Long = EvidenceDao().getId()
                val userId = PreferencesHelper(this).userId
                val date = expense.createdAt
                val evidenceDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                        Locale.getDefault()).format(date)

                val filename = "$evidenceDate$id.png"

                val evidence = Evidence(id = id, userId = userId, evidenceId = expense.id,
                        evidenceableType = type, file = imageFileBase64!!, filename = filename,
                        originalFilename = filename)

                val savedEvidence = EvidenceDao().saveEvidence(evidence)

                expense.evidence = savedEvidence
                return true
            } else {
                Log.e(tag, "Cannot get images: EMPTY")
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to create Evidence objects", ex)
            Rollbar.instance().error(ex, tag)
        }

        return false
    }

    private fun saveExpense(): Boolean {
        val expense = Expense()

        val savedExpenseData = setExpenseData(expense)
        val savedOtherData = setData(expense)
        val savedEvidence = setEvidence(expense)

        if (savedExpenseData && savedOtherData && savedEvidence) {
            ExpenseDao().create(expense)
            Log.d(tag, "Created new Expense")
        } else {
            return false
        }
        return true
    }

    private fun setListenerForSaveButton() {
        expense_summary_button_save.setOnClickListener {
            if (validatedCode != CAMERA_REQUEST_CODE
                    && this.imageFileBase64!!.isNotEmpty()) {
                showProgress(true)

                val saved = saveExpense()

                StorageAccess().deleteAllImageFiles(tag, this)

                if (saved) {
                    val snack = Snackbar.make(expense_summary_layout,
                            R.string.message_saved_changes,
                            Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)

                    setResult(IExpenseConstants.RESULT_CODE_SUCCESS)

                    Handler().postDelayed({
                        showProgress(false)
                        finish()
                        AnimationHelper().exitTransition(this)
                    }, IExpenseConstants.FINISH_DELAY)
                } else {
                    showProgress(false)
                    val snack = Snackbar.make(expense_summary_layout,
                            R.string.error_saving,
                            Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
                }
            } else {
                val snack = Snackbar.make(expense_summary_layout,
                        getString(R.string.error_incomplete_step_photo),
                        Snackbar.LENGTH_SHORT)

                UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
            }
        }
    }
}