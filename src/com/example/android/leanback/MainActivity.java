/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.leanback;

import android.app.Activity;
import android.os.Bundle;

/*
 * A wrapper class for main view of the app
 */
public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getSimpleName();

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

//		android.provider.Settings.Global.putString(getContentResolver(),
//				android.provider.Settings.Global.HTTP_PROXY,
//				"54.201.58.152:6634");
//		System.getProperties().setProperty("http.proxyHost", "54.201.58.152");
//		System.getProperties().setProperty("http.proxyPort", "6634");
//		Log.d(TAG, System.getProperty("http.proxyHost") + ":" + System.getProperty("http.proxyPort"));
//		System.getProperties().setProperty("socksProxyHost", "192.168.1.100");
//		System.getProperties().setProperty("socksProxyPort", "5000");
//		Log.d(TAG, System.getProperty("socksProxyHost") + ":" + System.getProperty("socksProxyPort"));
//		System.setProperty("sun.net.spi.nameservice.nameservers", "8.8.8.8");
//		System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");
//		Log.d(TAG, System.getProperty("sun.net.spi.nameservice.nameservers"));
	}
}

