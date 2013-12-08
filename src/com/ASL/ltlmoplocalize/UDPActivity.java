package com.ASL.ltlmoplocalize;

import com.ASL.ltlmoplocalize.R;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class UDPActivity extends Activity {
	private WifiManager wifiManager; 
	String sendToIp;
	int sendToPort;
	
	TextView ip_display;
	TextView port_display;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.udp_main);
		ip_display = (TextView)findViewById(R.id.ip_display);
		port_display = (TextView)findViewById(R.id.port_display);
	
		Button sendButton = (Button) findViewById(R.id.send);
		sendButton.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				boolean wifiEnabled = wifiManager.isWifiEnabled();
				if (wifiEnabled){
					EditText inputMessageTxt = (EditText) findViewById(R.id.inputMessage);
					String inMsg = inputMessageTxt.getText().toString();
					Thread t1 = new Thread(new Client(inMsg, sendToIp, sendToPort));
					t1.start();
					inputMessageTxt.setText("");
				}
				else{
					Log.d("my", "Wifi is disabled....");
					Toast.makeText(getApplicationContext(), "Wifi is Disabled", Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		Log.d("my","Created new activity");
		wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		
		loadPref();
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		/*
		 * Because it's onlt ONE option in the menu.
		 * In order to make it simple, We always start SetPreferenceActivity
		 * without checking.
		 */
		
		Intent intent = new Intent();
        intent.setClass(UDPActivity.this, SetPreferenceActivity.class);
        startActivityForResult(intent, 0); 
		
        return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		loadPref();
	}
    
	
	private void loadPref(){
		SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		sendToIp = mySharedPreferences.getString("UDP_ip_add", "192.169.10.1");
		ip_display.setText("IP Address: " + sendToIp);
		Log.d("UDP", "Loaded IP Address:" + sendToIp);
		
		String isendToPort = mySharedPreferences.getString("UDP_port_num", "5005");
		port_display.setText("Port Number: "+isendToPort);
		sendToPort = Integer.parseInt(isendToPort);
		Log.d("UDP", "Loaded Port:" + sendToPort);
		

	}
	

}


