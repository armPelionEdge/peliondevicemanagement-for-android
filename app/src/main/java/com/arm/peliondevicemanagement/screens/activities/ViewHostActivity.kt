/*
 * Copyright (c) 2018, Arm Limited and affiliates.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arm.peliondevicemanagement.screens.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavArgument
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.arm.peliondevicemanagement.R
import com.arm.peliondevicemanagement.constants.AppConstants.VIEW_HOST_LAUNCH_GRAPH
import com.arm.peliondevicemanagement.constants.AppConstants.WORKFLOW_ID_ARG
import com.arm.peliondevicemanagement.constants.AppConstants.viewHostLaunchActionList
import com.arm.peliondevicemanagement.databinding.ActivityViewHostActivityBinding

class ViewHostActivity : BaseActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var navigationController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var viewBinder: ActivityViewHostActivityBinding

    private lateinit var launchAction: String
    private var isJobRunGraph: Boolean = false

    companion object {
        private val TAG: String = ViewHostActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinder = ActivityViewHostActivityBinding.inflate(layoutInflater)
        initTheme(false)
        setContentView(viewBinder.root)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        launchAction = intent.getStringExtra(VIEW_HOST_LAUNCH_GRAPH)!!
        init()
    }

    private fun init() {
        toolbar = viewBinder.toolbar
        setupToolbar(toolbar, launchAction)

        navigationController = Navigation
            .findNavController(this, R.id.nav_host_fragment)
        val navGraph = navigationController.navInflater.inflate(R.navigation.view_host_nav_graph)
        when(launchAction){
            viewHostLaunchActionList[0] -> {
                navGraph.startDestination = R.id.jobFragment
                val args = NavArgument.Builder()
                    .setDefaultValue(intent.getStringExtra(WORKFLOW_ID_ARG))
                    .build()
                navGraph.addArgument(WORKFLOW_ID_ARG, args)
            }
            viewHostLaunchActionList[1] -> {
                navGraph.startDestination = R.id.settingsFragment
            }
        }
        navigationController.graph = navGraph
        appBarConfiguration = AppBarConfiguration.Builder().build()
        NavigationUI.setupActionBarWithNavController(this, navigationController, appBarConfiguration)

        setupListeners()
    }

    private fun setupListeners() {
        navigationController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id){
                R.id.jobFragment -> {
                    updateToolbarText("Job")
                    isJobRunGraph = false
                }
                R.id.jobRunFragment -> {
                    updateToolbarText("Job Run")
                    isJobRunGraph = true
                }
                R.id.settingsFragment -> {
                    updateToolbarText("Settings")
                }
                R.id.activityInfoFragment -> {
                    updateToolbarText("Login History")
                }
                R.id.helpAndSupportFragment -> {
                    updateToolbarText("Help & Support")
                }
                R.id.licensesFragment -> {
                    updateToolbarText("Libraries we use")
                }
                R.id.webViewFragment -> {
                    updateToolbarText("In-App Browsing")
                }
                R.id.developerOptionsFragment -> {
                    updateToolbarText("Developer Options")
                }
            }
        }
    }

    private fun updateToolbarText(titleText: String) {
        toolbar.apply {
            title = titleText
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return processNavigation()
    }

    override fun onBackPressed() {
        processNavigation()
    }

    private fun processNavigation(): Boolean {
        return if(isJobRunGraph) {
            onBackPressedDispatcher.onBackPressed()
            true
        } else {
            if(!NavigationUI.navigateUp(navigationController, appBarConfiguration)){
                navigateBackToHomeActivity()
            }
            return true
        }
    }

    private fun navigateBackToHomeActivity() {
        fireIntentWithFinish(Intent(this, HomeActivity::class.java), false)
    }

}