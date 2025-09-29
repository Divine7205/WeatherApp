package com.practice.weatherapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainActivity extends AppCompatActivity {
    private String API = "14e719695ce420f98a14dea8163654ab";

    private double latitude = 51.5085;
    private double longitude = -0.1257;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FusedLocationProviderClient locationProvider ;

    // UI variables
    private ProgressBar loader;
    private RelativeLayout mainContainer;
    private TextView error;
    private TextView address;
    private TextView updated_at;
    private TextView status;
    private TextView temp;
    private TextView temp_min;
    private TextView temp_max;
    private TextView sunrise;
    private TextView sunset;
    private TextView wind;
    private TextView pressure;
    private TextView humidity;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI variables

        loader = findViewById(R.id.loader);
        mainContainer = findViewById(R.id.mainContainer);
        error = findViewById(R.id.error);
        address = findViewById(R.id.address);
        updated_at = findViewById(R.id.update_at);
        status = findViewById(R.id.status);
        temp = findViewById(R.id.temp);
        temp_min = findViewById(R.id.min_temp);
        temp_max = findViewById(R.id.max_temp);
        sunrise = findViewById(R.id.sunrise);
        sunset = findViewById(R.id.sunset);
        wind = findViewById(R.id.wind);
        pressure = findViewById(R.id.pressure);
        humidity = findViewById(R.id.humidity);
        ImageButton refresh = findViewById(R.id.refresh);
        locationProvider = LocationServices.getFusedLocationProviderClient(this);
        // Call functions to fetch weather
        initiateWeatherFetch();
        refresh.setOnClickListener(v -> initiateWeatherFetch());

    }

    private void initiateWeatherFetch() {
        // Initiate Processes
        progressing();
        getCurrentLocation();
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation(){
        if (checkPermissions()){
            // Permission is granted
            if (isLocationEnabled()){
                // Location is enabled
                locationProvider.getLastLocation().addOnCompleteListener(task -> {
                    // Get the location
                    Location location = task.getResult();
                    if (location != null){
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        // Fetch the weather
                        fetchWeather();
                    }
                }).addOnFailureListener(e -> {
                    // Handle failure
                    Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
                    fetchWeather();
                });

            }
            else {
                // Location is not enabled
                Toast.makeText(this, "Please turn on your location", Toast.LENGTH_LONG).show();
                fetchWeather();
            }

        }
        else {
            requestPermission();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_ACCESS_CODE);
    }

    private final int PERMISSION_REQUEST_ACCESS_CODE = 100;

    private Boolean checkPermissions(){
        // Check if permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    private Boolean isLocationEnabled(){
        // Check if location is enabled
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
        if (requestCode == PERMISSION_REQUEST_ACCESS_CODE){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                // Permission granted
                initiateWeatherFetch();
            }else{
                // Permission denied
                Toast.makeText(this, "Location permission denied. Showing weather for default location.", Toast.LENGTH_LONG).show();
                fetchWeather();
            }
        }
    }

    // Screens states
    private void progressing(){
        loader.setVisibility(View.VISIBLE);
        mainContainer.setVisibility(View.GONE);
        error.setVisibility(View.GONE);
    }
    private void showError(){
        loader.setVisibility(View.GONE);
        mainContainer.setVisibility(View.GONE);
        error.setVisibility(View.VISIBLE);
    }
    private void loaded(){
        loader.setVisibility(View.GONE);
        mainContainer.setVisibility(View.VISIBLE);
        error.setVisibility(View.GONE);
    }

    // Fetching data from the API
    private String getData(){
        //holds the result
        String result =null;
        // HTTP connection
        HttpURLConnection connection = null;
        // TO read the data
        InputStream inputStream = null;
        try {
            // URL to fetch the data
            URL url = new URL("https://api.openweathermap.org/data/2.5/weather?lat="+ latitude +"&lon=" + longitude + "&units=metric&appid=" + API);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            //Reading the data from the stream
            inputStream = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            //Reading the data line by line
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            if (stringBuilder.length() > 0) {
                result = stringBuilder.toString();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            // Handle exceptions
            result= "Error";
        }
        finally {
            // Close the streams and connection
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        System.out.println(result);
        return result;
    }
    private void fetchWeather() {
        // Run the fetch in a separate thread
        executorService.execute(() -> {
            // Post the result to the main thread
            final String finalResult = getData();
            handler.post(() -> {
                if (!Objects.equals(finalResult, "Error")){
                    try {
                        // Parsing the data
                        JSONObject jsonObject = new JSONObject(finalResult);
                        JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
                        JSONObject main = jsonObject.getJSONObject("main");
                        JSONObject sys = jsonObject.getJSONObject("sys");
                        JSONObject winds = jsonObject.getJSONObject("wind");
                        long updatedAt = jsonObject.getLong("dt");
                        //Assigning values to variables
                        // Date and time
                        String updatedAtText = "Updated at: " + new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.ENGLISH).format(new java.util.Date(updatedAt * 1000));
                        long sunriseNo = sys.getLong("sunrise");
                        long sunsetNo = sys.getLong("sunset");
                        String sunriseText = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH).format(new java.util.Date(sunriseNo * 1000));
                        String sunsetText = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ENGLISH).format(new java.util.Date(sunsetNo * 1000));

                        // Temperatures
                        String temperature = main.getString("temp") + "°C";
                        String tempMin = "Min Temp: " + main.getString("temp_min") + "°C";
                        String tempMax = "Max Temp: " + main.getString("temp_max") + "°C";

                        // Other values
                        String pressureText = main.getString("pressure");
                        String humidityText = main.getString("humidity");
                        String windSpeed = winds.getString("speed");
                        String weatherDescription = weather.getString("description");
                        String addressText = jsonObject.getString("name") + ", " + sys.getString("country");

                        // Displaying values
                        updated_at.setText(updatedAtText);
                        status.setText(weatherDescription.toUpperCase());
                        temp.setText(temperature);
                        temp_min.setText(tempMin);
                        temp_max.setText(tempMax);
                        sunrise.setText(sunriseText);
                        sunset.setText(sunsetText);
                        wind.setText(windSpeed);
                        pressure.setText(pressureText);
                        humidity.setText(humidityText);
                        address.setText(addressText);
                        loaded();
                    }
                    catch (Exception e){
                        e.printStackTrace();
                        showError();
                    }
                }
                else {
                    showError();
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the executor service
        if (executorService != null && !executorService.isShutdown())
            executorService.shutdown();
    }
}