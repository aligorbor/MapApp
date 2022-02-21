package ru.geekbrains.android2.mapapp.view

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import ru.geekbrains.android2.mapapp.R
import ru.geekbrains.android2.mapapp.databinding.CustomAlertDialogBinding
import ru.geekbrains.android2.mapapp.databinding.FragmentMarkersBinding
import ru.geekbrains.android2.mapapp.model.DB.MarkerObj
import ru.geekbrains.android2.mapapp.utils.showSnackBar
import ru.geekbrains.android2.mapapp.viewmodel.AppStateMarkers
import ru.geekbrains.android2.mapapp.viewmodel.MarkersViewModel

class MarkersFragment : Fragment() {
    private var _binding: FragmentMarkersBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MarkersViewModel

    var progressDialog: ProgressDialog? = null
    private var popupInputDialogView: View? = null
    private var titleInput: EditText? = null
    private var descriptionInput: EditText? = null
    private var saveMarkerButton: Button? = null
    private var cancelUserDataButton: Button? = null

    private val onItemViewClickListener = object : OnItemViewClickListener {
        override fun onItemViewClick(title: String) {
            openFragment(
                MapsFragment.newInstance(
                    true,
                    title.substringBefore(',').toDouble(),
                    title.substringAfter(',').toDouble(),
                )
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMarkersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MarkersViewModel::class.java).apply {
            getLiveData().observe(viewLifecycleOwner) {
                renderData(it)
            }
            getListOfMarkers()
        }
        progressDialog = ProgressDialog(requireContext())

        binding.fab.setOnClickListener { fabButtonView ->
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            alertDialogBuilder.setTitle("Create a Marker")
            alertDialogBuilder.setCancelable(true)
            initPopupViewControls()
            //We are setting our custom popup view by AlertDialog.Builder
            alertDialogBuilder.setView(popupInputDialogView)
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
            saveMarkerButton?.setOnClickListener { saveButtonView ->
                saveData(alertDialog)
            }
            cancelUserDataButton?.setOnClickListener { cancelButtonView ->
                alertDialog.cancel()
            }
        }
    }

    private fun renderData(appState: AppStateMarkers) {
        when (appState) {
            is AppStateMarkers.Success -> {
                progressDialog?.dismiss()
                initMarkersList(appState.markers)
            }
            is AppStateMarkers.Loading -> {
                progressDialog?.show()
            }
            is AppStateMarkers.Error -> {
                binding.root.showSnackBar(
                    appState.error.message ?: "",
                    getString(R.string.reload),
                    {
                        viewModel.getListOfMarkers()
                    })
            }
        }
    }

    private fun saveData(alertDialog: AlertDialog) {
        val marker = MarkerObj()
        if (titleInput?.text.toString().isNotEmpty() && descriptionInput?.text
                .toString().isNotEmpty()
        ) {
            alertDialog.cancel()
            marker.title = titleInput?.text.toString()
            marker.description = descriptionInput?.text.toString()
            viewModel.saveMarker(marker)
        } else {
            showAlert("Error", "Please enter a latitude,longtitude and description")
        }
    }

    private fun initMarkersList(list: List<MarkerObj>) {
        if (list.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            return
        }
        binding.emptyText.visibility = View.GONE

        val adapter = MarkersAdapter(
            list as ArrayList<MarkerObj>,
            onItemViewClickListener
        )

        adapter.onDeleteListener.observe(viewLifecycleOwner) { markerObj ->
            viewModel.deleteMarker(markerObj)
        }

        adapter.clickListenerToEdit.observe(viewLifecycleOwner) { markerObj ->
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            alertDialogBuilder.setTitle("Update a Marker")
            alertDialogBuilder.setCancelable(true)

            //We are initializing PopUp Views with title and description parameters of markerObj

            initPopupViewControls(
                markerObj.title,
                markerObj.description
            )

            alertDialogBuilder.setView(popupInputDialogView)
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()

            saveMarkerButton?.setOnClickListener { saveButtonView ->
                if (titleInput?.text.toString().isNotEmpty() && descriptionInput?.text.toString()
                        .isNotEmpty()
                ) {
                    alertDialog.cancel()
                    progressDialog?.show()
                    markerObj.title = titleInput?.text.toString()
                    markerObj.description = descriptionInput?.text.toString()
                    viewModel.saveMarker(markerObj)
                } else {
                    showAlert("Error", "Please enter a title and description")
                }
            }
            cancelUserDataButton?.setOnClickListener { cancelButtonView ->
                alertDialog.cancel()
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.setHasFixedSize(false)
        binding.recyclerView.adapter = adapter
    }


    private fun initPopupViewControls() {
        val bndng = CustomAlertDialogBinding.inflate(LayoutInflater.from(requireContext()))
        popupInputDialogView = bndng.root
        titleInput = bndng.titleInput
        descriptionInput = bndng.descriptionInput
        saveMarkerButton = bndng.buttonSaveTodo
        cancelUserDataButton = bndng.buttonCancelUserData
    }

    private fun initPopupViewControls(title: String, description: String) {
        val bndng = CustomAlertDialogBinding.inflate(LayoutInflater.from(requireContext()))
        popupInputDialogView = bndng.root
        titleInput = bndng.titleInput
        descriptionInput = bndng.descriptionInput
        saveMarkerButton = bndng.buttonSaveTodo
        cancelUserDataButton = bndng.buttonCancelUserData

        titleInput?.setText(title)
        descriptionInput?.setText(description)
    }

    private fun showAlert(title: String, message: String) {
        val builder = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, which ->
                dialog.cancel()
            }
        val ok = builder.create()
        ok.show()
    }

    private fun openFragment(fragment: Fragment) {
        activity?.supportFragmentManager?.apply {
            beginTransaction()
                .add(R.id.container, fragment)
                .addToBackStack("")
                .commitAllowingStateLoss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            MarkersFragment()
    }

    interface OnItemViewClickListener {
        fun onItemViewClick(title: String)
    }
}