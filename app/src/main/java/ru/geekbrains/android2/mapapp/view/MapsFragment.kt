package ru.geekbrains.android2.mapapp.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import ru.geekbrains.android2.mapapp.R
import ru.geekbrains.android2.mapapp.databinding.FragmentMapsContentBinding
import ru.geekbrains.android2.mapapp.model.DB.MarkerObj
import ru.geekbrains.android2.mapapp.utils.showSnackBar
import ru.geekbrains.android2.mapapp.viewmodel.AppStateMaps
import ru.geekbrains.android2.mapapp.viewmodel.MapsViewModel
import java.io.IOException

class MapsFragment : Fragment() {
    private val locationPermissionReqCode = 2277
    private lateinit var map: GoogleMap
    private lateinit var viewModel: MapsViewModel
    private lateinit var rad: RouteAnimateAdapter
    private var _binding: FragmentMapsContentBinding? = null
    private val binding get() = _binding!!
    var progressDialog: ProgressDialog? = null
    private var moveCameraAfterLocationChanged: Boolean = true
    private var latLngFrom: LatLng? = null
    private var latLngTo: LatLng? = null
    private val onLocationListener = object : LocationListener {
        override fun onLocationChanged(locationChanged: Location) {
            if (moveCameraAfterLocationChanged)
                map.animateCamera(
                    CameraUpdateFactory.newLatLng(
                        LatLng(
                            locationChanged.latitude,
                            locationChanged.longitude
                        )
                    )
                )
            moveCameraAfterLocationChanged = true
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.setOnMapClickListener {
            getAddressAsync(it)
        }
        map.setOnMapLongClickListener {
            getAddressAsync(locationAsync = it, saveToDB = false, route = true)
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }
        checkPermission()
        arguments?.let {
            val lat = it.getDouble(ARG_LAT)
            val long = it.getDouble(ARG_LONG)
            if (it.getBoolean(ARG_MARKER_SET)) {
                moveCameraAfterLocationChanged = false
                getAddressAsync(LatLng(lat, long), false)
            }
        }
        rad = RouteAnimateAdapter(requireContext())
        rad.setOnAnimateListener(object : RouteAnimateAdapter.OnAnimateListener {
            override fun onFinish() {
                binding.textProgress.visibility = View.GONE
                latLngFrom = null
                latLngTo = null
            }

            override fun onStart() {
                binding.textProgress.visibility = View.VISIBLE
            }

            override fun onProgress(progress: Int, total: Int) {
                binding.textProgress.text = "$progress / $total"
            }
        })
    }

    private fun showCar(polyPoints: List<LatLng>) {
        var mo = MarkerOptions()
        mo.icon(BitmapDescriptorFactory.fromResource(R.drawable.car3r1))
        rad.animateDirection(
            map, ArrayList(polyPoints), 2, true,
            false, true, true, mo, true, true, null
        )
    }

    private fun showRoute(polyPoints: List<LatLng>) {
        // declare bounds object to fit whole route in screen
        val LatLongB = LatLngBounds.Builder()
        // Declare polyline object and set up color and width
        val options = PolylineOptions()
        options.color(Color.RED)
        options.width(5f)
        // Add  points to polyline and bounds
        options.add(latLngFrom)
        LatLongB.include(latLngFrom!!)
        for (point in polyPoints) {
            options.add(point)
            LatLongB.include(point)
        }
        options.add(latLngTo)
        LatLongB.include(latLngTo!!)
        // build bounds
        val bounds = LatLongB.build()
        ///    add polyline to the map
        map.addPolyline(options)
        // show map with route centered
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        latLngFrom = null
        latLngTo = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(MapsViewModel::class.java).apply {
            getLiveData().observe(viewLifecycleOwner) {
                renderData(it)
            }
        }
        progressDialog = ProgressDialog(requireContext())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun renderData(appState: AppStateMaps) {
        when (appState) {
            is AppStateMaps.SuccessMarker -> {
                progressDialog?.dismiss()
            }
            is AppStateMaps.SuccessDirection -> {
                progressDialog?.dismiss()
                if (MainActivity.showCar) {
                    showCar(appState.polyPoints)
                } else {
                    showRoute(appState.polyPoints)
                }
            }
            is AppStateMaps.Loading -> {
                progressDialog?.show()
            }
            is AppStateMaps.Error -> {
                binding.root.showSnackBar(
                    appState.error.message ?: "Error saving marker"
                )
            }
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionReqCode
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val provider = locationManager.getProvider(LocationManager.GPS_PROVIDER)
            provider?.let {
                // Будем получать геоположение через каждые 50 секунд или каждые 10 метров
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    onLocationListener
                )
            }
        } else {
            val location =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.looks_like_location_disabled),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                map.animateCamera(
                    CameraUpdateFactory.newLatLng(
                        LatLng(
                            location.latitude,
                            location.longitude
                        )
                    )
                )
            }
        }
    }

    private fun getAddressAsync(
        locationAsync: LatLng,
        saveToDB: Boolean = true,
        route: Boolean = false
    ) {
        context?.let {
            val geoCoder = Geocoder(it)
            Thread {
                try {
                    val address =
                        geoCoder.getFromLocation(locationAsync.latitude, locationAsync.longitude, 1)
                    Handler(Looper.getMainLooper()).post {
                        if (route) setRouteMarker(locationAsync, address[0].getAddressLine(0))
                        else setMarker(locationAsync, address[0].getAddressLine(0), saveToDB)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    private fun setMarker(latLng: LatLng, title: String, saveToDB: Boolean = true) {
        map.clear()
        //  map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        map.addMarker(MarkerOptions().position(latLng).title(title))
        if (saveToDB) {
            val marker = MarkerObj()
            marker.title = "${latLng.latitude},${latLng.longitude}"
            marker.description = title
            marker.lat = latLng.latitude
            marker.long = latLng.longitude
            viewModel.saveMarker(marker)
        }
    }

    private fun setRouteMarker(latLng: LatLng, title: String) {
        if (latLngFrom == null && latLngTo == null) {
            map.clear()
            latLngFrom = latLng
            //  map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngFrom!!, 15f))
            map.addMarker(MarkerOptions().position(latLngFrom!!).title(title))
        } else if (latLngFrom != null && latLngTo == null) {
            latLngTo = latLng
            map.addMarker(MarkerOptions().position(latLngTo!!).title(title))
            viewModel.getDirection(latLngFrom!!, latLngTo!!, getString(R.string.google_maps_key))
        } else if (latLngFrom != null && latLngTo != null) {
            rad.cancelAnimated()
            latLngFrom = null
            latLngTo = null
            map.clear()
        }
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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val MIN_TIME_MS = 10000L
        private const val MIN_DISTANCE_M = 10f
        private const val ARG_LAT = "latitude"
        private const val ARG_LONG = "longtitude"
        private const val ARG_MARKER_SET = "markerset"

        fun newInstance(
            markerSet: Boolean = false,
            latitude: Double = 0.0,
            longtitude: Double = 0.0
        ) =
            MapsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_MARKER_SET, markerSet)
                    putDouble(ARG_LAT, latitude)
                    putDouble(ARG_LONG, longtitude)
                }
            }
    }
}