package com.hophacks.bmoresheltr;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import com.hophacks.bmoresheltr.R;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MainActivity extends Activity {

    // current latitude
    protected double my_latitude;

    // current longitude
    protected double my_longitude;

    // flag for whether data was parsed or not (for gps search)
    protected boolean searched;

    // flag for whether data was parsed or not (for hardcoded location search)
    protected boolean defaultsearched;

    // list object that contains the list of closest locations
    private ListView my_listview;

    // all data
    protected ArrayList<SheltrData> datalist;

    // dialog to show when item is clicked
    protected AlertDialog.Builder info;

    // method called when app starts up
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // set lat/long to 0.0 (to later check if lat/long are set or not)
        my_latitude = 0.0;
        my_latitude = 0.0;
        searched = false;

        // dialog for information in list
        info = new AlertDialog.Builder(this);

        // initialize data list
        datalist = new ArrayList<SheltrData>();

        super.onCreate(savedInstanceState);

        // open the main layout
        setContentView(R.layout.activity_main);

        // get the location from the MyLocationListener inner class
        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        MyLocationListener mlocListener = new MyLocationListener();
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                mlocListener);

        // error alert for when no GPS signal is found
        final AlertDialog.Builder error = new AlertDialog.Builder(this);
        error.setTitle("No GPS");
        error.setMessage("GPS location could not be retrieved.");
        error.setNeutralButton("Close", null);

        // button that corresponds to search by GPS
        Button search = (Button) findViewById(R.id.button1);
        search.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (my_latitude == 0.0 && my_longitude == 0.0) {
                    error.show();
                } else {
                    if (!searched) {
                        parseData();
                        Collections.sort(datalist);
                        setContentView(R.layout.activity_list);
                        generateListView();
                        searched = true;
                    } else {
                        setContentView(R.layout.activity_list);
                        generateListView();
                    }
                }
            }
        });

        // button that corresponds to hardcoded location (default) search
        Button defaultsearch = (Button) findViewById(R.id.button2);
        defaultsearch.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!defaultsearched) {
                    // defaults to inner harbor if no gps is found
                    my_latitude = 39.2835;
                    my_longitude = -76.6099;
                    parseData();
                    Collections.sort(datalist);
                    setContentView(R.layout.activity_list);
                    generateListView();
                    defaultsearched = true;
                    my_latitude = 0.0;
                    my_longitude = 0.0;
                    setContentView(R.layout.activity_list);
                    generateListView();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /*
     * Get the file from assets/sheltrdata.txt and parse it. The file
     * sheltrdata.txt was converted from Excel to CSV, and then converted to a
     * .txt file. Every piece of information is comma-delimited, so all commas
     * in the information were erased before this parsing takes place (i.e. none
     * of the information fields have commas because information is separated by
     * commas).
     */
    private void parseData() {

        // open the file
        AssetManager am = getAssets();
        InputStream is = null;
        try {
            is = am.open("sheltrdata.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Scanner sc = new Scanner(is);

        // first line is the header with name of fields
        sc.nextLine();

        // parse the file by reading each line and adding the data to the data
        // list
        int count = 0;
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            System.out.println(count++);
            String[] data = line.split(",");
            SheltrData tmp = new SheltrData(data[0], data[1], data[2], data[3],
                    data[4], data[5], data[6], data[7], data[8],
                    Double.parseDouble(data[9]), -1
                            * Double.parseDouble(data[10]));
            tmp.computeDistance();
            datalist.add(tmp);
        }
    }

    // function that generates a list and makes list items clickable
    public void generateListView() {

        // make a list view (to display list items)
        my_listview = (ListView) findViewById(R.id.id_list_view);

        // add header to list view
        TextView tv = new TextView(getApplicationContext());
        tv.setText("Displaying Closest " + datalist.size() + " Locations");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, 38);
        my_listview.addHeaderView(tv);

        // generate a list of items from the data list (names of organizations)
        String[] items = new String[datalist.size()];
        int count = 0;
        for (SheltrData item : datalist) {
            items[count++] = item.getName();
        }

        // set up the list view to display
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, items);
        my_listview.setAdapter(adapter);

        // make list items clickable
        my_listview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {

                // selected item (name of organization)
                String selected = ((TextView) view).getText().toString();
                SheltrData selecteddata = null;

                // find which SheltrData object corresponds to the selected name
                for (SheltrData item : datalist) {
                    if (item.getName().equals(selected)) {
                        selecteddata = item;
                        break;
                    }
                }

                // round miles to 2 decimal places
                DecimalFormat df = new DecimalFormat("#.##");
                String dist = df.format(selecteddata.getDistance());

                // aggregate all of the information and display in dialog
                // coded in HTML because of clickable hyperlinks (URLs)
                info.setTitle(selecteddata.getName() + " - " + dist + " miles");
                String message = selecteddata.getTag() + "<br/><br/>";
                message += selecteddata.getAddress() + "<br/><br/>";
                message += "Phone: " + selecteddata.getPhone() + "<br/>";
                message += "Hours: " + selecteddata.getHours() + "<br/><br/>";
                String urldisplay = selecteddata.getURL();
                if (urldisplay.charAt(urldisplay.length() - 1) == '/') {
                    urldisplay = urldisplay.substring(0,
                            urldisplay.length() - 1);
                }
                message += "URL: " + "<a href=\"" + selecteddata.getURL()
                        + "\">" + urldisplay + "</a>" + "<br/><br/>";
                message += "Eligible: " + selecteddata.getEligible() + "<br/>";
                info.setMessage(message);
                info.setPositiveButton("Close", null);
                info.setCancelable(true);
                AlertDialog dialog = info.create();
                dialog.show();

                // make URLs in the dialogs clickable
                TextView t = (TextView) dialog
                        .findViewById(android.R.id.message);
                t.setText(Html.fromHtml(message));
                t.setAutoLinkMask(Linkify.WEB_URLS);
                t.setMovementMethod(LinkMovementMethod.getInstance());

            }
        });
    }

    // SheltrData class defining attributes of every data object
    public class SheltrData implements Comparable<SheltrData> {

        // attributes of every location
        private String name;
        private String tag;
        private String eligible;
        private String phone;
        private String hours;
        private String url;
        private String address;
        private String city;
        private String state;
        private double latitude;
        private double longitude;
        private double distance;

        // constructor that sets attributes
        public SheltrData(String name, String tag, String eligible,
                String phone, String hours, String url, String address,
                String city, String state, double latitude, double longitude) {
            this.name = name;
            this.tag = tag;
            this.eligible = eligible;
            this.phone = phone;
            this.hours = hours;
            this.url = url;
            this.address = address;
            this.city = city;
            this.state = state;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        // get methods to get the attributes
        public String getName() {
            return this.name;
        }

        public String getTag() {
            return this.tag;
        }

        public String getEligible() {
            return this.eligible;
        }

        public String getPhone() {
            return this.phone;
        }

        public String getHours() {
            return this.hours;
        }

        public String getURL() {
            return this.url;
        }

        // dialog is in html (so getAddress needs to be in html format)
        public String getAddress() {
            String addr = this.address;
            addr += "<br/>" + this.city + ", " + this.state;
            return addr;
        }

        public double getLatitude() {
            return this.latitude;
        }

        public double getLongitude() {
            return this.longitude;
        }

        public double getDistance() {
            return this.distance;
        }

        /*
         * Computes the distance between the location set by GPS (or default
         * location) and the location of this organization. Uses a simple
         * distance formula.
         */
        public double computeDistance() {
            double dlat = (my_latitude - this.latitude) * Math.PI / 180.0;
            double dlong = (my_longitude - this.longitude) * Math.PI / 180.0;
            this.distance = 3959 * Math.sqrt(dlat * dlat + dlong * dlong);
            return this.distance;
        }

        // Overridden compareTo method to compare two SheltrData objects (for
        // sorting)
        public int compareTo(SheltrData other) {
            return (int) ((this.distance - other.distance) * 1000);
        }

        // Overridden toString method for testing purposes
        public String toString() {
            return this.name;
        }

    }

    // MyLocationListener inner class that gets GPS data
    public class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            loc.getLatitude();
            loc.getLongitude();
            if (my_latitude == 0.0 && my_longitude == 0.0) {
                my_latitude = loc.getLatitude();
                my_longitude = loc.getLongitude();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

}
