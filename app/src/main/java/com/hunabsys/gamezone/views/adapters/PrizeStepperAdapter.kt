package com.hunabsys.gamezone.views.adapters

import android.content.Context
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.views.fragments.SimpleFieldFragment
import com.hunabsys.gamezone.views.fragments.TakePhotoFragment
import com.stepstone.stepper.Step
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter

/**
 * Adapter for Prize's stepper.
 * Created by Jonathan Hernandez on 7/25/18.
 */
class PrizeStepperAdapter(private val appContext: Context,
                          fragmentManager: android.support.v4.app.FragmentManager) :
        AbstractFragmentStepAdapter(fragmentManager, appContext) {

    companion object {
        const val NUMBER_OF_FRAGMENTS = 8
    }

    override fun getCount(): Int {
        return NUMBER_OF_FRAGMENTS
    }

    override fun createStep(position: Int): Step {
        val title: String?

        return when (position) {
            0 -> {
                title = appContext.getString(R.string.prizes_entry_instruction)
                SimpleFieldFragment.newInstance(title, false, false, position)
            }

            1 -> {
                title = appContext.getString(R.string.prizes_outcome_instruction)
                TakePhotoFragment.newInstance(title, false, position)
            }

            2 -> {
                title = appContext.getString(R.string.prizes_screen_instruction)
                TakePhotoFragment.newInstance(title, false, position)
            }

            3 -> {
                title = appContext.getString(R.string.prizes_prize_instruction)
                SimpleFieldFragment.newInstance(title, false, true, position)
            }

            4 -> {
                title = appContext.getString(R.string.prizes_inside_machine_instruction)
                SimpleFieldFragment.newInstance(title, false, true, position)
            }

            5 -> {
                title = appContext.getString(R.string.prizes_complete_instruction)
                SimpleFieldFragment.newInstance(title, false, true, position)
            }

            6 -> {
                title = appContext.getString(R.string.prizes_current_fund_instruction)
                SimpleFieldFragment.newInstance(title, true, true, position)
            }

            7 -> {
                title = appContext.getString(R.string.prizes_expense_instruction)
                SimpleFieldFragment.newInstance(title, false, true, position)
            }

            else -> SimpleFieldFragment()
        }
    }
}