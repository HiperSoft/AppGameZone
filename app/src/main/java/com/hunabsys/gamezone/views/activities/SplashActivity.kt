package com.hunabsys.gamezone.views.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.hunabsys.gamezone.R
import com.hunabsys.gamezone.helpers.AnimationHelper
import com.hunabsys.gamezone.helpers.PreferencesHelper
import com.hunabsys.gamezone.views.activities.login.LoginActivity
import kotlinx.android.synthetic.main.activity_splash.*

const val BAR_WIDTH = 4

const val PROGRESS_DELAY = 500L
const val NAVIGATION_DELAY = 2500L

class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        setUpProgressWheel()

        Handler().postDelayed({
            validateSession()
        }, NAVIGATION_DELAY)
    }

    private fun setUpProgressWheel() {
        splash_progress_wheel.barWidth = BAR_WIDTH
        splash_progress_wheel.stopSpinning()
        splash_progress_wheel.visibility = View.INVISIBLE

        Handler().postDelayed({
            splash_progress_wheel.spin()
            splash_progress_wheel.visibility = View.VISIBLE
        }, PROGRESS_DELAY)
    }

    private fun validateSession() {
        val activeSession = PreferencesHelper(this).activeSession

        val intent = if (activeSession) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }

        goToNextScreen(intent)
    }

    private fun goToNextScreen(intent: Intent) {
        startActivity(intent)
        finish()
        AnimationHelper().exitTransition(this)
    }
}
