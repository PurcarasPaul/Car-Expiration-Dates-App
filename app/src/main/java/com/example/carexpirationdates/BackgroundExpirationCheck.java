package com.example.carexpirationdates;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackgroundExpirationCheck extends Worker {
    private final Context context;

    public BackgroundExpirationCheck(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork()
    {
        Intent intent = new Intent(this.context, Receiver.class);
        if (!Receiver.isServiceRunning)
        {
            ContextCompat.startForegroundService(context, intent);
        }

        try
        {
            CheckIntents();
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return Result.success();
    }

    public void AddNotifications(String Title,String Description)
    {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.StaticMainActivity, "ExpirationDateNotification")
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(Title)
                .setContentText(Description)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(Description));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.StaticMainActivity);
        notificationManager.notify((int) ((new Date().getTime() / 1000L) % Integer.MAX_VALUE), builder.build());
        SystemClock.sleep(1000);
    }

    //Checks if a car's expiration date is about to expire soon(in the next 3 days), if yes it will notify the user
    void CheckIntents() throws ParseException
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
            title = MainActivity.StaticMainActivity.getString(R.string.Title) + " " + CarData.getRegistration_Type();
            description = null;

            if (CarExpDate == null)
            {
                continue;
            }

            if (CarExpDate.equals(currentDate))
            {
                description = MainActivity.StaticMainActivity.getString(R.string.Description) + " " + CarData.getVehicle_Plate() + " " + MainActivity.StaticMainActivity.getString(R.string.Has) + " " + MainActivity.StaticMainActivity.getString(R.string.Expired) + ".";
            }
            else if (CarExpDate.equals(currentDate1))
            {
                description = MainActivity.StaticMainActivity.getString(R.string.Description) + " " + CarData.getVehicle_Plate() + " " + MainActivity.StaticMainActivity.getString(R.string.Has) + " " + 1 + " " + MainActivity.StaticMainActivity.getString(R.string.Day_Left) + ".";
            }
            else if (CarExpDate.equals(currentDate2))
            {
                description = MainActivity.StaticMainActivity.getString(R.string.Description) + " " + CarData.getVehicle_Plate() + " " + MainActivity.StaticMainActivity.getString(R.string.Has) + " " + 2 + " " + MainActivity.StaticMainActivity.getString(R.string.Days_Left) + ".";
            }
            else if (CarExpDate.equals(currentDate3))
            {
                description = MainActivity.StaticMainActivity.getString(R.string.Description) + " " + CarData.getVehicle_Plate() + " " + MainActivity.StaticMainActivity.getString(R.string.Has) + " " + 3 + " " + MainActivity.StaticMainActivity.getString(R.string.Days_Left) + ".";
            }

            if(description != null)
            {
                AddNotifications(title,description);
            }
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
    }
}