package com.hackerkernel.backgroundlocationjobservice.Worker;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.hackerkernel.backgroundlocationjobservice.Infrastructure.MyApplication;
import com.hackerkernel.backgroundlocationjobservice.Location.UpdatesLocation;
import com.hackerkernel.backgroundlocationjobservice.MainActivity;
import com.hackerkernel.backgroundlocationjobservice.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class MyWorker extends Worker implements UpdatesLocation.ILocationProvider {
    private static final String TAG = "MyWorker";
    private Context context;
    public static UpdatesLocation updatesLocation;

    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        initJobService();
        return Result.success();
    }

    public void initJobService() {
        sendNotificationAlert(getApplicationContext(), "Location Start", "Please wait, Fetching your current location");
        updatesLocation = new UpdatesLocation(this);
        updatesLocation.onCreate(getApplicationContext());
        updatesLocation.onStart();
    }

    @Override
    public void onLocationUpdate(Location location) {
        Log.d(TAG, "onLocationUpdate: " + location.getLatitude() + " == " + location.getLongitude());
        getAddress(location.getLatitude() + "", location.getLongitude() + "");
    }

    //send push notification in app with location and address
    private void sendNotificationAlert(Context context, String title, String details) {
        Intent intent;
        intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = getID() + "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification", NotificationManager.IMPORTANCE_MAX);
            notificationChannel.setDescription("location");
            notificationChannel.enableLights(true);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(title)
                .setContentText(details)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(details))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.mipmap.ic_launcher))
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_MAX);
        notificationManager.notify(getID(), notificationBuilder.build());
    }

    //convert real date and time
    public static String convertDateTime(String timestamp) {
        long unixSeconds = Long.parseLong(timestamp);
        Date date = new Date(unixSeconds * 1000L);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM, dd yyyy hh:mm a");
        return sdf.format(date);
    }

    private final static AtomicInteger c = new AtomicInteger(0);

    private static int getID() {
        return c.incrementAndGet();
    }

    //get address by latitude and longitude from google api
    public void getAddress(final String lat,
                           final String lng) {
        StringRequest request = new StringRequest(Request.Method.GET, "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + ","
                + lng + "&sensor=true" + "&key=" + "AIzaSyCw6x9hRsWmbGewmaER0R6OPne8zWQtzWk", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "onResponse: " + response);
                try {
                    JSONObject jsonObj = new JSONObject(response);
                    String Status = jsonObj.getString("status");
                    if (Status.equalsIgnoreCase("OK")) {
                        JSONArray Results = jsonObj.getJSONArray("results");
                        JSONObject zero = Results.getJSONObject(0);
                        String formatted_address = zero.getString("formatted_address");
                        sendNotificationAlert(getApplicationContext(), "Current Location", "Your latest location is\nLatitude : " + lat + "\nLongitude : " + lng + "\nAddress is : " + formatted_address + " at : " + convertDateTime(System.currentTimeMillis() / 1000 + ""));
                        updatesLocation.onStop();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MyApplication.mRequestQue.add(request);
    }
}