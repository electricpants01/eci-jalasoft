package com.jumptech.tracklib.utils.scan.integrator;

import java.util.Observable;

/**
 * Encapsulates the observes for input/output.
 */
public class ScannerObserver {

    /**
     * Observer input
     */
    private ScannerObservable input;

    /**
     * Observer output
     */
    private ScannerObservable output;

    /**
     * Default constructor
     */
    public ScannerObserver() {
        input = new ScannerObservable();
        output = new ScannerObservable();
    }

    public ScannerObservable getInput() {
        return input;
    }

    public ScannerObservable getOutput() {
        return output;
    }

    /**
     * Encapsulates the observables for scanner
     */
    public class ScannerObservable extends Observable {
        public void emit(Object object) {
            setChanged();
            notifyObservers(object);
        }
    }
}
