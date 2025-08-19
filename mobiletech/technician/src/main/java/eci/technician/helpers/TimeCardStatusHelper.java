package eci.technician.helpers;

import eci.technician.tools.Constants;

import java.util.HashMap;
import java.util.Map;

public class TimeCardStatusHelper {
    private static final Object lock = new Object();
    private static TimeCardStatusHelper instance;
    private Map<String, String> statuses;
    private Map<String, Integer> states;

    private TimeCardStatusHelper() {
        statuses = new HashMap<>();
        states = new HashMap<>();

        statuses.put("BreakIn", Constants.STATUS_BRAKE_IN);
        states.put("BreakIn", 5);
        statuses.put("BreakOut", Constants.STATUS_BRAKE_OUT);
        states.put("BreakOut", 6);
        statuses.put("BrakeIn", Constants.STATUS_BRAKE_IN);
        states.put("BrakeIn", 5);
        statuses.put("BrakeOut", Constants.STATUS_BRAKE_OUT);
        states.put("BrakeOut", 6);

        statuses.put("SignIn", Constants.STATUS_SIGNED_IN);
        states.put("SignIn", 1);
        statuses.put("SignOut", Constants.STATUS_SIGNED_OUT);
        states.put("SignOut", 2);

        statuses.put("LunchIn", Constants.STATUS_LUNCH_IN);
        states.put("LunchIn", 3);
        statuses.put("LunchOut", Constants.STATUS_LUNCH_OUT);
        states.put("LunchOut", 4);
    }

    public static TimeCardStatusHelper getInstance() {
        synchronized (lock) {
            if (instance == null) {
                instance = new TimeCardStatusHelper();
            }
            return instance;
        }
    }

    public String getStatus(String status) {
        if (statuses.containsKey(status)) {
            return statuses.get(status);
        } else {
            return "";
        }
    }

    public int getState(String status) {
        if (statuses.containsKey(status)) {
            return states.get(status);
        } else {
            return 0;
        }
    }
}