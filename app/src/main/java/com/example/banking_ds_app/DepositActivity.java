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

public class DepositActivity extends AppCompatActivity {

    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deposit);

        dbHelper = new DatabaseHelper(this);

        String currentUsername = getIntent().getStringExtra("username");

        Button depositButton = findViewById(R.id.withdraw_button);
        EditText depositValue = findViewById(R.id.deposit_value);
        Button returnButton = findViewById(R.id.return_button);

        depositButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String depositAmount1 = depositValue.getText().toString();

                double depositAmount = Double.parseDouble(depositAmount1);

                int userId = getUserId(currentUsername);
                if (userId != -1 && depositFunds(userId, depositAmount)) {
                    Toast.makeText(DepositActivity.this, "Deposit successful!", Toast.LENGTH_SHORT).show();
                    depositValue.setText("");
                } else {
                    Toast.makeText(DepositActivity.this, "Deposit failed.", Toast.LENGTH_SHORT).show();
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

    private boolean depositFunds(int userId, double depositAmount) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("UPDATE " + DatabaseHelper.USERS_TABLE + " SET balance = balance + ? WHERE id = ?",
                new Object[]{depositAmount, userId});

        db.execSQL("INSERT INTO " + DatabaseHelper.TRANSACTIONS_TABLE + " (user_id, type, amount) VALUES (?, ?, ?)",
                new Object[]{userId, "Deposit", depositAmount});

        return true;
    }
}