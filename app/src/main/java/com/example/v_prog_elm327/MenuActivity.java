package com.example.v_prog_elm327;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MenuActivity extends AppCompatActivity {

    ImageView imageQuestion;
    TextView textObd2,textRenault;

    SharedPreferences sharedPreferences;
    private static final String SHARED_PREV_MODEL = "mypref";
    private static final String KEY_MODEL = "model";



    public static boolean isConnected = false;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        imageQuestion = findViewById(R.id.imageQuestion);
        textObd2 = findViewById(R.id.textObd2);
        textRenault = findViewById(R.id.textRenault);

        imageQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MenuActivity.this,Help_online.class);
                startActivity(intent);
            }
        });

        //////////////////////////////////////////////////////////////////////
        sharedPreferences = getSharedPreferences(SHARED_PREV_MODEL, MODE_PRIVATE);
        String name = sharedPreferences.getString(KEY_MODEL, null);
        if (name != null) {
            Intent intent = new Intent(MenuActivity.this, UniversalActivitySet.class);
            startActivity(intent);
        }


//        /// выбираем ОБД ObdActivity //////////////////////////////////
//        textObd2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Intent intent = new Intent(MenuActivity.this, ObdActivity.class);
//                startActivity(intent);
//            }
//        });
        /// выбираем ОБД BluetoothActivity //////////////////////////////////
        textObd2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MenuActivity.this, BluetoothActivity.class);
                startActivity(intent);
            }
        });

        /// выбираем в меню Renault ////////////////////////////////////////////
        textRenault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putString(KEY_MODEL, textRenault.getText().toString());
//                editor.apply();

                Intent intent = new Intent(MenuActivity.this, UniversalActivitySet.class);
                startActivity(intent);
                Toast.makeText(MenuActivity.this, "Model Renault success", Toast.LENGTH_SHORT).show();
            }
        });




    }


}