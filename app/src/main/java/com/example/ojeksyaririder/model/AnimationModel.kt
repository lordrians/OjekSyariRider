package com.example.ojeksyaririder.model

import com.firebase.geofire.GeoLocation

class AnimationModel (){

    var isRun: Boolean = false
    var geoQueryModel: GeoQueryModel? = null


    constructor(isRun: Boolean, geoQueryModel: GeoQueryModel?): this(){
        this.isRun = isRun
        this.geoQueryModel = geoQueryModel
    }





}