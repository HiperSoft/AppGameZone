package com.hunabsys.gamezone.views.adapters

import android.content.Context
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.views.fragments.CommissionFragment
import com.hunabsys.gamezone.views.fragments.NegativeFieldFragment
import com.hunabsys.gamezone.views.fragments.SimpleFieldFragment
import com.hunabsys.gamezone.views.fragments.TakePhotoFragment
import com.stepstone.stepper.Step
import com.stepstone.stepper.adapter.AbstractFragmentStepAdapter

/**
 * Adapter for SaleRevision's stepper.
 * Created by Silvia Valdez on 2/16/18.
 */
class SaleRevisionStepperAdapter(private val appContext: Context,
                                 fragmentManager: android.support.v4.app.FragmentManager) :
        AbstractFragmentStepAdapter(fragmentManager, appContext) {

    companion object {
        const val NUMBER_OF_FRAGMENTS = 6
    }

    override fun getCount(): Int {
        return NUMBER_OF_FRAGMENTS
    }

    override fun createStep(position: Int): Step {
        val title: String?

        return when (position) {
            0 -> {
                title = appContext.getString(R.string.sale_revisions_entry_instruction)
                SimpleFieldFragment.newInstance(title, false, false, position)
            }

            1 -> {
                title = appContext.getString(R.string.sale_revisions_outcome_instruction)
                TakePhotoFragment.newInstance(title, false, position)
            }

            2 -> {
                title = appContext.getString(R.string.sale_revisions_screen_instruction)
                NegativeFieldFragment.newInstance(title, position)
            }

            3 -> {
                title = appContext.getString(R.string.sale_revisions_taken_instruction)
                SimpleFieldFragment.newInstance(title, false, true, position)
            }

            4 -> {
                title = appContext.getString(R.string.sale_revisions_commission_instruction)
                CommissionFragment.newInstance(title, position)
            }

            5 -> {
                title = appContext.getString(R.string.sale_revisions_current_fund_instruction)
                SimpleFieldFragment.newInstance(title, true, true, position)
            }

            else -> SimpleFieldFragment()
        }
    }
}