package com.smsdemo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.SparseIntArray;

/**
 * Created by dhavalrupapara on 11/1/15.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    public static final String DATABASE_NAME = "SmsDemo.db";
    public static final String SMS_TABLE_NAME = "Sms";
    public static final String SMS_COLUMN_ID = "_id";
    public static final String SMS_COLUMN_MONTH = "month";
    public static final String SMS_COLUMN_COUNTS = "counts";
    private SQLiteDatabase db;

    public DBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table " + SMS_TABLE_NAME +
                        " ( " + SMS_COLUMN_ID + " integer primary key, " + SMS_COLUMN_MONTH + " integer not null, " + SMS_COLUMN_COUNTS + " integer not null)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        //we can do changes here when we change the database verison.
    }

    public void addCounts(SparseIntArray sparseIntArray, boolean isUpdate)
    {
        try
        {
            if(sparseIntArray != null && sparseIntArray.size() > 0)
            {

                for(int i = 0 ; i < sparseIntArray.size() ; i++)
                {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(SMS_COLUMN_MONTH, sparseIntArray.keyAt(i));
                    contentValues.put(SMS_COLUMN_COUNTS, sparseIntArray.valueAt(i));
                    if(isUpdate)
                        db.update(SMS_TABLE_NAME, contentValues, SMS_COLUMN_MONTH  + " = " + sparseIntArray.keyAt(i), null);
                    else
                        db.insert(SMS_TABLE_NAME, null, contentValues);
                }
            }
        }catch (Exception e){

        }
    }

    public SparseIntArray getAllSmsCountsMonthWise()
    {
        SparseIntArray sparseIntArray = new SparseIntArray();

        Cursor c = null;
        try
        {
            c = db.query(SMS_TABLE_NAME, new String[]{SMS_COLUMN_MONTH, SMS_COLUMN_COUNTS}, null, null, null, null, null);
            c.moveToFirst();
            do{
                sparseIntArray.put(c.getInt(c.getColumnIndex(SMS_COLUMN_MONTH)), c.getInt(c.getColumnIndex(SMS_COLUMN_COUNTS)));

            }while (c.moveToNext());


        }catch (Exception e){

        }finally {
            if(c != null && !c.isClosed())
                c.close();
        }

        return sparseIntArray;
    }

    public int getSmsRowCount()
    {
        Cursor c = null;
        try
        {
            c = db.query(SMS_TABLE_NAME, null, null, null, null, null, null);
            return c.getCount();
        }catch (Exception e){

        }finally {
            if(c != null && !c.isClosed())
                c.close();
        }

        return 0;
    }

    public void openDB(){
        try
        {
            db = this.getWritableDatabase();
        }catch (Exception e){

        }
    }

    public void closeDB(){
        try
        {
            db.close();
        }catch (Exception e){

        }
    }

}
