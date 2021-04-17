package com.example.ojeksyaririder.utils

import com.example.ojeksyaririder.model.RiderModel

class Common {

    companion object {
        fun buildWelcomeMessage(): String? {
            if (currentRider != null){
                return StringBuilder("Welcome ")
                    .append(currentRider.firstName)
                    .append(" ")
                    .append(currentRider.lastName).toString()
            } else
                return ""
        }

        const val RIDER_INFO_REFERENCE = "Riders"
        lateinit var currentRider: RiderModel
    }

}