package com.example.airdataapp;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity
{

    FusedLocationProviderClient mFusedLocationClient;


    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    boolean bool_button = false;


    double latitudine_current=0;
    double longitudine_current=0;

    Context context = this;
    DBHelper mydb;

    List<Address> addresses;


    volatile boolean stopWorker;
    private final String BACKEND_ADDRESS = "10.0.215.1:8000";
    private String data;
    ArrayList<String> list_of_measure = new ArrayList<String>();
    private String source,measure_pm, measure_humidity, measure_temperature;
    private int temperature, humidity, pm1,pm25, pm10;

    private double avg_humidity, avg_temperature, avg_pm1, avg_pm25, avg_pm10;
    private int max_humidity, max_temperature, max_pm1, max_pm25, max_pm10;
    private int min_humidity, min_temperature, min_pm1, min_pm25, min_pm10;

    private TextView pm1_value;
    private TextView pm25_value;
    private TextView pm10_value;
    private TextView temperatura_value;
    private TextView umidita_value;
    private String userEmail;

    private FirebaseAuth auth;

    public Geocoder geocoder;

    private String loading = "Rilevamento...";


    //funzione per il calcolo della media

    private double calculateAverage(ArrayList <Integer> marks) {
        Integer sum = 0;
        if(!marks.isEmpty()) {
            for (Integer mark : marks) {
                sum += mark;
            }
            return sum.doubleValue() / marks.size();
        }
        return sum;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        mydb = new DBHelper(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        auth = FirebaseAuth.getInstance();
        userEmail = auth.getCurrentUser().getEmail();

        Button openButton = (Button)findViewById(R.id.open);
        Button closeButton = (Button)findViewById(R.id.close);
        Button mapsButton = (Button)findViewById(R.id.mappa);

        pm1_value = findViewById(R.id.pm1_value);
        pm25_value = findViewById(R.id.pm25_value);
        pm10_value = findViewById(R.id.pm10_value);
        temperatura_value = findViewById(R.id.temperatura_value);
        umidita_value = findViewById(R.id.umidita_value);

        pm1_value.setText(loading);
        pm25_value.setText(loading);
        pm10_value.setText(loading);
        temperatura_value.setText(loading);
        umidita_value.setText(loading);

        TableLayout table = findViewById(R.id.lastMeasurement);

        TableRow headers = new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        headers.setLayoutParams(lp);

        TextView pm1_header = new TextView(this);
        TextView pm25_header = new TextView(this);
        TextView pm10_header = new TextView(this);

        TextView date_header = new TextView(this);
        TextView address_header = new TextView(this);

        pm1_header.setText("PM1   ");
        pm25_header.setText("PM2.5   ");
        pm10_header.setText("PM10   ");
        date_header.setText("Data   ");
        address_header.setText("Luogo   ");

        headers.addView(pm1_header);
        headers.addView(pm25_header);
        headers.addView(pm10_header);
        headers.addView(date_header);
        headers.addView(address_header);

        table.addView(headers, 0);

        Cursor rilevazioni = mydb.getDataForCurrentUser(userEmail);

        geocoder = new Geocoder(this, Locale.getDefault());
/*
        TimeZone tz = TimeZone.getTimeZone("GMT+2");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        String nowAsISO = df.format(new Date());
*/






        int i = 1;
        while(rilevazioni.moveToNext())
        {
            TableRow row = new TableRow(this);
            TableRow.LayoutParams lp1 = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp1);
            TextView pm1_old = new TextView(this);
            TextView pm25_old = new TextView(this);
            TextView pm10_old = new TextView(this);
            TextView date_old = new TextView(this);
            TextView address_old = new TextView(this);

            String pm1_round;
            String pm25_round;
            String pm10_round;

            //double lat= Double.parseDouble(rilevazioni.getString(7));
            //double lng = Double.parseDouble(rilevazioni.getString(8));

            double lat_misure= rilevazioni.getDouble(7);
            double lng_misure = rilevazioni.getDouble(8);

            System.out.println("MAIN ACTIVITY LAT: "+ lat_misure);
            System.out.println("MAIN ACTIVITY LNG: "+ lng_misure);

            try {
                addresses = geocoder.getFromLocation(lat_misure,lng_misure,1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String address =  addresses.get(0).getLocality(); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()


            System.out.println("INDIRIZZO: "+ address+"---");

            if (rilevazioni.getString(2).length() > 3)
            {
                pm1_round = rilevazioni.getString(2).substring(0,3);
            }
            else
            {
                pm1_round = rilevazioni.getString(2);
            }

            if (rilevazioni.getString(3).length() > 3)
            {
                pm25_round = rilevazioni.getString(3).substring(0,3);
            }
            else
            {
                pm25_round = rilevazioni.getString(3);
            }

            if (rilevazioni.getString(4).length() > 3)
            {
                pm10_round = rilevazioni.getString(4).substring(0,3);
            }
            else
            {
                pm10_round = rilevazioni.getString(4);
            }

            //mydb.inserisci_misurazione(source, rilevazioni.getDouble(2), rilevazioni.getDouble(3), rilevazioni.getDouble(4), 0.0, 0.0, latitudine_current, longitudine_current, nowAsISO, userEmail);


            pm1_old.setText(pm1_round);
            pm25_old.setText(pm25_round);
            pm10_old.setText(pm10_round);
            date_old.setText(rilevazioni.getString(9).substring(0,10)+"  ");
            address_old.setText(address);
            row.addView(pm1_old);
            row.addView(pm25_old);
            row.addView(pm10_old);
            row.addView(date_old);
            row.addView(address_old);

            table.addView(row, i);
            i++;
        }

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    findBT();
                    openBT();
                    bool_button = true;

                }
                catch (IOException ex) { }
            }
        });


        //Close button
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                    bool_button = false;
                }
                catch (IOException ex) { }
            }
        });


        mapsButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v) {

                if (bool_button==true) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setCancelable(true);
                    builder.setMessage("Termina la rilevazione prima di proseguire");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

                else {
                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);}
            }
        });



    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            System.out.println("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("Airbeam2:001896104600"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
    }

    void openBT() throws IOException
    {

        if (mmDevice != null) { //IF verifica se sei connesso ad airbeam
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        beginListenForData();}
        else
        {
            System.out.println("NON CONNSESSO AD AIRBEAM");
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(true);
            builder.setMessage("Connettiti ad AIRBEAM tramite bluetooth");
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        //svuoto l'array di misure ogni volta che apro una connessione
        list_of_measure = new ArrayList<String>();

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    data = new String(encodedBytes, "UTF-8");
                                    System.out.println(data);

                                    new Handler(Looper.getMainLooper()).post(new Runnable(){
                                        @Override
                                        public void run() {
                                            if (data.indexOf("Temperature")!= -1){
                                                String[] result = data.split(";");
                                                temperatura_value.setText(result[0]);
                                            }
                                            if (data.indexOf("Humidity")!= -1){
                                                String[] result = data.split(";");
                                                umidita_value.setText(result[0]);
                                            }
                                            if (data.indexOf("AirBeam2-PM1") != -1 && data.indexOf("0;12;35;55;150") != -1) {
                                                String[] result = data.split(";");
                                                pm1_value.setText(result[0]);
                                            }
                                            if (data.indexOf("AirBeam2-PM2.5") != -1){
                                                String[] result = data.split(";");
                                                pm25_value.setText(result[0]);
                                            }
                                            if (data.indexOf("AirBeam2-PM10") != -1 && data.indexOf("0;20;50;100;200") != -1) {
                                                String[] result = data.split(";");
                                                pm10_value.setText(result[0]);
                                            }
                                        }
                                    });

                                    list_of_measure.add(data);
                                    if(list_of_measure.size() == 100) {
                                        int m = 0;
                                        int n = 0;
                                        //dichiaro le stringhe di valori che uso per calcolare max, min, media delle quantità
                                        ArrayList<Integer> list_of_PM1 = new ArrayList<>();
                                        ArrayList<Integer> list_of_PM25 = new ArrayList<>();
                                        ArrayList<Integer> list_of_PM10 = new ArrayList<>();
                                        ArrayList<Integer> list_of_humidity = new ArrayList<>();
                                        ArrayList<Integer> list_of_temperature = new ArrayList<>();

                                        for (m = 0; m < list_of_measure.size(); m = m + 5) {
                                            try {
                                                for (n = m; n < m + 5; n++) {

                                                    if (list_of_measure.get(n).indexOf("Temperature") != -1) {
                                                        String[] result = list_of_measure.get(n).split(";");
                                                        temperature = Integer.parseInt(result[0]);
                                                        source = result[1];
                                                        list_of_temperature.add(temperature);
                                                        measure_temperature = result[5];
                                                    }
                                                    ;

                                                    if (list_of_measure.get(n).indexOf("Humidity") != -1) {
                                                        String[] result = list_of_measure.get(n).split(";");
                                                        humidity = Integer.parseInt(result[0]);
                                                        source = result[1];
                                                        list_of_humidity.add(humidity);
                                                        measure_humidity = result[5];
                                                    }
                                                    ;

                                                    if (list_of_measure.get(n).indexOf("AirBeam2-PM1") != -1 && list_of_measure.get(n).indexOf("0;12;35;55;150") != -1) {
                                                        String[] result = list_of_measure.get(n).split(";");
                                                        pm1 = Integer.parseInt(result[0]);
                                                        source = result[1];
                                                        list_of_PM1.add(pm1);
                                                        measure_pm = result[5];
                                                    }
                                                    ;

                                                    if (list_of_measure.get(n).indexOf("AirBeam2-PM2.5") != -1) {
                                                        String[] result = list_of_measure.get(n).split(";");
                                                        pm25 = Integer.parseInt(result[0]);
                                                        source = result[1];
                                                        list_of_PM25.add(pm25);
                                                        measure_pm = result[5];
                                                    }
                                                    ;

                                                    if (list_of_measure.get(n).indexOf("AirBeam2-PM10") != -1 && list_of_measure.get(n).indexOf("0;20;50;100;200") != -1) {
                                                        String[] result = list_of_measure.get(n).split(";");
                                                        pm10 = Integer.parseInt(result[0]);
                                                        source = result[1];
                                                        list_of_PM10.add(pm10);
                                                        measure_pm = result[5];
                                                    }
                                                    ;
                                                }
                                            }
                                            catch (Exception e) {
                                                Log.d("Exception!", e.getLocalizedMessage());
                                            }
                                            };

                                    // calcolo media, max e min delle varie quantità

                                    // humidity
                                    avg_humidity = calculateAverage(list_of_humidity);
                                    max_humidity = Collections.max(list_of_humidity);
                                    min_humidity = Collections.min(list_of_humidity);

                                    // temperature
                                    avg_temperature = calculateAverage(list_of_temperature);
                                    max_temperature = Collections.max(list_of_temperature);
                                    min_temperature = Collections.min(list_of_temperature);

                                    // pm1
                                    avg_pm1 = calculateAverage(list_of_PM1);
                                    max_pm1 = Collections.max(list_of_PM1);
                                    min_pm1 = Collections.min(list_of_PM1);

                                    // pm2.5
                                    avg_pm25 = calculateAverage(list_of_PM25);
                                    max_pm25 = Collections.max(list_of_PM25);
                                    min_pm25 = Collections.min(list_of_PM25);

                                    // pm10
                                    avg_pm10 = calculateAverage(list_of_PM10);
                                    max_pm10 = Collections.max(list_of_PM10);
                                    min_pm10 = Collections.min(list_of_PM10);

                                    try {

                                        //get device position

                                        //final double lng;
                                        //final double lat;


                                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                                            return;
                                        } else {


                                            if (checkPermissions()) {
                                                if (isLocationEnabled()) {
                                                    mFusedLocationClient.getLastLocation().addOnCompleteListener(
                                                            new OnCompleteListener<Location>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Location> task) {
                                                                    Location location = task.getResult();
                                                                    if (location == null) {
                                                                        requestNewLocationData();
                                                                    } else {

                                                                        latitudine_current = location.getLatitude();
                                                                        longitudine_current = location.getLongitude();
                                                                    }
                                                                }
                                                            }
                                                    );
                                                } else {
                                                    //Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show();

                                                    System.out.println("SONO NELL'ELSE DOPO LA LOCALIZZAIONE qualcosa è andato storto");

                                                }
                                            } else {
                                                requestPermissions();
                                            }


                                            //lng = location.getLongitude();
                                            //lat = location.getLatitude();


                                            //getLastLocation();


                                        }

                                        //get current date

                                        TimeZone tz = TimeZone.getTimeZone("GMT+2");
                                        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); // Quoted "Z" to indicate UTC, no timezone offset
                                        df.setTimeZone(tz);
                                        String nowAsISO = df.format(new Date());

                                        //Inserisco i dati sul database locale MYSQLite

                                        mydb.inserisci_misurazione(source, avg_pm1, avg_pm25, avg_pm10, avg_temperature, avg_humidity, latitudine_current , longitudine_current, nowAsISO, userEmail);

                                        int count = mydb.getCount();

                                        System.out.println("NUMERO DI MISURAZIONI SALVATE: "+ count);

                                        JSONObject pm1_measurement = new JSONObject();
                                        pm1_measurement.put("value", avg_pm1);
                                        pm1_measurement.put("max_value", max_pm1);
                                        pm1_measurement.put("min_value", min_pm1);
                                        pm1_measurement.put("unitMeasure", measure_pm);

                                        JSONObject pm25_measurement = new JSONObject();
                                        pm25_measurement.put("value", avg_pm25);
                                        pm25_measurement.put("max_value", max_pm25);
                                        pm25_measurement.put("min_value", min_pm25);
                                        pm25_measurement.put("unitMeasure", measure_pm);


                                        JSONObject pm10_measurement = new JSONObject();
                                        pm10_measurement.put("value", avg_pm10);
                                        pm10_measurement.put("max_value", max_pm10);
                                        pm10_measurement.put("min_value", min_pm10);
                                        pm10_measurement.put("unitMeasure", measure_pm);

                                        JSONObject temperature_measurement = new JSONObject();
                                        temperature_measurement.put("value", avg_temperature);
                                        temperature_measurement.put("max_value", max_temperature);
                                        temperature_measurement.put("min_value", min_temperature);
                                        temperature_measurement.put("unitMeasure", measure_temperature);

                                        JSONObject humidity_measurement = new JSONObject();
                                        humidity_measurement.put("value", avg_humidity);
                                        humidity_measurement.put("max_value", max_humidity);
                                        humidity_measurement.put("min_value", min_humidity);
                                        humidity_measurement.put("unitMeasure", measure_humidity);

                                        JSONObject jsonParam = new JSONObject();
                                        jsonParam.put("source", source);
                                        jsonParam.put("PM1", pm1_measurement);
                                        jsonParam.put("PM2_5", pm25_measurement);
                                        jsonParam.put("PM10", pm10_measurement);
                                        jsonParam.put("temperature", temperature_measurement);
                                        jsonParam.put("relative_humidity", humidity_measurement);
                                        jsonParam.put("longitude", longitudine_current);
                                        jsonParam.put("latitude", latitudine_current);
                                        jsonParam.put("timestamp", nowAsISO);
                                        jsonParam.put("userID", userEmail);

                                        //Inserisco le misurazioni nella collection di mongo

                                        URL url = new URL("http://" + BACKEND_ADDRESS + "/misurazioni");
                                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                        conn.setRequestMethod("POST");
                                        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                                        conn.setRequestProperty("Accept", "application/json");
                                        conn.setDoOutput(true);
                                        conn.setDoInput(true);

                                        DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                                        //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                                        os.writeBytes(jsonParam.toString());

                                        os.flush();
                                        os.close();

                                        Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                                        Log.i("MSG", conn.getResponseMessage());

                                        conn.disconnect();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                            list_of_measure = new ArrayList<String>();
                            System.out.println(list_of_measure);
                        }

                        readBufferPosition = 0;

                        handler.post(new Runnable()
                        {
                            public void run()
                            {
                                System.out.println(data);
                            }
                        });
                    }
                    else
                    {
                        readBuffer[readBufferPosition++] = b;
                    }
                }
            }
        }
        catch (IOException ex)
        {
            stopWorker = true;
        }
                }
            }
        });

        workerThread.start();
    }


    void closeBT() throws IOException
    {
        if (mmOutputStream != null) {

        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        System.out.println("Bluetooth Closed");}
        else{
            System.out.println("NON sei connesso");

            //Messaggio quando clicca close e non sei connesso ad AIRBEAM
            /*AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setCancelable(true);
            builder.setMessage("Non sei connesso ad AIRBEAM");
            AlertDialog dialog = builder.create();
            dialog.show();*/
        }
    }


    //MEtodi per localizzazione corrente
    int PERMISSION_ID = 44;



    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ID
        );
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_ID) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Granted. Start getting the location information
            }
        }
    }



    @SuppressLint("MissingPermission")
    private void getLastLocation(){
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.getLastLocation().addOnCompleteListener(
                        new OnCompleteListener<Location>() {
                            @Override
                            public void onComplete(@NonNull Task<Location> task) {
                                Location location = task.getResult();
                                if (location == null) {
                                    requestNewLocationData();
                                } else {
                                    //latTextView.setText(location.getLatitude()+"");
                                    //lonTextView.setText(location.getLongitude()+"");
                                    latitudine_current=location.getLatitude();
                                    longitudine_current=location.getLongitude();


                                }
                            }
                        }
                );
            } else {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);

            }
        } else {
            requestPermissions();
        }
    }


    //To avoid these rare cases when the location == null, we called a new method requestNewLocationData()
    // which will record the location information in runtime.
    @SuppressLint("MissingPermission")
    private void requestNewLocationData(){

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                Looper.myLooper()
        );

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();

        }
    };

    @Override
    public void onResume(){
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }

    }



}

