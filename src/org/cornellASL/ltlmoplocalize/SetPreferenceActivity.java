package org.cornellASL.ltlmoplocalize;

/*
 * Author: Abhishek Sriraman
 * Last Modified: 12/12/13
 */

import com.ASL.ltlmoplocalize.R;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceFragment;

//Preference Activity
public class SetPreferenceActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment {

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.preferences);
		}

	}

}
