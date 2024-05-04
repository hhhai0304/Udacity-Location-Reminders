package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig.APPLICATION_ID
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.Locale

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    companion object {
        private const val REQUEST_CODE_LOCATION_PERMISSION = 1001
    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap
    private var poi: PointOfInterest? = null
    private var marker1: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        val mapFrag = childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment
        mapFrag.getMapAsync(this)
        binding.btnSaveLocation.setOnClickListener { onLocationSelected() }
        return binding.root
    }

    private fun onLocationSelected() {
        if (poi != null) {
            _viewModel.setPOI(poi)
            findNavController().popBackStack()
        } else {
            Toast.makeText(context, "Please select location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                _viewModel.showSnackBar.postValue(getString(R.string.permission_denied_explanation))
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
            } else {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_CODE_LOCATION_PERMISSION
                )
            }
            return
        }

        map.isMyLocationEnabled = true
        val requestLocation =
            LocationRequest.create().apply { priority = LocationRequest.PRIORITY_LOW_POWER }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(requestLocation)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener {
            if (it !is ResolvableApiException) {
                Toast.makeText(
                    requireContext(),
                    "Please enable Location services",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                startIntentSenderForResult(
                    it.resolution.intentSender,
                    1002, null,
                    0, 0, 0, null
                )
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        val zoom = 15f
                        val home = LatLng(loc.latitude, loc.longitude)
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, zoom))
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setMapStyle(
            MapStyleOptions.loadRawResourceStyle(
                requireContext(),
                R.raw.map_style
            )
        )
        map.setOnPoiClickListener {
            marker1?.remove()
            val marker = map.addMarker(MarkerOptions().position(it.latLng).title(it.name))
            marker?.showInfoWindow()
            poi = it
            marker1 = marker

        }
        map.setOnMapClickListener {
            val address =
                Geocoder(
                    requireContext(),
                    Locale.getDefault()
                ).getFromLocation(it.latitude, it.longitude, 1)
            if (!address.isNullOrEmpty()) {
                val addressLine: String = address[0].getAddressLine(0)
                val addressPOI = PointOfInterest(it, "", addressLine)
                marker1?.remove()
                val marker = map.addMarker(
                    MarkerOptions().position(addressPOI.latLng).title(addressPOI.name)
                )
                marker?.showInfoWindow()
                marker1 = marker
                poi = addressPOI
            }
        }
        getCurrentLocation()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if ((PackageManager.PERMISSION_GRANTED ==
                    ContextCompat.checkSelfPermission(
                        requireActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ))
        ) {
            getCurrentLocation()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Snackbar.make(
                requireView(),
                getString(R.string.require_location),
                Snackbar.LENGTH_SHORT
            )
                .setAction(getString(R.string.enable_location)) {
                    val permissionArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                    requestPermissions(permissionArray, REQUEST_CODE_LOCATION_PERMISSION)
                }.show()
        } else {
            Snackbar.make(requireView(), "location denied", Snackbar.LENGTH_SHORT)
                .setAction("change Permission") {
                    startActivity(Intent().apply {
                        data =
                            Uri.fromParts("package", APPLICATION_ID, null)

                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS

                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                }.show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }

        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }

        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }

        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}