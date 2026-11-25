package com.basics.sessionpulse;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "sessionpulse_prefs";
    private static final String KEY_DARK = "dark_mode";
    private static final String BASE_CURRENCY = "USD";
    private static final String SYMBOLS = "EUR,GBP,JPY,CHF";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private MarketAdapter adapter;
    private Market london;
    private Market newYork;

    private TextView overlapBanner;
    private TextView ratesText;
    private Button retryButton;

    private RatesApi ratesApi;
    private SharedPreferences prefs;

    // Runs every second while the screen is visible.
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tick();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply the saved dark/light choice before the screen is built.
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean dark = prefs.getBoolean(KEY_DARK, false);
        AppCompatDelegate.setDefaultNightMode(
                dark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        overlapBanner = findViewById(R.id.overlapBanner);
        ratesText = findViewById(R.id.ratesText);
        retryButton = findViewById(R.id.retryButton);
        Button darkButton = findViewById(R.id.darkButton);

        // Markets: name, time zone, local opening and closing time.
        london = new Market("London", ZoneId.of("Europe/London"),
                LocalTime.of(8, 0), LocalTime.of(17, 0));
        newYork = new Market("New York", ZoneId.of("America/New_York"),
                LocalTime.of(8, 0), LocalTime.of(17, 0));

        List<Market> markets = Arrays.asList(
                new Market("Sydney", ZoneId.of("Australia/Sydney"),
                        LocalTime.of(7, 0), LocalTime.of(16, 0)),
                new Market("Tokyo", ZoneId.of("Asia/Tokyo"),
                        LocalTime.of(9, 0), LocalTime.of(18, 0)),
                london,
                newYork
        );

        adapter = new MarketAdapter(markets);
        RecyclerView rv = findViewById(R.id.marketList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // Dark mode button: save the choice and apply it.
        darkButton.setOnClickListener(v -> {
            boolean newValue = !prefs.getBoolean(KEY_DARK, false);
            prefs.edit().putBoolean(KEY_DARK, newValue).apply();
            AppCompatDelegate.setDefaultNightMode(
                    newValue ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Rates: build Retrofit once, then load.
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.frankfurter.dev/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ratesApi = retrofit.create(RatesApi.class);

        retryButton.setOnClickListener(v -> loadRates());
        loadRates();
    }

    /** One tick: update all clocks and the overlap banner. */
    private void tick() {
        Instant now = Instant.now();
        adapter.setNow(now);

        boolean overlap = london.isOpen(now) && newYork.isOpen(now);
        overlapBanner.setVisibility(overlap ? View.VISIBLE : View.GONE);
    }

    /** Downloads the latest rates in the background (Retrofit enqueue). */
    private void loadRates() {
        ratesText.setText("Loading rates...");
        retryButton.setVisibility(View.GONE);

        ratesApi.getLatest(BASE_CURRENCY, SYMBOLS).enqueue(new Callback<RatesResponse>() {
            @Override
            public void onResponse(@NonNull Call<RatesResponse> call,
                                   @NonNull Response<RatesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showRates(response.body());
                } else {
                    showRatesError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<RatesResponse> call, @NonNull Throwable t) {
                showRatesError();   // for example: no internet connection
            }
        });
    }

    private void showRates(RatesResponse data) {
        Map<String, Double> rates = data.getRates();
        if (rates == null || rates.isEmpty()) {
            showRatesError();
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String symbol : SYMBOLS.split(",")) {
            Double value = rates.get(symbol);
            if (value != null) {
                sb.append(String.format(Locale.US, "1 %s = %.4f %s%n",
                        BASE_CURRENCY, value, symbol));
            }
        }
        sb.append("Daily reference rates, date: ").append(data.getDate());
        ratesText.setText(sb.toString());
    }

    private void showRatesError() {
        ratesText.setText("Could not load rates. Check your internet connection.");
        retryButton.setVisibility(View.VISIBLE);
    }

    // Lifecycle: run the clock only while the screen is visible (saves battery).
    @Override
    protected void onStart() {
        super.onStart();
        handler.post(ticker);
    }

    @Override
    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(ticker);
    }
}