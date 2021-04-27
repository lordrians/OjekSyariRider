package com.example.ojeksyaririder.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
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
import com.example.ojeksyaririder.model.AnimationModel
import com.example.ojeksyaririder.model.DriverGeoModel
import com.example.ojeksyaririder.model.DriverInfoModel
import com.example.ojeksyaririder.model.GeoQueryModel
import com.example.ojeksyaririder.remote.IGoogleAPI
import com.example.ojeksyaririder.remote.RetrofitClient
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
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.*
import kotlin.collections.HashSet

class HomeFragment : Fragment(), OnMapReadyCallback, IFirebaseDriverInfoListener,
    IFirebaseFailedListener {

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

    //
    private var compositeDisposable: CompositeDisposable = CompositeDisposable()
    private lateinit var iGoogleAPI: IGoogleAPI

    override fun onDestroyView() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroyView()
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
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

        loadAvailableDrivers()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        //Request permission to add current location
        mMap = googleMap
        Dexter.withContext(context)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener, GoogleMap.OnMyLocationClickListener,
                GoogleMap.OnMyLocationButtonClickListener {
                @SuppressLint("ResourceType")
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
                    mMap.uiSettings.isZoomControlsEnabled = true
                    mMap.setOnMyLocationButtonClickListener(this)
                    mMap.setOnMyLocationClickListener(this)

                    //CurrentLocation Position
                    val locationButton: View? = (mapFragment.view?.findViewById<View>(Integer.parseInt("1"))?.parent as View).findViewById<View>(Integer.parseInt("2"))
                        .findViewById(Integer.parseInt("2"))
                    val params: RelativeLayout.LayoutParams = locationButton?.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.setMargins(0,0,0,50)

                    //Control Zoom Position
                    val zoomControl: View? = mapFragment.view?.findViewById(0x1)
                    val paramsZoom: RelativeLayout.LayoutParams = zoomControl?.layoutParams as RelativeLayout.LayoutParams
                    paramsZoom.addRule(RelativeLayout.ALIGN_PARENT_TOP,0)
                    paramsZoom.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    paramsZoom.setMargins(0,0,0,170)
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
                    var driverLocationRef = FirebaseDatabase.getInstance()
                        .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                        .child(cityName)

                    val geoFire: GeoFire = GeoFire(driverLocationRef)
                    val geoQuery: GeoQuery = geoFire.queryAtLocation(GeoLocation(location.latitude, location.longitude), distance )
                    geoQuery.removeAllListeners()

                    geoQuery.addGeoQueryEventListener(object: GeoQueryEventListener{
                        override fun onGeoQueryReady() {
                            if (distance <= LIMIT_RANGE){
                                distance++
                                loadAvailableDrivers() //Continue search in new distance
                            } else {
                                distance = 1.0 // reset it
                                addDriverMarker()
                            }
                        }

                        override fun onKeyEntered(key: String, location: GeoLocation) {
                            Common.driversFound.add(DriverGeoModel(key, location))
                        }

                        override fun onKeyMoved(key: String?, location: GeoLocation?) {
                            TODO("Not yet implemented")
                        }

                        override fun onKeyExited(key: String?) {
                            Log.d("ONCHILDREMOVED", key.toString())
                        }

                        override fun onGeoQueryError(error: DatabaseError) {
                            view?.let { Snackbar.make(it, error.message, Snackbar.LENGTH_LONG).show() }
                        }

                    })

                    driverLocationRef.addChildEventListener(object: ChildEventListener{
                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }

                        override fun onChildMoved(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            TODO("Not yet implemented")
                        }

                        override fun onChildChanged(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            TODO("Not yet implemented")
                        }

                        override fun onChildAdded(
                            snapshot: DataSnapshot,
                            previousChildName: String?
                        ) {
                            val geoQueryModel: GeoQueryModel? = snapshot.getValue(GeoQueryModel::class.java)
                            val geoLocation = GeoLocation(geoQueryModel!!.l[0],geoQueryModel.l[1])
                            val driverGeoModel = DriverGeoModel(snapshot.key!!, geoLocation)
                            var newDriverLocation = Location("")
                            newDriverLocation.latitude = geoLocation.latitude
                            newDriverLocation.longitude = geoLocation.longitude
                            var newDistance = location.distanceTo(newDriverLocation) / 1000
                            if (newDistance <= LIMIT_RANGE)
                                findDriverByKey(driverGeoModel) // if driver in range , add to map

                        }

                        override fun onChildRemoved(snapshot: DataSnapshot) {
                        }
                    })

                } catch (e: IOException){
                    e.printStackTrace()
                    view?.let { Snackbar.make(it, e.message.toString(), Snackbar.LENGTH_SHORT).show() }
                }
            }
    }

    @SuppressLint("CheckResult")
    private fun addDriverMarker() {
        if (Common.driversFound.size > 0){
            Observable.fromIterable(Common.driversFound)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { driverGeoModel ->
                    findDriverByKey(driverGeoModel)
                }
        } else {
            view?.let { Snackbar.make(it, resources.getString(R.string.drivers_not_found), Snackbar.LENGTH_SHORT).show() }
        }
    }

    private fun findDriverByKey(driverGeoModel: DriverGeoModel?) {
        driverGeoModel?.getKey()?.let {
            FirebaseDatabase.getInstance()
                .getReference(Common.DRIVER_INFO_REFERENCE)
                .child(it)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onCancelled(error: DatabaseError) {
                        iFirebaseFailedListener.onFirebaseLoadFailed(error.message)
                    }

                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.hasChildren()){
                            driverGeoModel.setDriverInfoModel(snapshot.getValue(DriverInfoModel::class.java)!!)
                            iFirebaseDriverInfoListener.onDriverInfoLoadSuccess(driverGeoModel)
                        } else {
                            iFirebaseFailedListener.onFirebaseLoadFailed(resources.getString(R.string.not_found_driver_key) + driverGeoModel.getKey())
                        }
                    }
                })
        }
    }

    override fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel) {
        //If already have marker with this key, doesn't set again
        if (!Common.markerList.containsKey(driverGeoModel.getKey())){
            Common.markerList.put(driverGeoModel.getKey(),
            mMap.addMarker(MarkerOptions()
                .position(LatLng(driverGeoModel.getGeoLocation().latitude, driverGeoModel.getGeoLocation().longitude))
                .flat(true)
                .title(Common.buildName(driverGeoModel.getDriverinfoModel()?.firstName,
                driverGeoModel.getDriverinfoModel()?.lastName))
                .snippet(driverGeoModel.getDriverinfoModel()?.phoneNumber)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))))
        }

        if (!TextUtils.isEmpty(cityName)){
            val driverLocation = FirebaseDatabase.getInstance()
                .getReference(Common.DRIVERS_LOCATION_REFERENCES)
                .child(cityName)
                .child(driverGeoModel.getKey())
            driverLocation.addValueEventListener(object: ValueEventListener{
                override fun onCancelled(error: DatabaseError) {
                    view?.let { Snackbar.make(it, error.message, Snackbar.LENGTH_SHORT).show() }
                }

                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.hasChildren()){
                        if (Common.markerList.get(driverGeoModel.getKey()) != null){
                            Common.markerList.get(driverGeoModel.getKey())!!.remove() // remove marker
                        }
                        Common.markerList.remove(driverGeoModel.getKey()) // Remove marker info from hash map
                        driverLocation.removeEventListener(this)
                    } else {
                        if (Common.markerList.get(driverGeoModel.getKey()) != null){
                            val geoQueryModel = snapshot.getValue(GeoQueryModel::class.java)
                            val animationModel = AnimationModel(false, geoQueryModel)
                            if (Common.driverLocationSubscribe.get(driverGeoModel.getKey()) != null){
                                var currentMarker: Marker? = Common.markerList.get(driverGeoModel.getKey())
                                var oldPosition: AnimationModel? = Common.driverLocationSubscribe.get(driverGeoModel.getKey())

                                var from = StringBuilder()
                                    .append(oldPosition?.geoQueryModel?.l?.get(0))
                                    .append(",")
                                    .append(oldPosition.geoQueryModel?.l?.get(1))
                                    .toString()

                                var to = StringBuilder()
                                    .append(animationModel.geoQueryModel?.l?.get(0))
                                    .append(",")
                                    .append(animationModel.geoQueryModel?.l?.get(1))
                                    .toString()

                                moveMarkerAnimation(driverGeoModel.getKey(), animationModel, currentMarker, from, to)
                            } else {
                                //First Location init
                                Common.driverLocationSubscribe.put(driverGeoModel.getKey(), animationModel)
                            }
                        }
                    }
                }
            })
        }

    }

    private fun moveMarkerAnimation(key: String, animationModel: AnimationModel, currentMarker: Marker?, from: String, to: String) {
        if (!animationModel.isRun){
            //Request API
            compositeDisposable.add(iGoogleAPI.getDirections("driving","less_driving",from, to, getString(R.string.google_api_key))
                )

        }
    }

    override fun onFirebaseLoadFailed(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }
}