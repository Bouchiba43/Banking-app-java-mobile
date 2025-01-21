package com.example.banking_ds_app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TransferActivity extends AppCompatActivity {

    private static final String TAG = "TransferActivity";
    private DatabaseHelper dbHelper;
    private String currentUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        dbHelper = new DatabaseHelper(this);

        // Get UI elements
        EditText receiverUsername = findViewById(R.id.receiver_input);
        EditText amountInput = findViewById(R.id.amount_input);
        Button returnButton = findViewById(R.id.return_button);
        Button transferButton = findViewById(R.id.transfer_button);

        // Verify username is passed
        currentUsername = getIntent().getStringExtra("username");
        if (currentUsername == null || currentUsername.isEmpty()) {
            Log.e(TAG, "No username provided in intent");
            Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Current user: " + currentUsername);


        transferButton.setOnClickListener(v -> handleTransfer(receiverUsername, amountInput));

        // Handle return button click
        returnButton.setOnClickListener(v -> finish());

        // Adjust for system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void handleTransfer(EditText receiverUsername, EditText amountInput) {
        String receiver = receiverUsername.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();

        // Log the attempt
        Log.d(TAG, "Transfer attempt - From: " + currentUsername + " To: " + receiver + " Amount: " + amountStr);

        // Input validation
        if (receiver.isEmpty()) {
            receiverUsername.setError("Please enter receiver username");
            return;
        }
        if (amountStr.isEmpty()) {
            amountInput.setError("Please enter amount");
            return;
        }

        try {
            double transferAmount = Double.parseDouble(amountStr);
            if (transferAmount <= 0) {
                amountInput.setError("Amount must be greater than 0");
                return;
            }

            int currentUserId = getUserId(currentUsername);
            if (currentUserId == -1) {
                Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show();
                return;
            }

            int receiverId = getUserId(receiver);
            if (receiverId == -1) {
                Toast.makeText(this, "Receiver not found", Toast.LENGTH_SHORT).show();
                return;
            }

            if (receiver.equals(currentUsername)) {
                Toast.makeText(this, "Cannot transfer to yourself", Toast.LENGTH_SHORT).show();
                return;
            }

            if (transferFunds(currentUserId, receiverId, transferAmount)) {
                Log.d(TAG, "Transfer successful");
                Toast.makeText(this, "Transfer successful", Toast.LENGTH_SHORT).show();
                receiverUsername.setText("");
                amountInput.setText("");
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Insufficient funds or transfer failed", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            amountInput.setError("Invalid amount format");
        }
    }

    private int getUserId(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT id FROM " + DatabaseHelper.USERS_TABLE + " WHERE username = ?",
                new String[]{username})) {
            if (cursor.moveToFirst()) {
                int id = cursor.getInt(0);
                Log.d(TAG, "Found user ID " + id + " for username: " + username);
                return id;
            }
            Log.e(TAG, "No user found for username: " + username);
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Database error while getting user ID: " + e.getMessage());
            return -1;
        }
    }

    private boolean transferFunds(int senderId, int receiverId, double transferAmount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            Log.d(TAG, "Starting transfer of " + transferAmount + " from user " + senderId + " to user " + receiverId);

            // Check sender's balance
            try (Cursor cursor = db.rawQuery("SELECT balance FROM " + DatabaseHelper.USERS_TABLE + " WHERE id = ?",
                    new String[]{String.valueOf(senderId)})) {
                if (cursor.moveToFirst()) {
                    double currentBalance = cursor.getDouble(0);
                    if (currentBalance < transferAmount) {
                        Log.e(TAG, "Insufficient funds. Required: " + transferAmount + ", Available: " + currentBalance);
                        return false;
                    }
                } else {
                    Log.e(TAG, "Sender not found in database");
                    return false;
                }
            }

            // Deduct from sender and add to receiver
            db.execSQL("UPDATE " + DatabaseHelper.USERS_TABLE + " SET balance = balance - ? WHERE id = ?",
                    new Object[]{transferAmount, senderId});
            db.execSQL("UPDATE " + DatabaseHelper.USERS_TABLE + " SET balance = balance + ? WHERE id = ?",
                    new Object[]{transferAmount, receiverId});


            db.setTransactionSuccessful();
            Log.d(TAG, "Transfer completed successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Database error during transfer: " + e.getMessage());
            return false;
        } finally {
            db.endTransaction();
        }
    }
}
