package com.arm.peliondevicemanagement.screens.fragments

import android.os.Bundle
import android.os.Message
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.arm.peliondevicemanagement.components.adapters.WorkflowAdapter
import com.arm.peliondevicemanagement.components.models.workflow.WorkflowModel
import com.arm.peliondevicemanagement.components.viewmodels.WorkflowViewModel
import com.arm.peliondevicemanagement.databinding.FragmentDashboardBinding
import com.arm.peliondevicemanagement.helpers.LogHelper
import com.arm.peliondevicemanagement.helpers.SharedPrefHelper
import com.arm.peliondevicemanagement.listeners.RecyclerItemClickListener
import com.arm.peliondevicemanagement.screens.activities.HostActivity

class DashboardFragment : Fragment(), RecyclerItemClickListener {

    companion object {
        private val TAG: String = DashboardFragment::class.java.simpleName
    }

    private var _viewBinder: FragmentDashboardBinding? = null
    private val viewBinder get() = _viewBinder!!

    private lateinit var workflowViewModel: WorkflowViewModel

    private var workflowAdapter: WorkflowAdapter? = null
    private var workflowModelsList = arrayListOf<WorkflowModel>()

    private val queryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(query: String?): Boolean = false

        override fun onQueryTextChange(newText: String?): Boolean {
            workflowAdapter!!.filter.filter(newText)
            return false
        }
    }

    private val refreshListener: androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener = androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener {
        refreshContent()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _viewBinder = FragmentDashboardBinding.inflate(inflater, container, false)
        return viewBinder.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        setupListeners()
        refreshContent()
    }

    private fun init() {
        workflowViewModel = ViewModelProvider(this).get(WorkflowViewModel::class.java)
        workflowAdapter = WorkflowAdapter(workflowModelsList, this)

        viewBinder.rvWorkflows.apply {
            layoutManager = LinearLayoutManager(context,
                RecyclerView.VERTICAL, false)
            itemAnimator = DefaultItemAnimator()
            adapter = workflowAdapter
        }
        resetSearchText()
    }

    private fun setupListeners() {
        viewBinder.swipeRefreshLayout.setOnRefreshListener(refreshListener)
        viewBinder.searchBar.searchTextBox.setOnQueryTextListener(queryTextListener)

        workflowViewModel.assignedWorkflowsLiveData.observe(viewLifecycleOwner, Observer { response ->
            if(response != null){
                LogHelper.debug(TAG, "onWorkflowsFetchSuccess(): $response")
                setSwipeRefreshStatus(false)

                if(response.workflows.isNotEmpty()){
                    workflowModelsList.clear()
                    workflowModelsList.addAll(response.workflows)
                    workflowAdapter!!.notifyDataSetChanged()
                    showHideSearchBar(true)
                } else {
                    showSnackbar("No jobs assigned")
                    showHide404View(true, "No jobs assigned")
                }
            } else {
                setSwipeRefreshStatus(false)
                showSnackbar("Failed to fetch workflows")
                showHide404View(true)
            }
        })

        // Using this just for testing purpose
        workflowViewModel.allWorkflowsLiveData.observe(viewLifecycleOwner, Observer { response ->
            if(response != null){
                LogHelper.debug(TAG, "onWorkflowsFetchSuccess(): $response")
                setSwipeRefreshStatus(false)

                if(response.workflows.isNotEmpty()){
                    workflowModelsList.clear()
                    workflowModelsList.addAll(response.workflows)
                    workflowAdapter!!.notifyDataSetChanged()
                    showHideSearchBar(true)
                } else {
                    showSnackbar("No workflows found")
                    showHide404View(true, "No workflows found")
                }
            } else {
                setSwipeRefreshStatus(false)
                showSnackbar("Failed to fetch workflows")
                showHide404View(true)
            }
        })
    }

    private fun setupData() {

    }

    private fun showSnackbar(message: String){
        (activity as HostActivity)
            .showSnackbar(viewBinder.root,
                message)
    }

    private fun refreshContent() {
        LogHelper.debug(TAG, "refreshContent()")

        workflowModelsList.clear()
        workflowAdapter!!.notifyDataSetChanged()

        showHideSearchBar(false)
        showHide404View(false)
        setSwipeRefreshStatus(true)
        workflowViewModel.getAssignedWorkflows()
    }

    private fun setSwipeRefreshStatus(isRefreshing: Boolean) {
        viewBinder.swipeRefreshLayout.isRefreshing = isRefreshing
    }

    private fun showHideSearchBar(visibility: Boolean) = if(visibility){
        viewBinder.searchBar.root.visibility = View.VISIBLE
    } else {
        viewBinder.searchBar.root.visibility = View.GONE
    }

    private fun resetSearchText() =
        viewBinder.searchBar.searchTextBox.setQuery("", false)

    private fun showHide404View(visibility: Boolean, message: String = "") {
        if (visibility) {
            viewBinder.notFoundView.root.visibility = View.VISIBLE

            if (message.isNotEmpty()) {
                viewBinder.notFoundView.errorText.text = message
            }
        } else {
            viewBinder.notFoundView.root.visibility = View.GONE
        }
    }

    override fun onItemClick(data: Any) {
        val model = data as WorkflowModel
        LogHelper.debug(TAG, "onItemClick()-> " +
                "workflowName: ${model.workflowName}, " +
                "workflowID: ${model.workflowID}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinder = null
    }
}
