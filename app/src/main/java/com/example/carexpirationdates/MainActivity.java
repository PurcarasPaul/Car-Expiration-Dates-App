package com.example.carexpirationdates;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.transition.ChangeBounds;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity
{
    @SuppressLint("StaticFieldLeak")
    //Used as a copy of the MainActivity for another file
    public static MainActivity StaticMainActivity;
    //Stores data used in tables
    public List<car_data> CarsData;

    //Stores the total number of tables on the app
    private int number_of_cars;
    //Stores the delete button of each table
    private List<Button> deleteButtons;
    //Stores the Date selector of each table
    private List<TextView> DateList;
    //Stores the money spent of each car for every table
    private Vector<Integer> MoneySpent;
    //Stores the money spent ordered by plate number
    private HashMap<String, Integer> MoneySpentCars;
    //Used for date selector
    private int GlobalId;
    //Stores the view of the toolbar
    private Toolbar toolbar;
    private DatePickerDialog.OnDateSetListener mDateSetListener;

    /*Initiates the main activity with the main view
     *and creates new lists */
    private void init()
    {
        StaticMainActivity = this;
        setContentView(R.layout.activity_main);
        toolbar  = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        number_of_cars = 0;
        deleteButtons = new ArrayList<>();
        DateList = new ArrayList<>();
        MoneySpent = new Vector<>(0);
        CarsData = new ArrayList<>();
        MoneySpentCars = new HashMap<>();

        //Database initialisation
        Paper.init(this);
    }

    //NotificationChannel used for notification manager
    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = "ExpirationDateNotification";
            String description = "A car's expiration date is going to expire.";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("ExpirationDateNotification", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass)
    {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if (serviceClass.getName().equals(service.service.getClassName()))
            {
                return true;
            }
        }
        return false;
    }

    //Starts the service if it's not running
    private void ServiceInit()
    {
        Receiver mYourService = new Receiver();
        Intent mServiceIntent = new Intent(this, mYourService.getClass());

        //Checks if the service is running
        if (!isMyServiceRunning(mYourService.getClass()))
        {
            startService(mServiceIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        init();

        //Sets an onClickListener for the "Add New Car" button
        Button add_new_vehicle = findViewById(R.id.button_add_new_vehicle);
        add_new_vehicle.setOnClickListener(AddOnClick());

        //Checks database for data, if it's not empty it will load the data into the app
        if(Paper.book().contains("CarsData"))
        {
            LoadData();
        }

        createNotificationChannel();
        startService(new Intent(this, Receiver.class));

        ServiceInit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        //Menu items, if the button save is clicked it will proceed with the saving of the data, otherwise nothing will happen
        if (id == R.id.action_settings)
        {
            return true;
        }
        else if(id==R.id.save)
        {
            SaveOnClick();
        }

        return super.onOptionsItemSelected(item);
    }

    private void RemoveAddNewCarButton(LinearLayout mLayout)
    {
        mLayout.removeView(findViewById(R.id.include_add_new_car));
        mLayout.removeView(findViewById(R.id.buttonConstraintLayout));
    }

    private void IncrementGlobals(Button tempDelete,TextView mDisplayDate,int tempMoneySpent)
    {
        number_of_cars++;
        deleteButtons.add(tempDelete);
        DateList.add(mDisplayDate);
        MoneySpent.add(tempMoneySpent);
    }

    //Builds constraints for the table
    private ConstraintSet BuildConstraints(View UsedView, ConstraintLayout ConstLayout)
    {
        ConstraintSet set = new ConstraintSet();
        set.clone(ConstLayout);

        if(number_of_cars == 0)
        {
            View toolbarLayout = findViewById(R.id.AppBarLayout2);
            set.connect(UsedView.findViewById(R.id.TableLayout).getId(), ConstraintSet.TOP,
                    toolbarLayout.getId(), ConstraintSet.BOTTOM, 0);
        }
        else
        {
            set.connect(ConstLayout.findViewById(R.id.TableLayout).getId(), ConstraintSet.TOP,
                    toolbar.getId(), ConstraintSet.BOTTOM, 0);
        }

        return set;
    }

    //Adds a new table to the main activity
    private void AddNewTable(LinearLayout mLayout,View myView)
    {
        ConstraintLayout ConstLayout = (ConstraintLayout) myView;
        ConstraintSet set = BuildConstraints(myView,ConstLayout);
        mLayout.addView(ConstLayout);

        //Animation
        Transition SlideTransition = new ChangeBounds();
        SlideTransition.setDuration(1000);
        TransitionManager.beginDelayedTransition(mLayout,SlideTransition);

        set.applyTo(ConstLayout);
    }

    //Sets the data selected by the user in the EditText
    void setDate(LinearLayout mLayout,int year,int month,int day)
    {
        month = month + 1;

        Log.d("MainActivity", "onDateSet: dd/mm/yyy: " + day + "/" + month + "/" + year);

        String date = day + "/" + month + "/" + year;

        View tempView = mLayout.findViewById(GlobalId);
        EditText tempDateStart = tempView.findViewById(R.id.editTextDateStart);
        tempDateStart.setText(date);
    }

    //Builds constraints for the "Add New Car" button
    private ConstraintSet SetConstraintsForButton(View ButtonLayout,View UsedView,ConstraintLayout ConstLayoutButton)
    {
        ConstraintSet setButton = new ConstraintSet();
        setButton.clone(ConstLayoutButton);

        setButton.connect(ButtonLayout.findViewById(R.id.button_add_new_vehicle).getId(), ConstraintSet.TOP,
                UsedView.findViewById(R.id.TableLayout).getId(), ConstraintSet.BOTTOM, 0);

        return setButton;
    }

    private void AddNewCarButton(LinearLayout mLayout,View ButtonLayout,View UsedView)
    {
        ConstraintLayout ConstLayoutButton = (ConstraintLayout) ButtonLayout.findViewById(R.id.buttonConstraintLayout);
        ConstraintSet setButton = SetConstraintsForButton(ButtonLayout,UsedView,ConstLayoutButton);
        mLayout.addView(ConstLayoutButton);

        setButton.applyTo(ConstLayoutButton);

        //Sets an onClickListener for the "Add New Car" button
        Button add_new_vehicle = findViewById(R.id.button_add_new_vehicle);
        add_new_vehicle.setOnClickListener(AddOnClick());
    }

    //Adds a new table on click
    private View.OnClickListener AddOnClick() {
        return v -> {
            LinearLayout mLayout = findViewById(R.id.linearLayout);

            LayoutInflater copy = getLayoutInflater();
            View myView = copy.inflate(R.layout.car_data,mLayout,false);
            myView.setId(number_of_cars);

            //Copies the button layout to be used later, it then deletes the button
            View ButtonLayout = copy.inflate(R.layout.add_new_car,mLayout,false);
            RemoveAddNewCarButton(mLayout);

            Button tempDelete = myView.findViewById(R.id.ButtonDelete);
            tempDelete.setOnClickListener(DeleteOnClick(number_of_cars));
            TextView mDisplayDate = myView.findViewById(R.id.editTextDateStart);
            mDisplayDate.setOnClickListener(DateOnClick(number_of_cars));
            IncrementGlobals(tempDelete,mDisplayDate,0);

            AddNewTable(mLayout,myView);

            //Adds a pop-up to select the date
            mDateSetListener = (datePicker, year, month, day) -> setDate(mLayout,year,month,day);

            AddNewCarButton(mLayout,ButtonLayout,myView);
        };
    }

    private void ReorganiseLists(LinearLayout mLayout,int table_index)
    {
        for(int i=table_index+1;i<deleteButtons.size();i++)
        {
            View tempView = mLayout.findViewById(i);
            tempView.setId(i-1);

            deleteButtons.get(i).setOnClickListener(DeleteOnClick(i-1));
            DateList.get(i).setOnClickListener(DateOnClick(i-1));
        }
    }

    private void SetConstraintsWhenDelete()
    {
        ConstraintLayout ConstLayoutButton = (ConstraintLayout) findViewById(R.id.buttonConstraintLayout);
        ConstraintSet setButton = new ConstraintSet();
        setButton.clone(ConstLayoutButton);

        if (number_of_cars > 0)
        {
            setButton.connect(findViewById(R.id.button_add_new_vehicle).getId(), ConstraintSet.TOP,
                    findViewById(R.id.TableLayout).getId(), ConstraintSet.BOTTOM, 0);
        }
        else
        {
            View toolbarLayout = findViewById(R.id.AppBarLayout2);
            setButton.connect(findViewById(R.id.button_add_new_vehicle).getId(), ConstraintSet.TOP,
                    toolbarLayout.getId(), ConstraintSet.BOTTOM, 0);
        }

        setButton.applyTo(ConstLayoutButton);
    }

    //Deletes a table on click
    private View.OnClickListener DeleteOnClick(int table_index)
    {
        return v ->
        {
            LinearLayout mLayout = findViewById(R.id.linearLayout);

            //Animation
            Transition SlideTransition = new Slide();
            SlideTransition.setDuration(1000);
            TransitionManager.beginDelayedTransition(mLayout,SlideTransition);

            //Removes the table
            mLayout.removeViewAt(table_index);

            ReorganiseLists(mLayout,table_index);

            deleteButtons.remove(table_index);
            DateList.remove(table_index);
            number_of_cars--;

            SetConstraintsWhenDelete();
        };
    }

    //Builds a pop-up for the user to select a date
    private View.OnClickListener DateOnClick(int table_index) {
        return v ->
        {
            Calendar cal = Calendar.getInstance();
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    MainActivity.this,
                    android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                    mDateSetListener,
                    year,month,day);

            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
            GlobalId = table_index;
        };
    }

    //Gets the number of months entered by the user, if nothing is inserted, the number of months will be 0
    private int GetMonths(View TempView)
    {
        EditText tempMonths = TempView.findViewById(R.id.editTextMonths);

        int iMonths = 0;
        if(!tempMonths.getText().toString().isEmpty())
        {
            iMonths = Integer.parseInt(tempMonths.getText().toString());
        }

        return iMonths;
    }

    private void SetMoneySpent(View TempView, EditText tempVehiclePlate,int i)
    {
        EditText tempMoneySpent = TempView.findViewById(R.id.editTextMoneySpent);

        if(MoneySpentCars.get(tempVehiclePlate.getText().toString()) == null)
        {
            MoneySpentCars.put(tempVehiclePlate.getText().toString(),0);
        }

        int iMoneySpent;
        if(!tempMoneySpent.getText().toString().equals(""))
        {
            iMoneySpent = Integer.parseInt(tempMoneySpent.getText().toString());

            if(MoneySpentCars.get(tempVehiclePlate.getText().toString()) != null)
            {
                MoneySpentCars.put(tempVehiclePlate.getText().toString(),MoneySpentCars.get(tempVehiclePlate.getText().toString()) + iMoneySpent);
            }
        }

        MoneySpent.set(i,MoneySpentCars.get(tempVehiclePlate.getText().toString()));
        if(MoneySpent.get(i) != 0)
        {
            tempMoneySpent.setHint(MoneySpent.get(i).toString());
        }

        tempMoneySpent.setText("");
    }

    @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    @SuppressLint("SimpleDateFormat")
    //Gets the expiration date, it's calculated by adding number of months introduced by the user and current date
    private String GetExpirationDate(EditText tempDateStart, int iMonths)
    {
        String ExpirationDate = null;
        String stringDateStart = tempDateStart.getText().toString();

        try
        {
            Date tempDate = sdf.parse(stringDateStart);
            assert tempDate != null;
            tempDate.setMonth(tempDate.getMonth() + iMonths);

            ExpirationDate = new SimpleDateFormat("dd/MM/yyyy").format(tempDate);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return ExpirationDate;
    }

    //In case there is another car with the same number plate in one of the tables they'll share the same amount of money spent
    private void FixMoneySpent()
    {
        LinearLayout mLayout = findViewById(R.id.linearLayout);

        for (int i = 0; i < CarsData.size(); i++)
        {
            View TempView = mLayout.findViewById(i);

            EditText tempVehiclePlate = TempView.findViewById(R.id.editTextVehiclePlate);
            EditText tempMoneySpent = TempView.findViewById(R.id.editTextMoneySpent);

            MoneySpent.set(i,MoneySpentCars.get(tempVehiclePlate.getText().toString()));

            if(MoneySpent.get(i) == null)
            {
                continue;
            }

            if(MoneySpent.get(i) != 0)
            {
                tempMoneySpent.setHint(MoneySpent.get(i).toString());
            }

            CarsData.get(i).setMoney_Spent(MoneySpent.get(i));
        }
    }

    //Saves the data from every table in a database
    private void SaveOnClick()
    {
        LinearLayout mLayout = findViewById(R.id.linearLayout);

        CarsData = new ArrayList<>();
        //Deletes the old data from the database
        Paper.book().delete("CarsData");

        //if there is a valid table it will proceed with the save
        if(number_of_cars > 0)
        {
            for (int i = 0; i < number_of_cars; i++)
            {
                View TempView = mLayout.findViewById(i);

                if(TempView  == null)
                {
                    continue;
                }

                EditText tempVehiclePlate = TempView.findViewById(R.id.editTextVehiclePlate);
                EditText tempRegistrationType = TempView.findViewById(R.id.editTextRegistrationType);
                EditText tempDateStart = TempView.findViewById(R.id.editTextDateStart);
                EditText tempDateFinish = TempView.findViewById(R.id.editTextDateFinish);

                int iMonths = GetMonths(TempView);
                SetMoneySpent(TempView,tempVehiclePlate,i);

                String stringDateStart = tempDateStart.getText().toString();
                String ExpirationDate = GetExpirationDate(tempDateStart,iMonths);
                tempDateFinish.setText(ExpirationDate);

                //Creates a new object of type car_data and adds it to the list
                car_data tempCarData = new car_data(tempVehiclePlate.getText().toString(),tempRegistrationType.getText().toString(),
                        stringDateStart,ExpirationDate,iMonths,MoneySpent.get(i));
                CarsData.add(tempCarData);
            }

            FixMoneySpent();
            startService(new Intent(this, Receiver.class));
        }

        //Adds data to the database
        if(CarsData.size() > 0)
        {
            Paper.book().write("CarsData",CarsData);
            Paper.book().write("MoneySpentCars",MoneySpentCars);
        }

    }

    //On load it will add the data from the database to table
    private void AddDataToTable(View myView, car_data CarDataTemp)
    {
        EditText tempVehiclePlate = myView.findViewById(R.id.editTextVehiclePlate);
        EditText tempRegistrationType = myView.findViewById(R.id.editTextRegistrationType);
        EditText tempMonths = myView.findViewById(R.id.editTextMonths);
        EditText tempDateStart = myView.findViewById(R.id.editTextDateStart);
        EditText tempDateFinish = myView.findViewById(R.id.editTextDateFinish);
        EditText tempMoneySpent = myView.findViewById(R.id.editTextMoneySpent);

        tempVehiclePlate.setText(CarDataTemp.getVehicle_Plate());
        tempRegistrationType.setText(CarDataTemp.getRegistration_Type());
        tempMonths.setText(Integer.toString(CarDataTemp.getMonths()));
        tempDateStart.setText(CarDataTemp.getStart());
        tempDateFinish.setText(CarDataTemp.getFinish());
        tempMoneySpent.setHint(Integer.toString(CarDataTemp.getMoney_Spent()));
    }

    @SuppressLint("SetTextI18n")
    //Adds the data from the database to the app
    private void AddCarData(car_data CarDataTemp)
    {
        LinearLayout mLayout = findViewById(R.id.linearLayout);

        LayoutInflater copy = getLayoutInflater();
        View myView = copy.inflate(R.layout.car_data,mLayout,false);
        myView.setId(number_of_cars);

        View ButtonLayout = copy.inflate(R.layout.add_new_car,mLayout,false);
        RemoveAddNewCarButton(mLayout);

        Button tempDelete = myView.findViewById(R.id.ButtonDelete);
        tempDelete.setOnClickListener(DeleteOnClick(number_of_cars));
        TextView mDisplayDate = myView.findViewById(R.id.editTextDateStart);
        mDisplayDate.setOnClickListener(DateOnClick(number_of_cars));
        IncrementGlobals(tempDelete,mDisplayDate,CarDataTemp.getMoney_Spent());

        AddDataToTable(myView,CarDataTemp);
        mLayout.addView(myView);

        mDateSetListener = (datePicker, year, month, day) -> setDate(mLayout,year,month,day);

        AddNewCarButton(mLayout,ButtonLayout,myView);
    }

    //Loads the data from the database
    private void LoadData()
    {
        List<car_data> LoadCarData;

        LoadCarData = Paper.book().read("CarsData", new ArrayList<>());
        CarsData = Paper.book().read("CarsData", new ArrayList<>());
        MoneySpentCars = Paper.book().read("MoneySpentCars",new HashMap<>());

        for(int i=0;i<LoadCarData.size();i++)
        {
            AddCarData(LoadCarData.get(i));
        }
    }

    @Override
    //On destroy it will restart the service
    protected void onDestroy() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
        super.onDestroy();
    }
}