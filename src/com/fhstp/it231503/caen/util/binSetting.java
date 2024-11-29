package com.fhstp.it231503.caen.util;

/**
 * Data structure class for saving a single bin setting for a temperature/humidity bin.
 * @author Emil Sedlacek / it231503
 * */
public class binSetting {
    /**
     * Stores the higher limit (Humidity in % or temperature in °C) and indirectly lower limit of following bin.
     * */
    private float highLimit;
    /**
     * Stores the count until an alarm is triggered for bin.
     * */
    private short threshold;
    /**
     * Stores whether Samples shall be logged for bin.
     * */
    private boolean storeSamples;
    /**
     * Stores whether timestamp of samples shall be logged for bin.
     * */
    private boolean storeTimes;
    /**
     * Stores the sampling interval in seconds for bin.
     * */
    private short samplingInterval;

    /**
     * Data structure class for saving a single bin setting.
     * @param highLimitInput Stores the higher limit (Humidity in % or temperature in °C) and indirectly lower limit of following bin.
     * @param samplingIntervalInput Stores the sampling interval in seconds for bin.
     * @param storeSamplesInput Stores whether Samples shall be logged for bin.
     * @param storeTimesInput Stores whether timestamp of samples shall be logged for bin.
     * @param thresholdInput Stores the count until an alarm is triggered for bin.
     * */
    public binSetting(float highLimitInput,
                      short thresholdInput,
                      boolean storeSamplesInput,
                      boolean storeTimesInput,
                      short samplingIntervalInput) {
        this.highLimit = highLimitInput;
        this.threshold = thresholdInput;
        this.storeSamples = storeSamplesInput;
        this.storeTimes = storeTimesInput;
        this.samplingInterval = samplingIntervalInput;
    }

    public float getHighLimit() {
        return highLimit;
    }

    public void setHighLimit(float highLimit) {
        this.highLimit = highLimit;
    }

    public short getThreshold() {
        return threshold;
    }

    public void setThreshold(short threshold) {
        this.threshold = threshold;
    }

    public boolean isStoreSamples() {
        return storeSamples;
    }

    public void setStoreSamples(boolean storeSamples) {
        this.storeSamples = storeSamples;
    }

    public boolean isStoreTimes() {
        return storeTimes;
    }

    public void setStoreTimes(boolean storeTimes) {
        this.storeTimes = storeTimes;
    }

    public short getSamplingInterval() {
        return samplingInterval;
    }

    public void setSamplingInterval(short samplingInterval) {
        this.samplingInterval = samplingInterval;
    }
}
