package org.cornell_ASL.ltlmoplocalize;
/*
 * Author: Abhishek Sriraman
 * Last Modified: 12/12/13
 */

import com.ASL.ltlmoplocalize.R;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

//Initial Activity when application is opened. Contains link to start image processing. 
public class MainActivity extends Activity {
	String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Button sendButton = (Button) findViewById(R.id.localizeStart);
		sendButton.setOnClickListener(new View.OnClickListener(){
			public void onClick(View v){
				Log.d(TAG, "Going to Start Localize!");
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, MainProcess.class);
				startActivityForResult(intent, 0);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetPreferenceActivity.class);
        startActivityForResult(intent, 0); 
		
        return true;
	}

}
