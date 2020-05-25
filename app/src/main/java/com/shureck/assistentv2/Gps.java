package com.shureck.assistentv2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Gps extends AppCompatActivity
        implements GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener,
        OnMapReadyCallback {

    private LatLng mOrigin;
    private LatLng mDestination;
    private Polyline mPolyline;
    ArrayList<LatLng> mMarkerPoints;

    private GoogleMap mMap;
    public TextView textView, textView2, textView3;
    public EditText editText;
    public Button find_bt;
    public Sensor acc, mag;
    private SensorManager sensorManager;
    public static final int REQUEST_ID_ACCESS_COURSE_FINE_LOCATION = 100;

    public float[] gravityData = new float[3];
    public float[] geomagneticData = new float[3];
    public double rotationInDegrees;
    public boolean hasGravityData, hasGeomagneticData;
    public static Geocoder geocoder;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps);
        im_init();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);
        textView3 = findViewById(R.id.textView3);
        editText = findViewById(R.id.editText2);
        find_bt = findViewById(R.id.button4);
        SensorEventListener listener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {

                switch (event.sensor.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                        System.arraycopy(event.values, 0, gravityData, 0, 3);
                        hasGravityData = true;
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        System.arraycopy(event.values, 0, geomagneticData, 0, 3);
                        hasGeomagneticData = true;
                        break;
                    default:
                        return;
                }

                if (hasGravityData && hasGeomagneticData) {
                    float identityMatrix[] = new float[9];
                    float rotationMatrix[] = new float[9];
                    boolean success = SensorManager.getRotationMatrix(rotationMatrix, identityMatrix, gravityData, geomagneticData);

                    if (success) {
                        float orientationMatrix[] = new float[3];
                        SensorManager.getOrientation(rotationMatrix, orientationMatrix);
                        float rotationInRadians = orientationMatrix[0];
                        rotationInDegrees = ((Math.toDegrees(rotationInRadians)+360)%360);
                        textView.setText(String.valueOf(rotationInDegrees)+" deg");
                        //System.out.println("&&&&&&&&&&&&&&&&&&&& " + rotationInDegrees);
                        // do something with the rotation in degrees
                    }
                    //System.out.println("&&&&&&&&&&&&&&&&&&&&" + rotationInDegrees);
                }
            }


            public void onAccuracyChanged(Sensor event, int accuracy) {
                System.out.println("fffffffff");
            }
        };

        sensorManager.registerListener(listener, acc, Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(listener, mag, Sensor.TYPE_MAGNETIC_FIELD);

        find_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = editText.getText().toString();
                try {
                    List<Address> address = geocoder.getFromLocationName(s, 3);
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }
        });

        mMarkerPoints = new ArrayList<>();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        geocoder = new Geocoder(this);
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        // TODO: Before enabling the My Location layer, you must request
        // location permission from the user. This sample does not include
        // a request for location permission.
        if (ContextCompat.checkSelfPermission( this,  Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)  {
            mMap.setMyLocationEnabled(true);
        }  else {
            System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        }
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng point) {
                // Already two locations
                if(mMarkerPoints.size()>1){
                    mMarkerPoints.clear();
                    mMap.clear();
                }

                // Adding new item to the ArrayList
                mMarkerPoints.add(point);

                // Creating MarkerOptions
                MarkerOptions options = new MarkerOptions();

                // Setting the position of the marker
                options.position(point);

                /**
                 * For the start location, the color of marker is GREEN and
                 * for the end location, the color of marker is RED.
                 */
                if(mMarkerPoints.size()==1){
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                }else if(mMarkerPoints.size()==2){
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                }

                // Add new marker to the Google Map Android API V2
                mMap.addMarker(options);

                // Checks, whether start and end locations are captured
                if(mMarkerPoints.size() >= 2){
                    mOrigin = mMarkerPoints.get(0);
                    mDestination = mMarkerPoints.get(1);
                    drawRoute();
                }

            }
        });
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        try {
            //System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$ "+geocoder.getFromLocationName("premise",1,mMap.getMyLocation().getLatitude(),mMap.getMyLocation().getLongitude(),mMap.getMyLocation().getLatitude(),mMap.getMyLocation().getLongitude()));
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$ "+location);

            //mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude()-0.005, location.getLongitude()-0.005)).title("test"));
            //mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude()+0.0005f, location.getLongitude()+0.0005f)).title("test"));

            double t_1 = mMap.getMyLocation().getLatitude();// - 0.0005;
            double t_2 = mMap.getMyLocation().getLongitude();// - 0.0005;

            mMap.addMarker(new MarkerOptions().position(new LatLng(t_1, t_2)).title("test"));

            List<Address> address = geocoder.getFromLocation(t_1,t_2,5);
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$ "+ address);

            double[] res = get_home(address, location.getLatitude(), location.getLongitude(), rotationInDegrees);
            //double res[] = calcDist(location.getLatitude(), location.getLongitude(), address.getLatitude(), address.getLongitude());

            //Toast.makeText(this, address.getAddressLine(0) + "\n"+ res[0] + "m\n" + res[1] + "deg\n", Toast.LENGTH_LONG).show();

            if((int)res[2] == -1){
                Toast.makeText(this, "Can't find home", Toast.LENGTH_LONG).show();
                //mMap.clear();
            }
            else {
                Toast.makeText(this, "Successfully", Toast.LENGTH_LONG).show();
                textView2.setText("Current location:\n" + location.getLatitude() + " " + location.getLongitude());
                textView3.setText(address.get((int) res[2]).getAddressLine(0) + "\n\n" + res[0] + " m\n" + res[1] + " deg\n");
                //mMap.clear();
                mMap.addMarker(new MarkerOptions().position(new LatLng(address.get((int) res[2]).getLatitude(), address.get((int) res[2]).getLongitude())).title("test"));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                // Permission was denied. Display an error message.
            }
        }
    }

    private void drawRoute(){

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(mOrigin, mDestination);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Key
        String key = "key=" + getString(R.string.google_maps_key);

        // Building the parameters to the web service
        String parameters = str_origin+"&amp;"+str_dest+"&amp;"+key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb  = new StringBuffer();

            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("Exception on download", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /** A class to download data from Google Directions URL */
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("DownloadTask","DownloadTask : " + data);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Directions in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(8);
                lineOptions.color(Color.RED);
            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                if(mPolyline != null){
                    mPolyline.remove();
                }
                mPolyline = mMap.addPolyline(lineOptions);

            }else
                Toast.makeText(getApplicationContext(),"No route is found", Toast.LENGTH_LONG).show();
        }
    }

    public double[] calcDist(double llat1, double llong1, double llat2, double llong2){
        //Math.PI;
        int rad = 6372795;
        double lat1 = llat1*Math.PI/180;
        double lat2 = llat2*Math.PI/180;
        double long1 = llong1*Math.PI/180;
        double long2 = llong2*Math.PI/180;

        double cl1 = Math.cos(lat1);
        double cl2 = Math.cos(lat2);
        double sl1 = Math.sin(lat1);
        double sl2 = Math.sin(lat2);
        double delta = long2 - long1;
        double cdelta = Math.cos(delta);
        double sdelta = Math.sin(delta);

        double y = Math.sqrt(Math.pow(cl2*sdelta,2)+Math.pow(cl1*sl2-sl1*cl2*cdelta,2));
        double x = sl1*sl2+cl1*cl2*cdelta;
        double ad = Math.atan2(y,x);
        double dist = ad*rad;

        x = (cl1*sl2) - (sl1*cl2*cdelta);
        y = sdelta*cl2;
        double z = Math.toDegrees(Math.atan(-y/x));

        if (x < 0){z = z+180;}

        double z2 = (z+180.) % 360. - 180;
        z2 -= Math.toRadians(z2);
        double anglerad2 = z2 - ((2*Math.PI)*Math.floor((z2/(2*Math.PI))));
        double angledeg = (anglerad2*180.)/Math.PI;

        System.out.println("!!!!!!!!!!!!!!! "+ dist + " m " + angledeg + " dig");

        double mass[] = {dist,angledeg,-1};
        return mass;
    }

    public double[] calcDist_radius(double llat1, double llong1, double llat2, double llong2){
        //Math.PI;
        int rad = 6372795;
        double lat1 = llat1*Math.PI/180;
        double lat2 = llat2*Math.PI/180;
        double long1 = llong1*Math.PI/180;
        double long2 = llong2*Math.PI/180;

        double cl1 = Math.cos(lat1);
        double cl2 = Math.cos(lat2);
        double sl1 = Math.sin(lat1);
        double sl2 = Math.sin(lat2);
        double delta = long2 - long1;
        double cdelta = Math.cos(delta);
        double sdelta = Math.sin(delta);

        double y = Math.sqrt(Math.pow(cl2*sdelta,2)+Math.pow(cl1*sl2-sl1*cl2*cdelta,2));
        double x = sl1*sl2+cl1*cl2*cdelta;
        double ad = Math.atan2(y,x);
        double dist = ad*rad;

        x = (cl1*sl2) - (sl1*cl2*cdelta);
        y = sdelta*cl2;
        double z = Math.toDegrees(Math.atan(-y/x));

        if (x < 0){z = z+180;}

        double z2 = (z+180.) % 360. - 180;
        z2 -= Math.toRadians(z2);
        double anglerad2 = z2 - ((2*Math.PI)*Math.floor((z2/(2*Math.PI))));
        double angledeg = (anglerad2*180.)/Math.PI;

        System.out.println("!!!!!!!!!!!!!!! "+ dist + " m " + angledeg + " dig");

        double mass[] = {dist,angledeg,-1};
        return mass;
    }

    public void shell_sort(ArrayList<double[]> array, int size) {
        int step, i, j;
        double[] tmp;

        for (step = size / 2; step > 0; step /= 2) {
            for (i = step; i < size; i++) {
                for (j = i - step; j >= 0 && array.get(j)[0] > array.get(j + step)[0]; j -= step)
                {
                    tmp = array.get(j);
                    array.set(j,array.get(j + step));
                    array.set(j + step,tmp);
                }
            }
        }
    }

    public double[] get_home(List<Address> addresses, double llat, double llong, double degree){
        double min_dist = 10000;
        int numb_min = -1;
        ArrayList<double[]> data_mass = new ArrayList<double[]>();
        for(int i=0;i<addresses.size();i++){
            data_mass.add(calcDist(llat, llong, addresses.get(i).getLatitude(), addresses.get(i).getLongitude()));
            data_mass.get(i)[2] = i;
        }

        shell_sort(data_mass, data_mass.size());

        for(int i=0;i<data_mass.size();i++){
            if ((data_mass.get(i)[1]>=degree-30)&&(data_mass.get(i)[1]<=degree+30)){
                return data_mass.get(i);
            }
        }
        return new double[]{0, 0, -1};
    }

    private void im_init(){
        ImageView im_settings = (ImageView) findViewById(R.id.imageView4);
        im_settings.setClickable(true);
        im_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Gps.this, Settings.class);
                startActivity(intent);
            }
        });

        ImageView im_nfc = (ImageView) findViewById(R.id.imageView3);
        im_nfc.setClickable(true);
        im_nfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Gps.this, Nfc.class);
                startActivity(intent);
            }
        });

        ImageView im_range = (ImageView) findViewById(R.id.imageView5);
        im_range.setClickable(true);
        im_range.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Gps.this, Rangefinder.class);
                startActivity(intent);
            }
        });

        ImageView im_main = (ImageView) findViewById(R.id.imageView);
        im_main.setClickable(true);
        im_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Gps.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }
}
