package com.example.airdataapp.Model;

import java.sql.Timestamp;
import java.util.ArrayList;

public class Report {

    private int phoneInfo;
    private Measure emMeasure;
    private boolean isWiFiEnabled;
    private GPSMeasure gpsMeasure;
    private String userID;
    private String timestamp;


    public Measure getEmMeasure() {
        return emMeasure;
    }

    public void setEmMeasure(Measure emMeasure) {
        this.emMeasure = emMeasure;
    }

    public boolean isWiFiEnabled() {
        return isWiFiEnabled;
    }

    public void setWiFiEnabled(boolean wiFiEnabled) {
        isWiFiEnabled = wiFiEnabled;
    }

    public GPSMeasure getGpsMeasure() {
        return gpsMeasure;
    }

    public void setGpsMeasure(GPSMeasure gpsMeasure) {
        this.gpsMeasure = gpsMeasure;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}