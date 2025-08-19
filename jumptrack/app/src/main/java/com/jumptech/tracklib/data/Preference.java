package com.jumptech.tracklib.data;

public class Preference {
	public static class DriverImage {
		public Integer max;
		public Integer shortSidePixel;
	}
	public static class Gps {
		public Boolean enabled;
		public Integer minDuration;
		public Integer minDistance;
	}

	public Boolean unscheduledDropoff = false;
	public DriverImage driverImage;
	public Boolean stopSortEnable;
	public Gps gps;
	public Integer syncRate;
	public Boolean unscheduledPickup;
	public String[] partialReason;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if(driverImage != null) {
			sb.append("\ndriverImage max ").append(driverImage.max);
			sb.append("\ndriverImage shortSidePixel ").append(driverImage.shortSidePixel);
		}
		sb.append("\nstopSortAllow ").append(stopSortEnable);
		if(gps != null) {
			sb.append("\ngps enabled ").append(gps.enabled);
		}
		sb.append("\nsyncRate ").append(syncRate);
		sb.append("\nunscheduledDropoff ").append(unscheduledDropoff);
		sb.append("\nunscheduledPickup ").append(unscheduledPickup);
		sb.append("\npartialReason");
		if(partialReason != null) {
			for(String reason : partialReason) {
				sb.append(";").append(reason);
			}
		}
		
		return sb.toString();
	}
}
