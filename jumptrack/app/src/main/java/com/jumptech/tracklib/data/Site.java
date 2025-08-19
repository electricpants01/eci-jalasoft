package com.jumptech.tracklib.data;

public class Site {
	Integer key;
	String account;
	String name;
	String address1;
	String address2;
	String address3;
	String city;
	String state;
	String zip;
	
	
	public String address() {
		StringBuilder sb = new StringBuilder();
		sb.append(address1);
		if(address2 != null) sb.append("\n").append(address2);
		if(address3 != null) sb.append("\n").append(address3);
		sb.append("\n");
		if(city != null) sb.append(city).append(", ");
		if(state != null) sb.append(state).append(" ");
		if(zip != null) sb.append(zip);
		
		return sb.toString();
	}

}
