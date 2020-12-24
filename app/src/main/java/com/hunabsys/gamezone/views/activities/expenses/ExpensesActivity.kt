package com.hunabsys.gamezone.views.activities.expenses

/* ktlint-disable no-wildcard-imports */
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.*
import com.hunabsys.gamezone.models.daos.EvidenceDao
import com.hunabsys.gamezone.models.daos.ExpenseDao
import com.hunabsys.gamezone.models.daos.PointOfSaleDao
import com.hunabsys.gamezone.models.daos.RouteDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.Expense
import com.hunabsys.gamezone.models.datamodels.PointOfSale
import com.hunabsys.gamezone.services.delegates.IEvidenceDelegate
import com.hunabsys.gamezone.services.delegates.IExpenseDelegate
import com.hunabsys.gamezone.services.rest.HttpClientService
import com.hunabsys.gamezone.services.rest.SynchronizeEvidencesService
import com.hunabsys.gamezone.services.rest.SynchronizeExpensesService
import com.hunabsys.gamezone.storage.storage.StorageAccess
import com.hunabsys.gamezone.views.activities.BaseActivity
import com.hunabsys.gamezone.views.activities.ScannerActivity
import com.hunabsys.gamezone.views.activities.expenses.IExpenseConstants.Companion.EVIDENCE_TYPE
import com.hunabsys.gamezone.views.adapters.ExpensesListAdapter
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults
import kotlinx.android.synthetic.main.activity_expenses.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ExpensesActivity : BaseActivity(), IExpenseDelegate, IEvidenceDelegate {

    private val tag = ExpensesActivity::class.java.simpleName
    private var route = ""
    private var pointOfSaleFolio: String = ""
    private var buttonPosition: Int = 0
    private var evidenceNumber = 0
    private var expenseWebId = 0L
    private var synchronizeStarted = false
    private lateinit var itemStatus: MenuItem

    private val qrCode = ArrayList<String>(4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        Realm.init(this)

        setCodesToArray()
        setListenerToViews()
    }

    override fun onResume() {
        super.onResume()
        showExpenses()

        if (synchronizeStarted) {
            UtilHelper().showView(expenses_progress, true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.expenses, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId

        if (id == R.id.action_synchronize) {
            UtilHelper().showView(expenses_progress, true)
            itemStatus = item
            item.isEnabled = false
            isSessionActive()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PermissionsHelper.REQUEST_PERMISSIONS ->
                // If request is cancelled, the result arrays are empty.
                if (PermissionsHelper(this).validatePermissionResult(grantResults)) {
                    goToScannerScreen()
                } else {
                    Log.e(tag, "Denied permissions :(")

                    val snack = Snackbar.make(expenses_layout,
                            R.string.error_missing_permissions,
                            Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            if (requestCode == ScannerActivity.REQUEST_CODE_CAMERA) {
                try {
                    val code = data!!.extras.getString(ScannerActivity.EXTRA_RESULT_CODE)
                    tryToFindPointOSale(code)
                } catch (ex: Exception) {
                    Log.d(tag, "Attempting to get extras from result", ex)
                    Rollbar.instance().error(ex, tag)
                }
            }
        }
    }

    private fun getNotSyncedExpense(): List<Expense> {
        val allExpenses = ExpenseDao().findAll(PreferencesHelper(this).userId)
        return allExpenses.filter { !it.isSynchronized }
    }

    private fun getNotSyncedExpenses(): List<Expense> {
        val allExpenses = ExpenseDao().findAll(PreferencesHelper(this).userId)
        val notSynchronizedExpenses = allExpenses.filter { !it.isSynchronized }
        return notSynchronizedExpenses.filter { it.webId == 0L }
    }

    private fun getExpenseEvidence(): List<Evidence> {
        val allEvidences = EvidenceDao()
                .findAllByType(EVIDENCE_TYPE, PreferencesHelper(this).userId)
        val evidencesNotSynchronized = allEvidences.filter { !it.isSynchronized }
        return evidencesNotSynchronized.filter { it.evidenceableId == expenseWebId }
    }

    private fun getNotSyncedEvidences(): List<Evidence> {
        val allEvidences = EvidenceDao()
                .findAllByType(EVIDENCE_TYPE, PreferencesHelper(this).userId)
        return allEvidences.filter { !it.isSynchronized }
    }

    private fun isSessionActive() {
        if (!HttpClientService.sessionUnauthorized) {
            tryToSynchronizeExpenses()
        } else {
            UtilHelper().showView(expenses_progress, false)
            itemStatus.isEnabled = true
            LogoutHelper().tryToLogout(this)
        }
    }

    private fun tryToSynchronizeExpenses() {
        val noSyncedExpenses = getNotSyncedExpenses()
        val notSyncedEvidences = getNotSyncedEvidences()

        when {
            noSyncedExpenses.isNotEmpty() -> {
                val formattedExpense = getFormattedExpenses(noSyncedExpenses[0])

                val syncing = SynchronizeExpensesService(this)
                        .synchronizeExpenses(formattedExpense)

                validateSynchronization(syncing)
                evidenceNumber = 0
            }

            notSyncedEvidences.isNotEmpty() -> {
                expenseWebId = 0L
                tryToSynchronizeEvidences()
                setStatusExpenses()
            }
            else -> {
                if (synchronizeStarted) {
                    // In case that some expense status couldn't change, change it
                    setStatusExpenses()
                    UtilHelper().showView(expenses_progress, false)

                    val snack = Snackbar.make(expenses_layout,
                            getString(R.string.expenses_synced), Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_SUCCESS)
                    synchronizeStarted = false
                    itemStatus.isEnabled = true
                } else {
                    UtilHelper().showView(expenses_progress, false)

                    val snack = Snackbar.make(expenses_layout,
                            getString(R.string.expenses_not_pending), Snackbar.LENGTH_LONG)
                    UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
                    itemStatus.isEnabled = true
                }
            }
        }
    }

    private fun notifySynchronizationStarted() {
        if (!synchronizeStarted) {
            val snack = Snackbar.make(expenses_layout,
                    getString(R.string.expenses_syncing), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_WARNING)
            synchronizeStarted = true
        }
    }

    private fun getFormattedExpenses(expense: Expense): JSONObject {
        val expenses = JSONArray()

        val date = expense.createdAt
        val expenseDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                Locale.getDefault()).format(date)

        val expenseJson = JSONObject()
                .put("user_id", expense.userId)
                .put("point_of_sale_id", expense.pointOfSaleId)
                .put("concept", expense.concept)
                .put("amount", expense.amount)
                .put("week_number", expense.week)
                .put("comments", expense.comments)
                .put("created_at", expenseDate)
                .put("mobile_id", expense.id)

        expenses.put(expenseJson)

        val expenseArray = JSONObject()
                .put("expenses_array", expenses)

        return JSONObject()
                .put("expenses", expenseArray)
    }

    private fun tryToSynchronizeEvidences() {
        val noSyncedEvidence: List<Evidence> = if (expenseWebId == 0L) {
            // When the expenses are synced the variable can't change then get all the
            // evidences that are not synced and try to synchronize them
            getNotSyncedEvidences()
        } else {
            getExpenseEvidence()
        }

        if (noSyncedEvidence.isNotEmpty()) {
            val item = JSONObject()

            val fileDescription = JSONObject()
                    .put("file", noSyncedEvidence[0].file)
                    .put("filename", noSyncedEvidence[0].filename)
                    .put("original_filename", noSyncedEvidence[0].originalFilename)

            val file = JSONObject()
                    .put("evidenceable_id", noSyncedEvidence[0].evidenceableId)
                    .put("evidenceable_type", noSyncedEvidence[0].evidenceableType)
                    .put("file", fileDescription)

            item.put("evidence", file)

            val syncing = SynchronizeEvidencesService(this)
                    .synchronizeEvidences(item, noSyncedEvidence[0].id)

            validateSynchronization(syncing)
            evidenceNumber++
        }
    }

    private fun validateSynchronization(syncing: Boolean) {
        if (syncing) {
            notifySynchronizationStarted()
        } else {
            UtilHelper().showView(expenses_progress, false)

            synchronizeStarted = false
            val snack = Snackbar.make(expenses_layout,
                    getString(R.string.error_no_internet_connection), Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
            itemStatus.isEnabled = true
        }
    }

    private fun getPointsOfSale(): RealmResults<PointOfSale> {
        return PointOfSaleDao().findAll()
    }

    private fun getExpenses(): RealmResults<Expense> {
        return ExpenseDao().findAll(PreferencesHelper(this).userId)
    }

    private fun setDoneExpensesNumber() {
        val expenses = getExpenses().size

        val doneExpenses = if (expenses == 1) {
            "$expenses " + getString(R.string.expenses_given_expense)
        } else {
            "$expenses " + getString(R.string.expenses_given_expenses)
        }

        expenses_text_number.text = doneExpenses
    }

    private fun addItemToListView() {
        val expenses = getExpenses()

        if (expenses.size != 0) {
            expenses_list.adapter = ExpensesListAdapter(this, expenses)
            expenses_text_no_expenses.visibility = View.GONE
        }
    }

    private fun showExpenses() {
        UtilHelper().showView(expenses_progress, true)

        setDoneExpensesNumber()
        addItemToListView()

        UtilHelper().showView(expenses_progress, false)
    }

    private fun goToScannerScreen() {
        val intent = Intent(this, ScannerActivity::class.java)
        startActivityForResult(intent, ScannerActivity.REQUEST_CODE_CAMERA)
        AnimationHelper().enterTransition(this)
    }

    private fun goToFormScreen(code: String, pointOfSaleFolio: String) {
        val intent = ExpenseFormActivity.getStartIntent(this, code, pointOfSaleFolio)
        startActivity(intent)
        AnimationHelper().enterTransition(this)
    }

    private fun setCodesToArray() {
        qrCode.add(0, "pos-000")
        qrCode.add(1, "pos-00")
        qrCode.add(2, "pos-0")
        qrCode.add(3, "pos-")
    }

    private fun validateCode(code: String): Boolean {
        var pointOfSaleCode = false

        for (qr in qrCode) {
            pointOfSaleCode = getPointsOfSale()
                    .map { "$qr${it.id}" }
                    .contains(code)
            if (pointOfSaleCode) {
                val index = code.indexOf('-')
                pointOfSaleFolio = if (index == -1) {
                    null.toString()
                } else {
                    code.substring(index + 1)
                }
                break
            }
        }

        if (pointOfSaleCode) {
            route = RouteDao().findAll().first()?.code!!
            return true
        }
        return false
    }

    private fun tryToFindPointOSale(code: String) {
        val found = validateCode(code)

        if (found) {
            Log.d(tag, "-------> Point of sale was found!")
            goToFormScreen(route, pointOfSaleFolio)
        } else {
            val snack = Snackbar.make(expenses_layout,
                    R.string.error_unregistered_pos,
                    Snackbar.LENGTH_LONG)
            UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
        }
    }

    private fun validatePermissions() {
        val hasPermissions = PermissionsHelper(this).requestAllPermissions()

        if (hasPermissions) {
            goToScannerScreen()
        }
    }

    private fun validateEvidenceNumber() {
        when (evidenceNumber) {
            1 -> {
                setStatusExpenses()
                isSessionActive()
            }
            else -> {
                isSessionActive()
            }
        }
    }

    private fun setStatusExpenses() {
        val expenses = if (expenseWebId != 0L) {
            getNotSyncedExpense().filter { it.webId == expenseWebId }
        } else {
            getNotSyncedExpense()
        }

        if (expenses.isNotEmpty()) {
            for (revision in expenses) {
                val expenseObject = ExpenseDao().findCopyById(revision.id)
                val evidence = expenseObject.evidence

                if (evidence != null) {
                    if (evidence.isSynchronized) {
                        expenseObject.isSynchronized = true
                        ExpenseDao().update(expenseObject)
                    }
                }
            }
        }
        setDoneExpensesNumber()
        addItemToListView()
    }

    private fun setListenerToViews() {
        expenses_fab.setOnClickListener {
            if (StorageAccess().checkInternalStorageAvailable()) {
                validatePermissions()
            } else {
                UtilHelper().showStorageAvailableAlert(this)
            }
        }

        expenses_list.setOnScrollListener(object : RecyclerView.OnScrollListener(),
                AbsListView.OnScrollListener {
            override fun onScroll(p0: AbsListView?, p1: Int, p2: Int, p3: Int) {
                if (p1 == 0) {
                    expenses_fab.visibility = View.VISIBLE
                } else {
                    if (p1 != buttonPosition) {
                        expenses_fab.visibility = View.GONE
                        buttonPosition = p1
                    }
                }
            }

            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {
                expenses_fab.visibility = View.VISIBLE
            }
        })
    }

    override fun onExpenseSuccess(webId: Long) {
        expenseWebId = webId
        tryToSynchronizeEvidences()
    }

    override fun onExpenseFailure(error: String) {
        Log.e(tag, "Expense failed: $error")
        UtilHelper().showView(expenses_progress, false)

        val snack = Snackbar.make(expenses_layout,
                getString(R.string.error_default), Snackbar.LENGTH_LONG)
        UtilHelper().showSnackBar(this, snack, UtilHelper.SNACK_TYPE_ERROR)
        itemStatus.isEnabled = true
    }

    override fun onEvidenceSuccess() {
        validateEvidenceNumber()
    }

    override fun onEvidenceFailure(error: String) {
        Log.e(tag, "Evidence failed: $error")
        // If one evidence fails, continue synchronizing the others
        validateEvidenceNumber()
    }
}