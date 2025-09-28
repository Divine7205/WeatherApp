package com.practice.weatherapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    String CITY = "london,uk";
    String API = "14e719695ce420f98a14dea8163654ab";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
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
        fetchWeather();

    }

    private void fetchWeather() {
        if (loader != null){
            loader.setVisibility(View.VISIBLE);
            mainContainer.setVisibility(View.GONE);
            error.setVisibility(View.GONE);
        }
        executorService.execute(() -> {
            String result =null;
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            try {
                URL url = new URL("https://api.openweathermap.org/data/2.5/weather?q=" + CITY + "&units=metric&appid=" + API);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET"); // Optional, GET is default
                // urlConnection.setReadTimeout(10000 /* milliseconds */);
                // urlConnection.setConnectTimeout(15000 /* milliseconds */);
                connection.connect();

                inputStream = connection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                if (stringBuilder.length() > 0) {
                    result = stringBuilder.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Handle exceptions
                result= "Error fetching weather data";
            } finally {
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
            // Post the result to the main thread
            final String finalResult = result;
            handler.post(() -> {
                if (!Objects.equals(finalResult, "Error fetching weather data")){
                    loader.setVisibility(View.GONE);

                    try {
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

                        // Temperature
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

                        mainContainer.setVisibility(View.VISIBLE);
                    } catch (Exception e){
                        e.printStackTrace();
                        error.setVisibility(View.VISIBLE);
                    }
                } else {
                    loader.setVisibility(View.GONE);
                    error.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown())
            executorService.shutdown();
    }
}