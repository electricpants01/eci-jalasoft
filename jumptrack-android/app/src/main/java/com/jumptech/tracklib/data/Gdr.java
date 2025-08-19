package com.jumptech.tracklib.data;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.jumptech.jumppod.R;

public class Gdr {
	public enum Level {
		ROOT(R.string.regionList),
		REGION(R.string.regionList),
		DISTRIBUTION(R.string.distributionList),
		ROUTE(R.string.routeList) {{ _hasDelivery = true; }},
		;
		
		public boolean _hasDelivery = false;
		public int _title;
		
		Level(int title) {
			_title = title;
		}
	};
	
	public enum Path { 
		PARENT, SIBLING, CHILD, ;
	};

	public static final int COLOR_LENGTH = 7;

	@SerializedName("level")
	protected Level _level;
	@SerializedName("key")
	protected Long _key;
	@SerializedName("name")
	protected String _name;

	@SerializedName("color-hex")
	private String colorHex;
	private Boolean assigned;
	@SerializedName("departure-date")
	private String departureDate;
	@SerializedName("owner-label")
	private String ownerLabel;

	public Gdr() {
		_level = Level.ROOT;
	}

	public Gdr(Level level, Long key, String name) {
		_level = level;
		_key = key;
		_name = name;
	}
	
	public Long getKey() {
		return _key;
	}
	
	public String getName() {
		return _name;
	}

	public Level getLevel() {
		return _level;
	}

	public String getColorHex() {
		return colorHex;
	}

	public Boolean getAssigned() {
		return assigned;
	}

	public String getDepartureDate() {
		return departureDate;
	}

	public String getOwnerLabel() {
		return ownerLabel;
	}

	public boolean isColorValid() {
		if (!TextUtils.isEmpty(colorHex) && colorHex.length() == COLOR_LENGTH) {
			return true;
		}
		return false;
	}
}
