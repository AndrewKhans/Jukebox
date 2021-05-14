package com.example.jukebox;

import android.content.Intent;
import android.net.Uri;

public class Messenger {

    public static void sendMessage(String phoneNumber, String message) {
        // Create the intent.
        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        // Set the data for the intent as the phone number.
        smsIntent.setData(Uri.parse(phoneNumber));
        // Add the message (sms) with the key ("sms_body").
        smsIntent.putExtra("sms_body", message);
    }

}
