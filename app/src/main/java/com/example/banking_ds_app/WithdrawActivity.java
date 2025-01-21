package com.example.banking_ds_app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class WithdrawActivity extends AppCompatActivity {

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_withdraw);

        dbHelper = new DatabaseHelper(this);

        String currentUsername = getIntent().getStringExtra("username");
        EditText withdrawAmountInput = findViewById(R.id.withdraw_value);
        Button withdrawButton = findViewById(R.id.withdraw_button);
        Button returnButton = findViewById(R.id.return_button);

        withdrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amountStr = withdrawAmountInput.getText().toString();

                if (amountStr.isEmpty()) {
                    Toast.makeText(WithdrawActivity.this, "Please enter a valid amount.", Toast.LENGTH_SHORT).show();
                    return;
                }

                double withdrawAmount = Double.parseDouble(amountStr);

                int userId = getUserId(currentUsername);
                if (userId != -1) {
                    if (withdrawFunds(userId, withdrawAmount)) {
                        Toast.makeText(WithdrawActivity.this, "Withdrawal successful!", Toast.LENGTH_SHORT).show();
                        withdrawAmountInput.setText("");
                    } else {
                        Toast.makeText(WithdrawActivity.this, "Insufficient balance.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(WithdrawActivity.this, "Withdrawal failed.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        returnButton.setOnClickListener(v -> {
            finish();
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private int getUserId(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM " + DatabaseHelper.USERS_TABLE + " WHERE username = ?",
                new String[]{username});

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(0);
            cursor.close();
            return userId;
        }

        cursor.close();
        return -1;
    }

    private boolean withdrawFunds(int userId, double withdrawAmount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Vérifier si le solde est suffisant
        Cursor cursor = db.rawQuery("SELECT balance FROM " + DatabaseHelper.USERS_TABLE + " WHERE id = ?",
                new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            double currentBalance = cursor.getDouble(0);
            cursor.close();

            if (currentBalance >= withdrawAmount) {
                db.execSQL("UPDATE " + DatabaseHelper.USERS_TABLE + " SET balance = balance - ? WHERE id = ?",
                        new Object[]{withdrawAmount, userId});

                db.execSQL("INSERT INTO " + DatabaseHelper.TRANSACTIONS_TABLE + " (user_id, type, amount) VALUES (?, ?, ?)",
                        new Object[]{userId, "Withdraw", withdrawAmount});

                return true;
            }
        }

        if (cursor != null) {
            cursor.close();
        }
        return false;
    }
}