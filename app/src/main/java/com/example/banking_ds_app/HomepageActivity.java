package com.example.banking_ds_app;

import android.content.Intent;
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

    private static final int TRANSFER_REQUEST = 1;
    private DatabaseHelper dbHelper;
    private TextView balanceDisplay;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_homepage);

        dbHelper = new DatabaseHelper(this);

        Button logoutButton = findViewById(R.id.return_button);
        Button depositButton = findViewById(R.id.deposit_button);
        Button withdrawButton = findViewById(R.id.withdraw_button);
        Button transferButton = findViewById(R.id.transfer_button);
        balanceDisplay = findViewById(R.id.balance_display);
        TextView usernameDisplay = findViewById(R.id.username_display);

        username = getIntent().getStringExtra("username");
        if (username == null) {
            balanceDisplay.setText("Balance not available.");
            usernameDisplay.setText("Welcome!");
            return;
        }

        usernameDisplay.setText("Welcome, " + username + "!");
        displayBalance();

        depositButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomepageActivity.this, DepositActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        withdrawButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomepageActivity.this, WithdrawActivity.class);
            intent.putExtra("username", username);
            startActivity(intent);
        });

        transferButton.setOnClickListener(v -> startTransfer());

        logoutButton.setOnClickListener(v -> {
            finish(); // Better than System.exit(0)
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.homepage_title), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void startTransfer() {
        Intent intent = new Intent(this, TransferActivity.class);
        intent.putExtra("username", username);
        startActivityForResult(intent, TRANSFER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TRANSFER_REQUEST && resultCode == RESULT_OK) {
            displayBalance(); // Refresh the balance after successful transfer
        }
    }

    private void displayBalance() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery(
                "SELECT balance FROM " + DatabaseHelper.USERS_TABLE + " WHERE username = ?",
                new String[]{username})) {
            if (cursor != null && cursor.moveToFirst()) {
                double balance = cursor.getDouble(0);
                balanceDisplay.setText(String.format("Your Balance: $%.2f", balance));
            } else {
                balanceDisplay.setText("Balance not available.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            balanceDisplay.setText("Error retrieving balance.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        displayBalance(); // Refresh balance when returning to the activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}