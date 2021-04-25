package com.example.ojeksyaririder.model

import com.firebase.geofire.GeoLocation

class DriverGeoModel (

){
    private lateinit var key: String
    private lateinit var geoLocation: GeoLocation
    private var driverInfoModel: DriverInfoModel? = null

    fun DriverGeoModel(){

    }

    constructor(key: String, geoLocation: GeoLocation): this(){
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