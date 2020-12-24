package com.hunabsys.gamezone.models.daos

import android.util.Log
import com.hunabsys.gamezone.models.datamodels.GameMachine
import com.rollbar.android.Rollbar
import io.realm.Realm
import io.realm.RealmResults

/**
 * Data access object for GameMachine.
 * Created by Silvia Valdez on 11/02/2018.
 */
class GameMachineDao {

    private val tag = GameMachineDao::class.simpleName
    private val realm = Realm.getDefaultInstance()

    private fun create(gameMachine: GameMachine) {
        try {
            realm.executeTransaction {
                val gameMachineObject = realm.createObject(GameMachine::class.java, gameMachine.id)
                gameMachineObject.folio = gameMachine.folio
                gameMachineObject.realFund = gameMachine.realFund
                gameMachineObject.week = gameMachine.week
                gameMachineObject.lastEntry = gameMachine.lastEntry
                gameMachineObject.lastOutcome = gameMachine.lastOutcome
            }
        } catch (ex: IllegalArgumentException) {
            Log.e(tag, "Attempting to create a GameMachine", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun findById(id: Long): GameMachine {
        var gameMachine = GameMachine()
        try {
            gameMachine = realm.where(GameMachine::class.java).equalTo("id", id)
                    .findFirst()!!
        } catch (ex: KotlinNullPointerException) {
            Log.e(tag, "Attempting to find GameMachine", ex)
            Rollbar.instance().critical(ex, tag)
        }
        return gameMachine
    }

    fun findByFolio(folio: String): GameMachine {
        return realm.where(GameMachine::class.java).equalTo("folio", folio).findFirst()!!
    }

    fun findAll(): RealmResults<GameMachine> {
        return realm.where(GameMachine::class.java).findAll()!!
    }

    fun deleteAll() {
        try {
            realm.executeTransaction {
                val gameMachines = findAll()
                gameMachines.deleteAllFromRealm()
            }
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to delete all GameMachines", ex)
            Rollbar.instance().critical(ex, tag)
        }
    }

    fun saveGameMachine(gameMachine: GameMachine): GameMachine? {
        var savedMachine: GameMachine? = null
        create(gameMachine)

        try {
            savedMachine = findById(gameMachine.id)
            Log.d(tag, "GameMachine is saved: " + savedMachine.toString())
        } catch (ex: KotlinNullPointerException) {
            Log.e(tag, "Null GameMachine", ex)
            Rollbar.instance().critical(ex, tag)
        }

        return savedMachine
    }
}