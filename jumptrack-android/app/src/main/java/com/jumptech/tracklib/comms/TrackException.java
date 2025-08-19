package com.jumptech.tracklib.comms;

public class TrackException extends Exception {

    /**
     * Stores the message for Log.e()
     */
    public static final String MSG_EXCEPTION = "Exception: ";

	private static final long serialVersionUID = 1L;

	public TrackException(String messge) {
		super(messge);
	}

	public TrackException() {
		super();
	}
	
	public TrackException(Throwable throwable) {
		super(throwable);
	}
}
