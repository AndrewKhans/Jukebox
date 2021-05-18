package com.example.jukebox;

import android.app.PendingIntent;
import android.telephony.SmsManager;
import android.util.Log;

public class Messenger {

    public static void sendMessage(String phoneNumber, String message) {
        // Set the service center address if needed, otherwise null.
        String scAddress = null;
        // Set pending intents to broadcast
        // when message sent and when delivered, or set to null.
        PendingIntent sentIntent = null, deliveryIntent = null;
        // Use SmsManager.
        SmsManager smsManager = SmsManager.getDefault();
        String destination = "1" + phoneNumber;
        Log.d("Pepis", "Sending '" + message + "' to '" + destination + "'");

        smsManager.sendTextMessage(destination, null, message, null, null);
    }

}
