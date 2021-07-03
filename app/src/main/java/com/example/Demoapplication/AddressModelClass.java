package com.example.Demoapplication;

import com.here.sdk.core.GeoCoordinates;

public class AddressModelClass {

    String RestaurantName;
    GeoCoordinates location;

    public String getRestaurantName() {
        return RestaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        RestaurantName = restaurantName;
    }

    public GeoCoordinates getLocation() {
        return location;
    }

    public void setLocation(GeoCoordinates location) {
        this.location = location;
    }
}
