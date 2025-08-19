package com.jumptech.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Window;
import android.widget.TextView;

import com.jumptech.jumppod.R;

public class NewCustomerActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.newcustomer_activity);
		Linkify.addLinks((TextView)findViewById(R.id.email), Linkify.EMAIL_ADDRESSES);
		Linkify.addLinks((TextView)findViewById(R.id.phoneNumber), Linkify.PHONE_NUMBERS);
	}
}
