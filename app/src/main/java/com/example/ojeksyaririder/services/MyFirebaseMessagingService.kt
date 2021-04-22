package com.example.ojeksyaririder.services

import com.example.ojeksyaririder.utils.Common
import com.example.ojeksyaririder.utils.UserUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        if (FirebaseAuth.getInstance().currentUser != null)
            UserUtils.updateToken(this,s)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val dataRecv: Map<String, String> = remoteMessage.data
        if (dataRecv != null){
            Common.showNotification(this, Random().nextInt(),
                dataRecv.get(Common.NOTI_TITLE),
                dataRecv.get(Common.NOTI_CONTENT),
                null
            )
        }
    }
}