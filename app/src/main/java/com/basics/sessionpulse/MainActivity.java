package com.basics.sessionpulse;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Market london;
    private TextView nameText, timeText, statusText;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nameText = findViewById(R.id.nameText);
        timeText = findViewById(R.id.timeText);
        statusText = findViewById(R.id.statusText);

        london = new Market("London", ZoneId.of("Europe/London"),
                LocalTime.of(8, 0), LocalTime.of(17, 0));
        nameText.setText(london.getName());
    }

    private void updateClock() {
        Instant now = Instant.now();
        timeText.setText(now.atZone(london.getZone()).format(fmt));

        boolean open = london.isOpen(now);
        statusText.setText(open ? "OPEN" : "CLOSED");
        statusText.setTextColor(Color.parseColor(open ? "#27500A" : "#5F5E5A"));
        statusText.setBackgroundColor(Color.parseColor(open ? "#EAF3DE" : "#F1EFE8"));
    }

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