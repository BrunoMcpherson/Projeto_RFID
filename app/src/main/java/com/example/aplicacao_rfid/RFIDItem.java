package com.example.aplicacao_rfid;

import java.io.Serializable;

public class RFIDItem extends Localizacao {
    private final String tagId;
    private final String itemName;
    private final String locationName;
    private final double latitude;
    private final double longitude;
    private final String timestamp;

    public RFIDItem(String tagId, String itemName, String locationName,
                    double latitude, double longitude, String timestamp) {
        this.tagId = tagId;
        this.itemName = itemName;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    // Getters
    public String getTagId() { return tagId; }
    public String getItemName() { return itemName; }
    public String getLocationName() { return locationName; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getTimestamp() { return timestamp; }
}
