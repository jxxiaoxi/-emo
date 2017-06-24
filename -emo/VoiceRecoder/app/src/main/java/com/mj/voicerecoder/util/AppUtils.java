package com.mj.voicerecoder.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by liuwei on 6/16/17.
 */

public class AppUtils {
    private static String locationProvider;
    public static final String TAG = " AppUtils";


    /**
     * @return 经纬度
     */
    public static ArrayList<String> getLocation(Context context) {
        //获取地理位置管理器
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        //获取所有可用的位置提供器
        List<String> providers = locationManager.getProviders(true);
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            //如果是GPS
            locationProvider = LocationManager.GPS_PROVIDER;
        } else if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
            //如果是Network
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else {
            Log.e(TAG, "NO location Provider");
        }
        //获取Location
        ArrayList<String> loc = new ArrayList<String>();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(locationProvider);
            if (location != null) {
                //不为空,显示地理位置经纬度
                double lat = location.getLatitude();//获取纬度
                double lng = location.getLongitude();//获取经度

                loc.add(String.valueOf(lat));
                loc.add(String.valueOf(lng));
                Log.e(TAG, "xxxxxx"+lat);

            } else {
                return null;
            }
        }


        return loc;
    }


    /**
     * 获取文件保存路径
     */
    public static String getAppPath(){
        return Environment.getExternalStorageDirectory() + "/voicerecoder/";
    }
}
