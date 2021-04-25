package com.example.ojeksyaririder.callback

import com.example.ojeksyaririder.model.DriverGeoModel

interface IFirebaseDriverInfoListener {

    fun onDriverInfoLoadSuccess(driverGeoModel: DriverGeoModel)

}