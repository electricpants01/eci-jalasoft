package com.jumptech.tracklib.data;

import java.util.List;

public class Stop {
	public long key;
	public long site_key;
	public String name;
	public String address;
	public int delivery_count;
	public Long baseDeliveryKey;
	public Long signature_key;
	private Boolean planned;
	private Integer sort;

	private Integer window_id;

	/**
	 * Stores the Stop Window Display
	 */
	private String windowDisplay;

	/**
	 * Stores the Stop Window Time list
	 */
	private List<WindowTime> windowTimeList;

	public boolean finished() {
		return signature_key != null;
	}

	public boolean isOrdered() {
		return planned;
	}

	public void setPlanned(Boolean planned) {
		this.planned = planned;
	}

	public Integer getSort() {
		return sort;
	}

	public void setSort(Integer sort) {
		this.sort = sort;
	}

	public Integer getWindow_id() {
		return window_id;
	}

	public void setWindow_id(Integer window_id) {
		this.window_id = window_id;
	}

	public String getWindowDisplay() {
		return windowDisplay;
	}

	public void setWindowDisplay(String windowDisplay) {
		this.windowDisplay = windowDisplay;
	}

	public List<WindowTime> getWindowTimeList() {
		return windowTimeList;
	}

	public void setWindowTimeList(List<WindowTime> windowTimeList) {
		this.windowTimeList = windowTimeList;
	}
}
