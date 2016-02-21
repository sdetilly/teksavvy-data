package com.example.steven.teksavvydata;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Steven on 2016-01-08.
 */
public class ListenerService extends WearableListenerService {

    /*@Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals("/message_path")) {
            final String message = new String(messageEvent.getData());
            Log.v("ListenerService", "Message path received on watch is: " + messageEvent.getPath());
            Log.v("ListenerService", "Message received on watch is: " + message);

            // Broadcast message to wearable activity for display
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            Log.v("ListenerService", "Intent sent to mainAct");
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }*/

}
