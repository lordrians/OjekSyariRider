package com.example.ojeksyaririder.model

import com.firebase.geofire.GeoLocation

class DriverGeoModel (
        private var key: String,
        private var geoLocation: GeoLocation,
        private var driverInfoModel: DriverInfoModel?
){
    fun DriverGeoModel(){

    }

    fun DriverGeoModels(key: String, geoLocation: GeoLocation){
        this.key = key
        this.geoLocation = geoLocation
    }
    fun getKey(): String{
        return key
    }

    fun getDriverinfoModel(): DriverInfoModel?{
        return driverInfoModel
    }

    fun setDriverInfoModel(driverInfoModel: DriverInfoModel){
        this.driverInfoModel = driverInfoModel
    }

    fun getGeoLocation(): GeoLocation {
        return geoLocation
    }

}