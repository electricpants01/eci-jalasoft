package com.jumptech.jumppod;

public enum Broadcast {
	ROUTE_SYNC,
	;
	
	public String getAction() {
		return getClass().getName() + "." + ROUTE_SYNC.name();
	}

	public static Broadcast fromAction(String action) {
		return Broadcast.valueOf(action.replaceFirst("^.*\\.", ""));
	}
}
