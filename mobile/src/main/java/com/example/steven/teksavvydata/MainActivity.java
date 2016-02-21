package com.example.steven.teksavvydata;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity{

        TextView tv_value;
        MessageReceiver messageReceiver = new MessageReceiver();
        final int PREFS = 0;
        final int REQUEST = 1;
        String inputKey;
        final String TAG = "MainActivity";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            tv_value = (TextView)findViewById(R.id.tv_data_left);
            IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
            LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
                    startService(new Intent(this, ListenerService.class));
        }

        public class MessageReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                String popup = intent.getStringExtra("popup");
                if(popup != null && popup.equals("please")){
                    Log.d(TAG, "received a popup request");
                    popUp();
                }
                String value = intent.getStringExtra("value");
                if(value != null && !value.equals("")){
                    Log.d(TAG, "received a value");
                    tv_value.setText(value + " GB");
                }
            }
        }

        @Override
        public void onResume(){
            super.onResume();
            sendToService(REQUEST);
            Log.d(TAG, "request sent to service");
        }

        @Override
        public void onDestroy(){
            super.onDestroy();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_apikey) {
                popUp();
            }

            return super.onOptionsItemSelected(item);
        }


        public void popUp(){
            final EditText input = new EditText(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getResources().getString(R.string.pop_title));
            builder.setMessage(getResources().getString(R.string.pop_description));
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            inputKey = input.getText().toString();
                            Log.d(TAG, "sending apikey to service: " + inputKey);
                            sendToService(PREFS);
                        }
                    });
            builder.setView(input);

            builder.show();
        }

        public void sendToService(int reason){
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            switch(reason){
                case PREFS:
                    Log.d(TAG, "requesting prefs change to service");
                    messageIntent.putExtra("inputkey", inputKey);
                    break;
                case REQUEST:
                    Log.d(TAG, "requesting a value from service");
                    messageIntent.putExtra("request", "please");
            }

            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
}