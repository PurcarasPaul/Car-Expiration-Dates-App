package com.example.carexpirationdates;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Receiver extends Service
{
    Timer timer;
    TimerTask timerTask;
    private Intent ActualIntent;

    @Nullable
    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        ActualIntent = intent;

        startTimer();

        return START_STICKY;
    }

    public void startTimer()
    {
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                try {
                    CheckIntents(ActualIntent);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        };

        //schedule the timer, after the first 3000ms the TimerTask will run every day 86400000
        timer.scheduleAtFixedRate(timerTask, 3000, 86400000);
    }

    public void stoptimertask()
    {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void AddNotifications(Intent intent,String Title,String Description)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "ExpirationDateNotification")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(Title)
                .setContentText(Description)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(Description));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify((int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE), builder.build());
        SystemClock.sleep(1000);
    }

    //Checks if a car's expiration date is about to expire soon(in the next 3 days), if yes it will notify the user
    void CheckIntents(Intent intent) throws ParseException
    {
        String title = null;
        String description = null;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        Date tempDate = sdf.parse(currentDate);

        tempDate.setDate(tempDate.getDate() + 1);
        String currentDate1 = new SimpleDateFormat("dd/MM/yyyy").format(tempDate);
        tempDate.setDate(tempDate.getDate() + 1);
        String currentDate2 = new SimpleDateFormat("dd/MM/yyyy").format(tempDate);
        tempDate.setDate(tempDate.getDate() + 1);
        String currentDate3 = new SimpleDateFormat("dd/MM/yyyy").format(tempDate);

        for (car_data CarData : MainActivity.StaticMainActivity.CarsData)
        {
            String CarExpDate = CarData.getFinish();
            title = getString(R.string.Title) + " " + CarData.getRegistration_Type();
            description = null;

            if (CarExpDate == null)
            {
                continue;
            }

            if (CarExpDate.equals(currentDate))
            {
                description = getString(R.string.Description) + " " + CarData.getVehicle_Plate() + " " + getString(R.string.Has) + " " + getString(R.string.Expired) + ".";
            }
            else if (CarExpDate.equals(currentDate1))
            {
                description = getString(R.string.Description) + " " + CarData.getVehicle_Plate() + " " + getString(R.string.Has) + 1 + " " + getString(R.string.Day_Left) + ".";
            }
            else if (CarExpDate.equals(currentDate2))
            {
                description = getString(R.string.Description) + " " + CarData.getVehicle_Plate() + " " + getString(R.string.Has) + 2 + " " + getString(R.string.Days_Left) + ".";
            }
            else if (CarExpDate.equals(currentDate3))
            {
                description = getString(R.string.Description) + " " + CarData.getVehicle_Plate() + " " + getString(R.string.Has) + 3 + " " + getString(R.string.Days_Left) + ".";
            }

            if(description != null)
            {
                AddNotifications(intent,title,description);
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "example.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stoptimertask();

        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }
}