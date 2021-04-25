package com.example.ojeksyaririder.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ojeksyaririder.R
import com.example.ojeksyaririder.model.DriverGeoModel
import com.example.ojeksyaririder.model.RiderModel
import com.google.android.gms.maps.model.Marker

class Common {

    companion object {
        const val TOKEN_REFERENCE = "Token"
        const val RIDER_INFO_REFERENCE = "Riders"
        const val DRIVERS_LOCATION_REFERENCES = "DriversLocation" //Same as driver
        const val DRIVER_INFO_REFERENCE = "DriverInfo"

        var driversFound: HashSet<DriverGeoModel> = HashSet()
        var markerList: HashMap<String, Marker> = HashMap()

        lateinit var currentRider: RiderModel

        const val NOTI_TITLE = "title"
        const val NOTI_CONTENT = "body"

        fun buildName(firstname: String?, lastName: String?): String? {
            return StringBuilder(firstname).append(" ").append(lastName).toString()
        }

        fun buildWelcomeMessage(): String? {
            if (currentRider != null){
                return StringBuilder("Welcome ")
                    .append(currentRider.firstName)
                    .append(" ")
                    .append(currentRider.lastName).toString()
            } else
                return ""
        }

        fun showNotification(mContext: Context, id: Int, title: String?, body: String?, intent: Intent?){
            var pendingIntent: PendingIntent?
            pendingIntent = null
            if (intent != null){
                pendingIntent = PendingIntent.getActivity(mContext, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }
            val NOTIFICATION_CHANNEL_ID = "ojek_syari"
            val notificationManager: NotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                val notificationChannel : NotificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Ojek Syar'i", NotificationManager.IMPORTANCE_HIGH)
                notificationChannel.description = "Ojek Syar'i"
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
                notificationChannel.enableVibration(true)

                notificationManager.createNotificationChannel(notificationChannel)
            }

            val builder = NotificationCompat.Builder(mContext, NOTIFICATION_CHANNEL_ID)
            builder.setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(false)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setSmallIcon(R.drawable.ic_car_black)
                    .setLargeIcon(BitmapFactory.decodeResource(mContext.resources, R.drawable.ic_car_black))
            builder.setContentIntent(pendingIntent)
            val notification = builder.build()
            notificationManager.notify(id, notification)
        }


    }

}