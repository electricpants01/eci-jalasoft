package com.jumptech.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

import androidx.core.splashscreen.SplashScreen;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.jumptech.jumppod.R;
import com.jumptech.tracklib.data.Route;
import com.jumptech.tracklib.db.TrackPreferences;
import com.jumptech.tracklib.repository.RouteRepository;
import com.jumptech.tracklib.utils.PermissionUtil;

public class LauncherActivity extends Activity {

	/**
	 * Log tag
 	 */
    private static final String TAG = "LauncherActivity";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		securityProviderUpdaterWhenAvailable();
		SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//state recovery
		TrackPreferences pref = new TrackPreferences(this);
		//if we don't have an auth token
		if(pref.getAuthToken() == null) {
			//if we have a username
			if(pref.getLogin() != null){
				startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
				finish();
				return;
			}
			//else stay here for splash screen
		}
		else if(pref.getAuthToken() != null && pref.getIsEulaPrompt()) {
			Log.i(TAG, "display EulaActivity");
			Intent eulaIntent = new Intent(this, EulaActivity.class);
			eulaIntent.putExtra(EulaActivity.DISPLAY_TERMS_OF_USE_READONLY_KEY, false);
			startActivity(eulaIntent);
			finish();
			return;
		}
		//else if we need truck listing
		else if(!RouteRepository.routeHasStop(this, null)) {
			startActivity(new Intent(this, GdrActivity.class));
			finish();
			return;
		} else if (pref.getSignatureInProgress() != null) {
			startActivity(new Intent(this, SignatureNameActivity.class));
			finish();
			return;
		}
		//else we are in route
		else {
			Route route = RouteRepository.fetchRoute(this);
			if(pref.isRoutesInProgress() && route != null ){
				goToRouteScreen();
			}else if(pref.isDeliveryStopInProgress()){
				long stopKey = pref.getStopKey();
				if(stopKey == -1){
					goToGdrActivity();
				}else {
					startActivity(new Intent(this,StopActivity.class).putExtra(BundleTrack.KEY_STOP, pref.getStopKey()));
					finish();
				}
				return;
			}else if(pref.isDeliveryInProgress()){
				startActivity(new Intent(this, DeliveryActivity.class).putExtra(BundleTrack.KEY_DELIVERY, pref.getDeliveryKey()));
				finish();
				return;
			}else if(pref.isSignatureImageInProgress()){
				startActivity(new Intent(this, SignatureImageActivity.class));
				finish();
				return;
			}else {
				Route auxiliaryRoute = RouteRepository.fetchRoute(this);
				if (auxiliaryRoute == null){
					goToGdrActivity();
				}else {
					goToRouteScreen();
				}
				return;
			}
			return;
		}
		startActivity(new Intent(LauncherActivity.this, LoginActivity.class));
		finish();
	}

	private void securityProviderUpdaterWhenAvailable(){
		if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
			try {
				ProviderInstaller.installIfNeeded(this);
			} catch (Exception e) {
				Log.e(TAG, getString(R.string.error_security_provider_updater), e);
			}
		}
	}

	private void goToRouteScreen() {
		Intent intent = new Intent(this, RouteActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
	}

	private void goToGdrActivity(){
		startActivity(new Intent(this, GdrActivity.class));
		finish();
	}

}
