package com.example.ojeksyaririder.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.ojeksyaririder.R
import com.example.ojeksyaririder.callback.IFirebaseDriverInfoListener
import com.example.ojeksyaririder.callback.IFirebaseFailedListener
import com.example.ojeksyaririder.databinding.FragmentHomeBinding
import com.example.ojeksyaririder.utils.Common
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var binding: FragmentHomeBinding

    private lateinit var mMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment

    private var firstTime: Boolean = true

    //Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var cityName: String

    //Load Driver
    private var distance : Double = 1.0 //default in km
    private val LIMIT_RANGE: Double = 10.0 // km
    private lateinit var previousLocation: Location
    private lateinit var currentLocation: Location

    //listener
    private lateinit var iFirebaseDriverInfoListener: IFirebaseDriverInfoListener
    private lateinit var iFirebaseFailedListener: IFirebaseFailedListener

    override fun onDestroyView() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        init()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    private fun init() {

        iFirebaseDriverInfoListener = this
        iFirebaseFailedListener = this

        locationRequest = LocationRequest()
            .setSmallestDisplacement(10f)
            .setInterval(5000)
            .setFastestInterval(3000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val newPosition = LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition, 18f))

                //If user has change Location, calculate and load driver again
                if (firstTime){
                    previousLocation = locationResult.lastLocation
                    currentLocation = previousLocation
                    firstTime = false
                } else {
                    previousLocation = currentLocation
                    currentLocation = locationResult.lastLocation
                }

                if (previousLocation.distanceTo(currentLocation)/1000 <= LIMIT_RANGE)
                    loadAvailableDrivers()


            }

            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())

    }

    override fun onMapReady(googleMap: GoogleMap) {
        //Request permission to add current location
        mMap = googleMap
        Dexter.withContext(context)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener, GoogleMap.OnMyLocationClickListener,
                GoogleMap.OnMyLocationButtonClickListener {
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return
                    }

                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationButtonClickListener(this)
                    mMap.setOnMyLocationClickListener(this)

                    //CurrentLocation Position
                    val locationButton: View? = (mapFragment.view?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(Integer.parseInt("2"))
                        .findViewById(Integer.parseInt("2"))
                    val params: RelativeLayout.LayoutParams = locationButton?.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.setMargins(0,0,0,50)
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {
                    TODO("Not yet implemented")
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Snackbar.make(binding.root, p0?.permissionName.toString() + " need enable", Snackbar.LENGTH_SHORT).show()
                }

                override fun onMyLocationButtonClick(): Boolean {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return false
                    }
                    fusedLocationProviderClient.lastLocation
                        .addOnFailureListener { err ->
                            Snackbar.make(binding.root, err.message.toString(), Snackbar.LENGTH_SHORT).show()
                        }
                        .addOnSuccessListener { location ->
                            val userLatlng = LatLng(location.latitude, location.longitude)
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatlng, 18f))
                        }

                    return true
                }

                override fun onMyLocationClick(p0: Location) {
                    Toast.makeText(context, "MyLocation button clicked", Toast.LENGTH_SHORT)
                        .show()
                }

            }).check()

        try {
            var success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.uber_maps_style))
            if (!success){
                Log.d("OJEK_ERROR", resources.getString(R.string.map_style_error))
            }

        } catch (e: Resources.NotFoundException){
            Log.d("OJEK_ERROR", e.message.toString())
        }
    }

    private fun loadAvailableDrivers() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            view?.let { Snackbar.make(it, resources.getString(R.string.permissoin_location_require), Snackbar.LENGTH_SHORT).show() }
            return
        }
        fusedLocationProviderClient.lastLocation
            .addOnFailureListener { e ->
                view?.let { Snackbar.make(it, e.message.toString(), Snackbar.LENGTH_SHORT).show() }
            }
            .addOnSuccessListener { location ->
                val geoCoder: Geocoder = Geocoder(context, Locale.getDefault())
                var addressList: List<Address>
                try {
                    addressList = geoCoder.getFromLocation(location.latitude, location.longitude, 1)
                    cityName = addressList.get(0).locality

                    //Query
                    var driver_location_ref = FirebaseDatabase.getInstance()
                        .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                        .child(cityName)

                    val geoFire: GeoFire = GeoFire(driver_location_ref)
                    val geoQuery: GeoQuery = geoFire.queryAtLocation(GeoLocation(location.latitude, location.longitude), distance )
                    geoQuery.removeAllListeners()

                    geoQuery.addGeoQueryEventListener(object: GeoQueryEventListener{
                        override fun onGeoQueryReady() {
                            TODO("Not yet implemented")
                        }

                        override fun onKeyEntered(key: String?, location: GeoLocation?) {
                            Common.driversFound.
                        }

                        override fun onKeyMoved(key: String?, location: GeoLocation?) {
                            TODO("Not yet implemented")
                        }

                        override fun onKeyExited(key: String?) {
                            TODO("Not yet implemented")
                        }

                        override fun onGeoQueryError(error: DatabaseError?) {
                            TODO("Not yet implemented")
                        }

                    })


                } catch (e: IOException){
                    e.printStackTrace()
                    view?.let { Snackbar.make(it, e.message.toString(), Snackbar.LENGTH_SHORT).show() }
                }
            }
    }
}