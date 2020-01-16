package com.example.airdataapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.airdataapp.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    DBHelper mydb_maps;
    private String userEmail;
    private FirebaseAuth auth;
    ArrayList<Double> lat_arraylist = new ArrayList<Double>();
    ArrayList<Double> lng_arraylist = new ArrayList<Double>();
    ArrayList<Double> pm10_arraylist = new ArrayList<Double>();
    Double[] array_lat = new Double[lat_arraylist.size()];
    Double[] array_lng = new Double[lng_arraylist.size()];
    Double[] array_pm10 = new Double[pm10_arraylist.size()];
    LatLng latlng;
    double lat;
    double lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mydb_maps = new DBHelper(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        Button misurazioneButton = (Button)findViewById(R.id.misurazione);
        auth = FirebaseAuth.getInstance();
        userEmail = auth.getCurrentUser().getEmail();

        System.out.println("USER: "+ userEmail );

        //Inserimento valori
        Cursor rilevazioni = mydb_maps.getDataForCurrentUser(userEmail);

        int i = 1;
        while(rilevazioni.moveToNext())
        {

            String pm1_round;
            String pm25_round;
            String pm10_round;

            lat= rilevazioni.getDouble(7);
            lng = rilevazioni.getDouble(8);

            //lat= i;
            //lng = i;


            System.out.println("I: "+i);
            //double lat= i;
            //double lng = i+1;

            System.out.println("LATITUDINE: ---"+ lat);
            System.out.println("LONGITUDINE: ---"+ rilevazioni.getString(8));
            lat_arraylist.add(lat);
            lng_arraylist.add(lng);

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
                pm10_arraylist.add(rilevazioni.getDouble(4));
            }
            else
            {
                pm10_round = rilevazioni.getString(4);
                pm10_arraylist.add(rilevazioni.getDouble(4));
            }

            i++;
        }




        misurazioneButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {

                Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);


            }
        });



    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        int k;

        for (k=0;k<lat_arraylist.size();k++){


            array_lat = lat_arraylist.toArray(array_lat);
            array_lng = lng_arraylist.toArray(array_lng);
            array_pm10 = pm10_arraylist.toArray(array_pm10);

            System.out.println("LA#######################T: "+ array_lat[k]);
            System.out.println("LA#######################G: "+ array_lng[k]);


            if(array_pm10[k]>40){
                latlng = new LatLng(array_lat[k], array_lng[k]);
                mMap.addMarker(new MarkerOptions().position(latlng).title("Qualità aria: Pessima!"));
            }
            if(array_pm10[k]<=15){
                latlng = new LatLng(array_lat[k], array_lng[k]);
                mMap.addMarker(new MarkerOptions().position(latlng).title("Qualità aria: Ottima!"));
            }
            if(array_pm10[k]>15 && array_pm10[k]<=40 ){
                latlng = new LatLng(array_lat[k], array_lng[k]);
                mMap.addMarker(new MarkerOptions().position(latlng).title("Qualità aria: Buona!"));
            }


            CameraPosition cameraPosition = new CameraPosition.Builder().target(latlng).zoom(10).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

    }




    protected Marker createMarker(double latitude, double longitude) {

        return mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .anchor(0.5f, 0.5f));
    }

}