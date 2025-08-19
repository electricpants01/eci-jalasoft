package com.jumptech.tracklib.data;

import android.graphics.Color;

import com.jumptech.jumppod.R;

import org.apache.commons.lang.RandomStringUtils;

public enum DeliveryType {
	DROPOFF(R.drawable.delivery_icon, R.color.login_control),
	PICKUP(R.drawable.returns_icon, R.color.red),
	;
	
	int _icon;
	int _color;
	
	private DeliveryType(int icon, int color) {
		_icon = icon;
		_color = color;
	}

	public int getColor() {
		return _color;
	}

	public int getIcon() {
		return _icon;
	}
	public String generateId() {
		return "U-" + RandomStringUtils.randomNumeric(10);
	}

}
