package com.hunabsys.gamezone.views.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper

/**
 * Created by Silvia Valdez on 1/26/18.
 */
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expenses)

        setUpActionBar()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        AnimationHelper().exitTransition(this)
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        AnimationHelper().exitTransition(this)
    }

    private fun setUpActionBar() {
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}