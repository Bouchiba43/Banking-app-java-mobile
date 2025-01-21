package com.example.banking_ds_app;

import static java.lang.System.exit;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HomepageActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private TextView balanceDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_homepage);

        dbHelper = new DatabaseHelper(this);

        Button logoutButton = findViewById(R.id.logout_button);
        balanceDisplay = findViewById(R.id.balance_display);
        TextView usernameDisplay = findViewById(R.id.username_display);

        String username = getIntent().getStringExtra("username");
        if (username == null) {
            balanceDisplay.setText("Balance not available.");
            usernameDisplay.setText("Welcome!");
            return;
        }

        usernameDisplay.setText("Welcome, " + username + "!");
        displayBalance(username);


        logoutButton.setOnClickListener(v -> {
            exit(0);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homepage_title), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void displayBalance(String username) {
        try {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT balance FROM " + DatabaseHelper.USERS_TABLE + " WHERE username = ?",
                    new String[]{username}
            );

            if (cursor != null && cursor.moveToFirst()) {
                double balance = cursor.getDouble(0);
                balanceDisplay.setText(String.format("Your Balance: $%.2f", balance));
            } else {
                balanceDisplay.setText("Balance not available.");
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            balanceDisplay.setText("Error retrieving balance.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}