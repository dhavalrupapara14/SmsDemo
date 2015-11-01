package com.smsdemo;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.Telephony;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    private DBHelper dbHelper;
    private final int SMS_PERMISSION_REQUEST = 20;
    private BarChart mChart;
    private SparseIntArray smsCountsArray = new SparseIntArray(12);
    private ProgressDialog progressDialog;
    private static final int ALL = 1;
    private static final int INBOX = 2;
    private static final int DRAFTS = 3;
    private static final int OUTBOX = 4;
    private static final int SENT = 5;
    private static final int CONVERSATIONS = 6;
    private int smsType = INBOX;
    private FetchSmsTask smsTask;
    private TextView tvSmsType;
    private Spinner spYear;
    public static final String CURRENT_YEAR = "Current Year";
    public static final String ALL_YEARS = "All Years";
    private String[] yearsArray = new String[]{CURRENT_YEAR,ALL_YEARS};
    private int yearSpinnerIndex = 0;
    private static final String KEY_SMS_TYPE = "sms_type";
    private static final String KEY_SMS_YEARS_SPINNER_INDEX = "sms_years_spinner_index";
    private static final String KEY_SMS_COUNT_ARRAY_KEYS = "sms_count_array_keys";
    private static final String KEY_SMS_COUNT_ARRAY_VALUES = "sms_count_array_values";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        dbHelper.openDB();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);

        tvSmsType = (TextView) findViewById(R.id.tvSmsType);

        initializeChart();

        spYear = (Spinner) findViewById(R.id.spYear);
        ArrayAdapter spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, yearsArray);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spYear.setAdapter(spinnerAdapter);
        spYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (yearSpinnerIndex != position) {
                    yearSpinnerIndex = position;
                    smsTask = new FetchSmsTask();
                    smsTask.execute(smsType);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if(savedInstanceState != null)
        {
            smsType = savedInstanceState.getInt(KEY_SMS_TYPE);
            yearSpinnerIndex = savedInstanceState.getInt(KEY_SMS_YEARS_SPINNER_INDEX);
            spYear.setSelection(yearSpinnerIndex);
            int[] keys = savedInstanceState.getIntArray(KEY_SMS_COUNT_ARRAY_KEYS);
            int[] values = savedInstanceState.getIntArray(KEY_SMS_COUNT_ARRAY_VALUES);
            try
            {
                for(int i = 0; i < keys.length; i ++)
                {
                    smsCountsArray.put(keys[i], values[i]);
                }

                setTitle();
                loadDataInChart();
            }catch (Exception e){

            }

        }else
        {
            //---SMS fetch start
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                        requestSmsPermission();
                    }else
                    {
                        smsTask = new FetchSmsTask();
                        smsTask.execute(smsType);
                    }
                }else
                {
                    smsTask = new FetchSmsTask();
                    smsTask.execute(smsType);
                }
            } catch (Exception e) {
            }
        }
    }

    /*@Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {

        super.onSaveInstanceState(outState, outPersistentState);
    }*/

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SMS_TYPE, smsType);
        int[] keys = new int[smsCountsArray.size()];
        int[] values = new int[smsCountsArray.size()];
        for(int i = 0; i < smsCountsArray.size(); i ++)
        {
            keys[i] = smsCountsArray.keyAt(i);
            values[i] = smsCountsArray.valueAt(i);
        }
        outState.putIntArray(KEY_SMS_COUNT_ARRAY_KEYS, keys);
        outState.putIntArray(KEY_SMS_COUNT_ARRAY_VALUES, values);
        outState.putInt(KEY_SMS_YEARS_SPINNER_INDEX, yearSpinnerIndex);
        super.onSaveInstanceState(outState);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestSmsPermission() {
        try {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)) {
                Toast.makeText(MainActivity.this,"App needs access to your sms", Toast.LENGTH_LONG).show();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        requestPermissions(new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_REQUEST);
                    }
                }, Toast.LENGTH_LONG + 500);
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == SMS_PERMISSION_REQUEST) {
            try {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    smsTask = new FetchSmsTask();
                    smsTask.execute(smsType);
                } else {
                    // Permission denied
                    Toast.makeText(MainActivity.this,"App needs access to your sms", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try
        {
            //close the DB.
            dbHelper.closeDB();

            //cancel async task if it is running.
            if(smsTask.getStatus() != AsyncTask.Status.FINISHED)
                smsTask.cancel(true);

        }catch (Exception e){

        }

    }

    public void fetchSms(int type)
    {
        smsCountsArray.put(Calendar.JANUARY,0);
        smsCountsArray.put(Calendar.FEBRUARY,0);
        smsCountsArray.put(Calendar.MARCH,0);
        smsCountsArray.put(Calendar.APRIL,0);
        smsCountsArray.put(Calendar.MAY,0);
        smsCountsArray.put(Calendar.JUNE,0);
        smsCountsArray.put(Calendar.JULY,0);
        smsCountsArray.put(Calendar.AUGUST,0);
        smsCountsArray.put(Calendar.SEPTEMBER,0);
        smsCountsArray.put(Calendar.OCTOBER,0);
        smsCountsArray.put(Calendar.NOVEMBER,0);
        smsCountsArray.put(Calendar.DECEMBER, 0);


        Cursor cursor = null;
        try
        {
            if(type == ALL)
            {
                if(Build.VERSION.SDK_INT >= 19)
                {
                    cursor = getContentResolver().query(Telephony.Sms.CONTENT_URI, new String[]{Telephony.Sms.DATE},null,null,Telephony.Sms.DATE + " DESC");
                }else
                {
                    cursor = getContentResolver().query(Uri.parse("content://sms"), new String[]{"date"},null,null,"date DESC");
                }

            }else if(type == INBOX)
            {
                if(Build.VERSION.SDK_INT >= 19)
                {
                    cursor = getContentResolver().query(Telephony.Sms.Inbox.CONTENT_URI, new String[]{Telephony.Sms.Inbox.DATE},null,null,Telephony.Sms.Inbox.DATE + " DESC");
                }else
                {
                    cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), new String[]{"date"},null,null,"date DESC");
                }

            }else if(type == SENT)
            {
                if(Build.VERSION.SDK_INT >= 19)
                {
                    cursor = getContentResolver().query(Telephony.Sms.Sent.CONTENT_URI, new String[]{Telephony.Sms.Sent.DATE},null,null,Telephony.Sms.Sent.DATE + " DESC");
                }else
                {
                    cursor = getContentResolver().query(Uri.parse("content://sms/sent"), new String[]{"date"},null,null,"date DESC");
                }

            }else if(type == DRAFTS)
            {
                if(Build.VERSION.SDK_INT >= 19)
                {
                    cursor = getContentResolver().query(Telephony.Sms.Draft.CONTENT_URI, new String[]{Telephony.Sms.Draft.DATE},null,null,Telephony.Sms.Draft.DATE + " DESC");
                }else
                {
                    cursor = getContentResolver().query(Uri.parse("content://sms/draft"), new String[]{"date"},null,null,"date DESC");
                }

            }else if(type == OUTBOX)
            {
                if(Build.VERSION.SDK_INT >= 19)
                {
                    cursor = getContentResolver().query(Telephony.Sms.Outbox.CONTENT_URI, new String[]{Telephony.Sms.Outbox.DATE},null,null,Telephony.Sms.Outbox.DATE + " DESC");
                }else
                {
                    cursor = getContentResolver().query(Uri.parse("content://sms/outbox"), new String[]{"date"},null,null,"date DESC");
                }

            }else if(type == CONVERSATIONS)
            {
                if(Build.VERSION.SDK_INT >= 19)
                {
                    cursor = getContentResolver().query(Telephony.MmsSms.CONTENT_CONVERSATIONS_URI, new String[]{Telephony.Sms.Conversations.DATE},null,null,Telephony.Sms.Conversations.DATE + " DESC");
                }else
                {
                    cursor = getContentResolver().query(Uri.parse("content://mms-sms/conversations"), new String[]{"date"},null,null,"date DESC");
                }

            }

            cursor.moveToFirst();

            Calendar cal = Calendar.getInstance();
            while  (cursor.moveToNext())
            {
                try
                {
                    System.out.println("Date = "+cursor.getString(0));

                    cal.setTimeInMillis(Long.parseLong(cursor.getString(0)));
                    int month = cal.get(Calendar.MONTH);

                    if(yearsArray[yearSpinnerIndex].equalsIgnoreCase(CURRENT_YEAR)) {

                        //if year is not current year then exit from the iteration.
                        if (cal.get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR))
                            break;
                    }
//                    if (smsCountsArray.indexOfKey(month) < 0)
//                        smsCountsArray.put(month, 0);

                    int temp = smsCountsArray.get(month) + 1;
                    smsCountsArray.put(month, temp);
                }catch (Exception e){

                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(cursor != null &&  !cursor.isClosed())
                cursor.close();
        }

        //insert or update into DB.
        if(dbHelper.getSmsRowCount() > 0)
            dbHelper.addCounts(smsCountsArray, true);
        else
            dbHelper.addCounts(smsCountsArray, false);

        //fetch all.
        smsCountsArray = dbHelper.getAllSmsCountsMonthWise();

        for(int i= 0; i <smsCountsArray.size(); i++)
        {
            System.out.println("key=" + smsCountsArray.keyAt(i) + ",value="+ smsCountsArray.valueAt(i));
        }
    }

    private void initializeChart()
    {
        //-----chart Start
        mChart = (BarChart) findViewById(R.id.chartBar);
        mChart.setOnChartValueSelectedListener(this);
        mChart.setDrawBarShadow(false);
        mChart.setDrawValueAboveBar(true);
        mChart.setDescription("");
        // if more than 60 entries are displayed in the chart, no values will be
        // drawn
        mChart.setMaxVisibleValueCount(60);

        // scaling can now only be done on x- and y-axis separately
        mChart.setPinchZoom(false);

        mChart.setDrawGridBackground(false);
        // mChart.setDrawYLabels(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setSpaceBetweenLabels(2);

//        YAxisValueFormatter custom = new MyYAxisValueFormatter();

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setLabelCount(8, false);
//        leftAxis.setValueFormatter(custom);
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);

        mChart.getAxisRight().setEnabled(false);

        Legend l = mChart.getLegend();
        l.setPosition(Legend.LegendPosition.BELOW_CHART_LEFT);
        l.setForm(Legend.LegendForm.SQUARE);
        l.setFormSize(9f);
        l.setTextSize(11f);
        l.setXEntrySpace(4f);
    }

    private void loadDataInChart()
    {
        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<BarEntry> yVals = new ArrayList<BarEntry>();

        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        for(int i= 0; i<= currentMonth; i++)
        {
            xVals.add(getMonthName(smsCountsArray.keyAt(i)));
            yVals.add(new BarEntry(smsCountsArray.valueAt(i), i));
        }

        BarDataSet set = new BarDataSet(yVals, "SMS Counts");
        set.setBarSpacePercent(35f);

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        dataSets.add(set);

        BarData data = new BarData(xVals, dataSets);
        data.setValueTextSize(10f);
//        data.setValueTypeface(mTf);

        mChart.setData(data);
        mChart.invalidate();
    }

    private String getMonthName(int month)
    {
        switch (month)
        {
            case Calendar.JANUARY:
                return "Jan";

            case Calendar.FEBRUARY:
                return "Feb";

            case Calendar.MARCH:
                return "Mar";

            case Calendar.APRIL:
                return "Apr";

            case Calendar.MAY:
                return "May";

            case Calendar.JUNE:
                return "Jun";

            case Calendar.JULY:
                return "Jul";

            case Calendar.AUGUST:
                return "Aug";

            case Calendar.SEPTEMBER:
                return "Sep";

            case Calendar.OCTOBER:
                return "Oct";

            case Calendar.NOVEMBER:
                return "Nov";

            case Calendar.DECEMBER:
                return "Dec";

        }

        return "";
    }

    @Override
    public void onValueSelected(Entry entry, int i, Highlight highlight) {

    }

    @Override
    public void onNothingSelected() {

    }

    private class FetchSmsTask extends AsyncTask<Integer, Void, Void>
    {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            if(!progressDialog.isShowing())
                progressDialog.show();
        }

        @Override
        protected Void doInBackground(Integer... params) {
            fetchSms(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if(progressDialog.isShowing())
                progressDialog.dismiss();

            setTitle();
            loadDataInChart();
        }
    }

    private void setTitle()
    {
        String temp1 = "Month wise ";
        String temp2 = " Sms Counts";
        switch (smsType)
        {
            case ALL:
                tvSmsType.setText(temp1 + "All" + temp2);
                break;

            case INBOX:
                tvSmsType.setText(temp1 + "Inbox" + temp2);
                break;

            case DRAFTS:
                tvSmsType.setText(temp1 + "Drafts" + temp2);
                break;

            case OUTBOX:
                tvSmsType.setText(temp1 + "Outbox" + temp2);
                break;

            case SENT:
                tvSmsType.setText(temp1 + "Sent" + temp2);
                break;

            case CONVERSATIONS:
                tvSmsType.setText(temp1 + "Conversations" + temp2);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.smsAll:
                smsType = ALL;
                break;

            case R.id.smsDrafts:
                smsType = DRAFTS;
                break;

            case R.id.smsInbox:
                smsType = INBOX;
                break;

            case R.id.smsOutbox:
                smsType = OUTBOX;
                break;

            case R.id.smsSent:
                smsType = SENT;
                break;

            case R.id.smsConversations:
                smsType = CONVERSATIONS;
                break;
        }

        smsTask = new FetchSmsTask();
        smsTask.execute(smsType);
        return super.onOptionsItemSelected(item);
    }
}
