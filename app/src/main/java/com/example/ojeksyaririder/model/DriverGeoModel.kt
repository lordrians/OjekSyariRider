package com.example.ojeksyaririder.model

import com.firebase.geofire.GeoLocation

class DriverGeoModel (
        private var key: String,
        private var geoLocation: GeoLocation,
        private var driverInfoModel: DriverInfoModel
){
    fun DriverGeoModel(){

    }

    fun DriverGeoModel(key: String, geoLocation: GeoLocation){
        this.key = key
        this.geoLocation = geoLocation
    }
}