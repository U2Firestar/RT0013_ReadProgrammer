package com.fhstp.it231503.caen.util;

import java.util.Date;

/**
 * Data structure class for saving a single datapoint for a temperature/humidity.
 * @author Emil Sedlacek / it231503
 * */
public class measurmentPoint {
    /**
     * Humidity in % or temperature in °C
     * */
    private float value;
    /**
     * Timestamp
     * */
    private Date date;

    /**
     * Data structure class for saving a single bin setting.
     * @param date Stores the timestamp of said value.
     * @param value Stores the value (Humidity in % or temperature in °C).
     * */
    public measurmentPoint(Date date, float value) {
        this.date = date;
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
