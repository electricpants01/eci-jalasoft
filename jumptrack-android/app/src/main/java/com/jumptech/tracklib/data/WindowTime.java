package com.jumptech.tracklib.data;

/**
 * Stores all Widow Time of a Stop Window
 */
public class WindowTime {

    private Long startSec;
    private Long endSec;

    public Long getStartSec() {
        return startSec;
    }

    public void setStartSec(Long startSec) {
        this.startSec = startSec;
    }

    public Long getEndSec() {
        return endSec;
    }

    public void setEndSec(Long endSec) {
        this.endSec = endSec;
    }
}
