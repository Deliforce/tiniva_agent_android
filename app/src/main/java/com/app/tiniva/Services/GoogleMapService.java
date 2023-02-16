/*
package com.app.tiniva.Services;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.app.tiniva.Cognito.AppHelper;
import com.app.tiniva.ModelClass.DriverStatusApi.DriverStatusUpdate;
import com.app.tiniva.RawHeaders.DriverStatusInfo.DriverStatus;
import com.app.tiniva.ServiceApi.APIServiceFactory;
import com.app.tiniva.ServiceApi.ApiService;
import com.app.tiniva.Utils.DeliforceConstants;
import com.app.tiniva.Utils.LoginPrefManager;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GoogleMapService extends Service implements LocationListener {

    private static final String TAG = "GoogleMapService";

    private AWSIotMqttManager mqttManager;
    private LocationManager mLocationManager = null;
    double latitude, longitude;
    Handler handler, eta_handler;
    Runnable runnable, eta_runnable;
    LocationManager locationManager;
    Location location, eta_location;
    boolean isGPSEnable = false;
    boolean isNetworkEnable = false;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 0f;
    ApiService apiService;
    DriverStatus driverStatus;
    LoginPrefManager loginPrefManager;
    LatLng drop_latlng;
    CountDownTimer cdt = null;
    private static final String API_KEY = "AIzaSyBIkhfCZLLeXaBwLzAGGfrBf_Kt7Ymi1L0";

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        initMQTT();

        apiService = APIServiceFactory.getRetrofit().create(ApiService.class);

        loginPrefManager = new LoginPrefManager(getBaseContext());


        drop_latlng = new LatLng(Double.parseDouble(loginPrefManager.getDropLatitude()), Double.parseDouble(loginPrefManager.getDropLongitude()));


        cdt = new CountDownTimer(DeliforceConstants.LOCATION_INTERVAL_FOR_API_TIME, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG, "Countdown seconds remaining: " + millisUntilFinished / 1000);
            }

            //make MQTT call after every 10 seconds

            @Override
            public void onFinish() {
                fn_getlocation();

                updateDriverStatus();
                Log.i(TAG, "Timer finished");
            }
        };
        cdt.start();

        fn_getlocation();

        eta_runnable = () -> {

            if (loginPrefManager.getStringValue("driver_status").equals("2")) {

                if (eta_location != null) {
                    draw_multi_route(new LatLng(eta_location.getLatitude(), eta_location.getLongitude()), drop_latlng);
                }
            } else {
                eta_handler.postAtTime(eta_runnable, DeliforceConstants.LOCATION_INTERVAL_FOR_ETA_TIME);
            }
        };

        eta_handler.postAtTime(eta_runnable, DeliforceConstants.LOCATION_INTERVAL_FOR_ETA_TIME);

    }

    private void init() {
        handler = new Handler();
        initializeLocationManager();

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("GPS tracker Service", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            }

        }
    }

    @Override
    public void onLocationChanged(Location location) {
//        fn_update(location);
        Log.e(TAG, "onLocation Changed--> " + location.getLatitude() + " " + location.getLongitude());
        eta_location = location;
//        loginPrefManager.getStringValue("driver_status").equalsIgnoreCase("1");
        if (loginPrefManager.getStringValue("driver_status").equalsIgnoreCase("2")) {
            publishData(location.getLatitude(), location.getLongitude());


        }

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void initMQTT() {
        mqttManager = new AWSIotMqttManager(AppHelper.APP_CLIENT_ID, AppHelper.CUSTOMER_SPECIFIC_ENDPOINT);
        AWSIoTConfig.getInstance().initContext(getBaseContext());
        AWSIoTConfig.getInstance().initAWSIot();
        mqttManager = AWSIoTConfig.getInstance().connectWithMQTT();
    }

    //publish to through MQTT
    private void publishData(double currentLat, double currentLon) {
        //read values from preferences
        String driverId = loginPrefManager.getDriverID();
        Set<String> adminIdSet = loginPrefManager.getAdminList();
        Log.e(TAG, "Driver Id --> " + driverId);
        String lattitude = String.valueOf(currentLat);
        String longitude = String.valueOf(currentLon);

        String driverOnlineOfflineStatus = loginPrefManager.getStringValue("driver_status");
        try {
            //form a string like JSON object to publish it
            JSONObject mqttObj;
            Map<String, String> publishData = new HashMap<>();
            publishData.put("driverId", driverId.trim());
            publishData.put("driverStatus", driverOnlineOfflineStatus.trim());
            publishData.put("location_lat", lattitude.trim());
            publishData.put("location_lan", longitude.trim());
            mqttObj = new JSONObject(publishData);
            String jsonToString = mqttObj.toString();
            byte[] pubMsg = jsonToString.getBytes();

            //publish message
            for (String mgrId : adminIdSet) {
                String topic = "/livetracking/" + mgrId;
                Log.e(TAG, "Topic--> " + topic);
                mqttManager.publishData(pubMsg, topic, AWSIotMqttQos.QOS0);
            }
        } catch (Exception e) {
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();


        if (loginPrefManager.getCogintoToken() != null) {
            sendBroadcast(new Intent("YouWillNeverKillMe"));
        } else {
            Log.e("No user", "found");
        }

    }

    private void fn_getlocation() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnable = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnable) {

        } else {


            if (isGPSEnable) {
                location = null;
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (location != null) {
                        Log.e("latitude", location.getLatitude() + "");
                        Log.e("longitude", location.getLongitude() + "");
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                }
            }


        }

    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e("onChange", "onLocationChanged: " + location);
            mLastLocation.set(location);
            if (loginPrefManager.getStringValue("driver_status").equalsIgnoreCase("2")) {
                publishData(location.getLatitude(), location.getLongitude());
            }

        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };


    private void updateDriverStatus() {

        try {

            driverStatus = new DriverStatus();

            driverStatus.setDriverStatus(Integer.parseInt(loginPrefManager.getStringValue("driver_status")));

            driverStatus.setCurrentLat(latitude);
            driverStatus.setCurrentLan(longitude);

            apiService.updateDriverStatus(loginPrefManager.getCogintoToken(),loginPrefManager.getDeviceToken(), driverStatus).enqueue(new Callback<DriverStatusUpdate>() {
                @Override
                public void onResponse(@NonNull Call<DriverStatusUpdate> call, @NotNull Response<DriverStatusUpdate> response) {

//                    Log.e("o/p", new GsonBuilder().setPrettyPrinting().create().toJson(response));


                    cdt.start();
                }

                @Override
                public void onFailure(@NonNull Call<DriverStatusUpdate> call, Throwable t) {

                    cdt.start();

                }
            });

        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    private void draw_multi_route(LatLng start, LatLng end) {

        Log.e("current", String.valueOf(start));
        Log.e("drop", String.valueOf(end));
        GoogleDirection.withServerKey(API_KEY)
                .from(start)
                .to(end)
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {

                        if (direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            try {
                                Log.e("distance", "" + route.getLegList().get(0).getDistance().getText());
                                Toast.makeText(getApplicationContext(), "Distance:" + "" + route.getLegList().get(0).getDistance().getText() + "//" + "TIME:" + route.getLegList().get(0).getDuration().getText(), Toast.LENGTH_LONG).show();
                                eta_handler.postAtTime(eta_runnable, DeliforceConstants.LOCATION_INTERVAL_FOR_ETA_TIME);

                            } catch (Exception e) {
                                Log.e("Exception", e.getMessage());
                            }
                        } else {
                            eta_handler.postAtTime(eta_runnable, DeliforceConstants.LOCATION_INTERVAL_FOR_ETA_TIME);
                        }


                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        // Do something
                        eta_handler.postAtTime(eta_runnable, DeliforceConstants.LOCATION_INTERVAL_FOR_ETA_TIME);
                    }
                });
    }

}
*/
