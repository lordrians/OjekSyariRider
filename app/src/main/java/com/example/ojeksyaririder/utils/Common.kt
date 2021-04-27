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
import com.example.ojeksyaririder.model.AnimationModel
import com.example.ojeksyaririder.model.DriverGeoModel
import com.example.ojeksyaririder.model.RiderModel
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

class Common {

    companion object {
        const val TOKEN_REFERENCE = "Token"
        const val RIDER_INFO_REFERENCE = "Riders"
        const val DRIVERS_LOCATION_REFERENCES = "DriversLocation" //Same as driver
        const val DRIVER_INFO_REFERENCE = "DriverInfo"

        var driversFound: HashSet<DriverGeoModel> = HashSet()
        var markerList: HashMap<String, Marker> = HashMap()
        var driverLocationSubscribe: HashMap<String, AnimationModel> = HashMap()

        lateinit var currentRider: RiderModel

        const val NOTI_TITLE = "title"
        const val NOTI_CONTENT = "body"

        fun getBearing(begin: LatLng, end: LatLng): Float{
            val lat = Math.abs(begin.latitude - end.latitude)
            val lng = Math.abs(begin.longitude - end.longitude)
            if (begin.latitude < end.latitude && begin.longitude < end.longitude)
                return (Math.toDegrees(Math.atan(lng / lat))).toFloat()
            else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
                return ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90).toFloat()
            else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
                return (Math.toDegrees(Math.atan(lng / lat)) + 180).toFloat()
            else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
                return ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270).toFloat()
            return (-1).toFloat()
        }

        fun decodePoly(encoded: String): List<LatLng>{
            var poly: ArrayList<LatLng> = ArrayList()
            var index = 0
            val len = encoded.length
            var lat = 0
            var lng = 0
            while (index < len)
            {
                var b:Int
                var shift = 0
                var result = 0
                do
                {
                    b = (encoded.get(index.inc()) - 63).toInt()
                    result = result or ((b and 0x1f) shl shift)
                    shift += 5
                }
                while (b >= 0x20)
                val dlat = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
                lat += dlat
                shift = 0
                result = 0
                do
                {
                    b = (encoded.get(index++) - 63).toInt()
                    result = result or ((b and 0x1f) shl shift)
                    shift += 5
                }
                while (b >= 0x20)
                val dlng = (if ((result and 1) != 0) (result shr 1).inv() else (result shr 1))
                lng += dlng
                val p = LatLng(((lat.toDouble() / 1E5)),
                    ((lng.toDouble() / 1E5)))
                poly.add(p)
            }
            return poly
        }

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