package com.ASL.ltlmoplocalize;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

public class SetPreferenceActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		getFragmentManager().beginTransaction().replace(android.R.id.content,
                new PrefsFragment()).commit();
	}
	
	public static class PrefsFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onCreate(savedInstanceState);
			
			// Load the preferences from an XML resource
	        addPreferencesFromResource(R.xml.preferences);
		}

	}
	
	

}


