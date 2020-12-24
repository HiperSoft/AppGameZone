package com.hunabsys.gamezone.views.adapters

import android.content.Context
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.views.fragments.SimpleFieldFragment
import com.hunabsys.gamezone.views.fragments.SimpleTextFragment
import com.stepstone.stepper.Step
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter

class ExpenseStepperAdapter(private val appContext: Context,
                            fragmentManager: android.support.v4.app.FragmentManager) :
        AbstractFragmentStepAdapter(fragmentManager, appContext) {

    companion object {
        const val NUMBER_OF_FRAGMENTS = 2
    }

    override fun getCount(): Int {
        return NUMBER_OF_FRAGMENTS
    }

    override fun createStep(position: Int): Step {
        val title: String?

        return when (position) {
            0 -> {
                title = appContext.getString(R.string.expenses_concept_instruction)
                SimpleTextFragment.newInstance(title, position)
            }

            1 -> {
                title = appContext.getString(R.string.expenses_amount_instruction)
                SimpleFieldFragment.newInstance(title, false, true, position)
            }

            else -> SimpleTextFragment()
        }
    }
}