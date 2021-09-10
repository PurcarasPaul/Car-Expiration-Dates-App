package com.example.carexpirationdates;

import java.io.Serializable;

public class car_data implements Serializable
{
    private final String Vehicle_Plate, Registration_Type, Start, Finish;
    private final int Months;
    private int Money_Spent;

    car_data(String Vehicle_Plate,String Registration_Type,String Start,String Finish,int Months,int MoneySpent)
    {
        this.Vehicle_Plate=Vehicle_Plate;
        this.Registration_Type=Registration_Type;
        this.Start=Start;
        this.Finish=Finish;
        this.Months=Months;
        this.Money_Spent=MoneySpent;
    }

    public String getVehicle_Plate() {
        return Vehicle_Plate;
    }

    public String getRegistration_Type() {
        return Registration_Type;
    }

    public String getStart() {
        return Start;
    }

    public String getFinish() {
        return Finish;
    }

    public int getMonths() {
        return Months;
    }

    public int getMoney_Spent() {
        return Money_Spent;
    }

    public void setMoney_Spent(int money_Spent) {
        Money_Spent = money_Spent;
    }
}
