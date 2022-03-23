package com.karypnino.RegresoCasaMapa;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    SupportMapFragment mapFragment;
    GoogleMap map;
    LatLng house,me;
    String url;
    JsonObjectRequest jsonObjectRequest;
    RequestQueue requestQueue;
    Button btnIr;
    List<List<HashMap<String, String>>> rutas;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mapFragment = SupportMapFragment.newInstance();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.map, mapFragment)
                .commit();
        mapFragment.getMapAsync(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);
        rutas = new ArrayList<List<HashMap<String,String>>>() ;

        btnIr = findViewById(R.id.btnIr);
        btnIr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                url= "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin="+me.latitude+","+me.longitude+"&"+"destination="+house.latitude+","+house.longitude+
                        "&key="+"AIzaSyBHtYD_i3eqYqdCroUTQDwzb5FtqD323oc";

                jsonObjectRequest = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(getApplicationContext(),"Hay te va"+house.latitude,Toast.LENGTH_LONG).show();
                        JSONArray jRoutes = null;
                        JSONArray jLegs = null;
                        JSONArray jSteps = null;

                        try {
                            jRoutes = response.getJSONArray("routes");
                            for(int i=0;i<jRoutes.length();i++){
                                jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                                List path = new ArrayList<HashMap<String, String>>();

                                for(int j=0;j<jLegs.length();j++){
                                    jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");

                                    for(int k=0;k<jSteps.length();k++){
                                        String polyline = "";
                                        polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                                        List<LatLng> list = decodePoly(polyline);

                                        for(int l=0;l<list.size();l++){
                                            HashMap<String, String> hm = new HashMap<String, String>();
                                            hm.put("lat", Double.toString(((LatLng)list.get(l)).latitude) );
                                            hm.put("lng", Double.toString(((LatLng)list.get(l)).longitude) );
                                            path.add(hm);
                                        }
                                    }
                                    rutas.add(path);
                                }
                            }

                            ArrayList<LatLng> points = null;
                            PolylineOptions lineOptions = null;

                            for(int i=0;i<rutas.size();i++){
                                points = new ArrayList<LatLng>();
                                lineOptions = new PolylineOptions();

                                List<HashMap<String, String>> path = rutas.get(i);

                                for(int j=0;j<path.size();j++){
                                    HashMap<String,String> point = path.get(j);

                                    double lat = Double.parseDouble(point.get("lat"));
                                    double lng = Double.parseDouble(point.get("lng"));
                                    LatLng position = new LatLng(lat, lng);

                                    points.add(position);
                                }

                                lineOptions.addAll(points);
                                lineOptions.width(4);
                                lineOptions.color(Color.RED);
                            }
                            if(lineOptions!=null) {
                                map.addPolyline(lineOptions);
                            }
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(),"Error1",Toast.LENGTH_LONG).show();
                        }catch (Exception e){
                            Toast.makeText(getApplicationContext(),"Error2",Toast.LENGTH_LONG).show();
                        }
                    }
                },error -> {
                    Toast.makeText(getApplicationContext(),"Error3",Toast.LENGTH_LONG).show();
                });
                requestQueue.add(jsonObjectRequest);
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        house = new LatLng(20.21021, -101.12659);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
      fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if (location != null) {
                            me = new LatLng(location.getLatitude(),location.getLongitude());
                            map.addMarker(new MarkerOptions()
                                    .position(me)
                                    .title("You"));
                            CameraPosition cameraPosition = CameraPosition.builder().target(me)
                                    .zoom(15)
                                    .tilt(67)
                                    .bearing(90).build();
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        }
                    }
                });

        map.addMarker(new MarkerOptions()
                .position(house)
                .title("Your House"));
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}
