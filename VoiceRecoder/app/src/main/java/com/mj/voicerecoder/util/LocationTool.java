package com.mj.voicerecoder.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuwei on 6/27/17.
 */

public class LocationTool {
    private LocationManager locationManager;
    private String provider;
    public static final LocationTool mLocationTool = new LocationTool();

    public static LocationTool getInstance() {
        return mLocationTool;
    }

    public ArrayList<String> getLocationData(Context context) {
        Location location = null;
        ArrayList<String> locations = new ArrayList<>();
        //initLocation(context);
        location = initLocation(context);
        if (location != null) {
            locations.add(location.getLongitude() + "");//经度
            locations.add(location.getLatitude() + "");//纬度
        }

        return locations;
    }


    public Location initLocation(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        List<String> providerList = locationManager.getProviders(true);
        if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else {
            Toast.makeText(context, "No location provider to use",
                    Toast.LENGTH_SHORT).show();
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("mijie", "no Location Permission");
        }
//        locationManager.requestLocationUpdates(provider, 5000, 5,
//                locationListener);
        return locationManager.getLastKnownLocation(provider);
    }

    LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            // 更新当前设备的位置信息 更新
        }
    };
}
