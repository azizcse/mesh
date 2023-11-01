package com.w3engineers.helper;

public class LocationHelper {

    private static LocationHelper locationHelper = new LocationHelper();
    private double MY_DISTANCE_RATIO = 0.1, USER_DISTANCE_RATIO = 10.0;

    public static LocationHelper getInstance() {
        return locationHelper;
    }

    public double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515; // Value in Mile
        dist = dist * 1.609344; // Value in KM
        return (dist);
    }

    public boolean myDistanceIsInRatio(double lat1, double lon1, double lat2, double lon2) {
        double myDistance = distance(lat1, lon1, lat2, lon2);
        return myDistance >= MY_DISTANCE_RATIO;
    }

    public boolean userDistanceIsInRatio(double lat1, double lon1, double lat2, double lon2, double range) {
        double userDistance = distance(lat1, lon1, lat2, lon2);
        return userDistance <= range;
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}
