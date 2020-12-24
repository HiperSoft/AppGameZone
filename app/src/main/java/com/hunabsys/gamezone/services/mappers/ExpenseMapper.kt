package com.hunabsys.gamezone.services.mappers

import android.util.Log
import com.hunabsys.gamezone.helpers.JsonValidationHelper
import com.hunabsys.gamezone.models.daos.EvidenceDao
import com.hunabsys.gamezone.models.daos.ExpenseDao
import com.hunabsys.gamezone.models.datamodels.Evidence
import com.hunabsys.gamezone.models.datamodels.Expense
import org.json.JSONArray
import org.json.JSONObject

class ExpenseMapper {

    private val tag = ExpenseMapper::class.simpleName

    companion object {
        var currentWebId: Long = 0
    }

    fun mapExpenses(expenses: JSONArray): Boolean {
        var result = true
        var success: Boolean
        var expense: JSONObject

        for (i in 0 until expenses.length()) {
            expense = expenses.getJSONObject(i)
            success = mapExpense(expense)

            if (!success) {
                result = false
            }
        }
        return result
    }

    private fun mapExpense(expense: JSONObject): Boolean {
        val id = JsonValidationHelper().getLongValue(expense, "mobile_id")
        val webId = JsonValidationHelper().getLongValue(expense, "web_id")
        val success = JsonValidationHelper().getBooleanValue(expense, "success")
        val error = JsonValidationHelper().getStringValue(expense, "error_message")

        if (success) {
            val expenseObject = ExpenseDao().findCopyById(id)
            updateExpense(expenseObject, webId)

            val evidence = expenseObject.evidence
            if (evidence != null) {
                updateEvidence(evidence, webId)
            }

            return true
        } else {
            if (error != "") {
                Log.e(tag, error)
            }
        }

        return false
    }

    private fun updateExpense(expense: Expense, webId: Long) {
        expense.webId = webId
        ExpenseDao().update(expense)

        val updateExpense = ExpenseDao().findCopyById(expense.id)
        Log.e(tag, "-------> Updated Expense with ID: " + updateExpense.id
                + ", synced: " + updateExpense.isSynchronized)
    }

    private fun updateEvidence(evidence: Evidence, webId: Long) {
        evidence.evidenceableId = webId

        EvidenceDao().update(evidence)

        currentWebId = webId
    }
}