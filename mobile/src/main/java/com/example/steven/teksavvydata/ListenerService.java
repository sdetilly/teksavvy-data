package com.example.steven.teksavvydata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Steven on 2016-01-09.
 */
public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    GoogleApiClient googleClient;
    String uriTek = "https://api.teksavvy.com/web/Usage/UsageSummaryRecords?$filter=IsCurrent%20eq%20true";
    String apiKey = "";
    //String apiKey = "07FF52D3B6A622A7D53123E7A6585DD7";
    SharedPreferences prefs;
    int maxDownload = 150;
    public static String value;
    final int MOBILE = 0;
    final int WEAR = 1;
    final int POPUP = 0;
    final int VALUE = 1;
    final String TAG = "ListenerService";

    @Override
    public void onCreate(){
        super.onCreate();
        googleClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleClient.connect();
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
        prefs = this.getSharedPreferences("apikey", 0);
        apiKey = prefs.getString("apikey", "");
        if(apiKey.equals("")){
            Log.d(TAG, "apikey is empty, sending a popup request to main activity");
            sendToActivity(POPUP);
        }
    }
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/wear_req")) {
            fetchData(WEAR);

        }else {
            super.onMessageReceived(messageEvent);
        }
    }


    @Override
    public void onConnected(Bundle connectionHint) {}

    @Override
    public void onConnectionSuspended(int cause) { }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) { }

    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String inputkey = intent.getStringExtra("inputkey");
            if(inputkey != null && !inputkey.equals("")){
                Log.d(TAG, "received: " + inputkey);
                prefs.edit().remove("apikey").apply();
                prefs.edit().putString("apikey", inputkey).apply();
                apiKey = prefs.getString("apikey", "");
                fetchData(MOBILE);
            }
            String request = intent.getStringExtra("request");
            if(request != null && request.equals("please")){
                Log.d(TAG, "received a request for data from Activity");
                fetchData(MOBILE);
            }
        }
    }

    public void fetchData(int deviceId){
        final int device = deviceId;
        final OkHttpClient client = new OkHttpClient();
        Log.d(TAG, "fetching data");
        Request requestTek = new Request.Builder()
                .url(uriTek)
                .get()
                .addHeader("teksavvy-apikey", apiKey)
                .build();

        client.newCall(requestTek).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                try {
                    Log.d(TAG, "fetchData is successful");
                    JSONObject jsonresponse = new JSONObject(response.body().string());
                    JSONArray jsonValue = jsonresponse.getJSONArray("value");
                    JSONObject objValue = jsonValue.getJSONObject(0);
                    String download = objValue.getString("OnPeakDownload");
                    Log.d("net_download", download);
                    double currentDownload = Double.valueOf(download);
                    double bdLeft = maxDownload - currentDownload;
                    value = String.valueOf(bdLeft);
                    if (device == WEAR) {
                        Log.d(TAG, "sending value to Wear");
                        new SendToDataLayerThread("/value_send", value).start();
                    }else{
                        Log.d(TAG, "sending value to main activity");
                        sendToActivity(VALUE);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(googleClient, node.getId(), path,
                        message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("mobSendData", "Message: {" + message + "} sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("mobSendData", "ERROR: failed to send Message");
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.onDestroy();
    }

    public void sendToActivity(int reason){
        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);
        switch(reason){
            case POPUP:
                Log.d(TAG, "sending popup request to main activity");
                messageIntent.putExtra("popup", "please");
                break;
            case VALUE:
                Log.d(TAG, "sending value to main activity");
                messageIntent.putExtra("value", value);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    }
}
