package com.hunabsys.gamezone.models.daos

import android.util.Log
import com.hunabsys.gamezone.models.datamodels.Expense
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort

/**
 * Expense model
 * Created by Jonathan Hernandez on 03/08/2018
 */
class ExpenseDao {

    private val tag = ExpenseDao::class.java.simpleName
    private val realm = Realm.getDefaultInstance()

    fun create(expense: Expense) {
        try {
            realm.executeTransaction {
                val expenseObject = realm.createObject(Expense::class.java, getId())
                expenseObject.webId = expense.webId
                expenseObject.userId = expense.userId
                expenseObject.pointOfSaleId = expense.pointOfSaleId

                expenseObject.concept = expense.concept
                expenseObject.amount = expense.amount
                expenseObject.comments = expense.comments

                expenseObject.week = expense.week
                expenseObject.createdAt = expense.createdAt

                expenseObject.evidence = expense.evidence
                expenseObject.isSynchronized = expense.isSynchronized
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to create a Expense", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun update(expense: Expense) {
        try {
            realm.executeTransaction {
                val expenseObject = findById(expense.id)
                expenseObject.webId = expense.webId
                expenseObject.userId = expense.userId
                expenseObject.pointOfSaleId = expense.pointOfSaleId

                expenseObject.concept = expense.concept
                expenseObject.amount = expense.amount
                expenseObject.comments = expense.comments

                expenseObject.week = expense.week
                expenseObject.createdAt = expense.createdAt

                //expenseObject.evidence = expense.evidence
                expenseObject.isSynchronized = expense.isSynchronized
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to update a Expense", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    private fun findById(id: Long): Expense {
        return realm.where(Expense::class.java)
                .equalTo("id", id)
                .findFirst()!!
    }

    fun findCopyById(id: Long): Expense {
        val expense = realm.where(Expense::class.java)
                .equalTo("id", id)
                .findFirst()!!
        return realm.copyFromRealm(expense)
    }

    private fun findAll(): RealmResults<Expense> {
        return realm.where(Expense::class.java)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun findAll(userId: Long): RealmResults<Expense> {
        return realm.where(Expense::class.java)
                .equalTo("userId", userId)
                .findAll()
                .sort("id", Sort.DESCENDING)
    }

    fun deleteAll(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(Expense::class.java)
                        .equalTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all Expenses", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun deleteAllOtherUsers(userId: Long) {
        try {
            realm.executeTransaction {
                realm.where(Expense::class.java)
                        .notEqualTo("userId", userId)
                        .findAll()
                        .deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all Expenses", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    private fun getId(): Long {
        return if (findAll().size != 0) {
            val lastId = findAll().first()!!.id
            lastId + 1
        } else {
            1
        }
    }
}