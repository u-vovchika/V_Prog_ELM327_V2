package com.example.v_prog_elm327;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;

import java.util.Objects;

public class BluetoothActivity extends AppCompatActivity {

    Dialog dialog;
    ImageView imageBluetooth;
    TextView textStatusBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        imageBluetooth = findViewById(R.id.imageBluetooth);
        textStatusBluetooth = findViewById(R.id.textStatusBluetooth);


        imageBluetooth.setImageResource(R.drawable.outline_bluetooth_24);
        imageBluetooth.setColorFilter(Color.BLUE);


        //вызов диалогового окна в начале игры
        dialog = new Dialog(this);  //создаем новое диалоговое окно
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); //скрываем заголовок
        dialog.setContentView(R.layout.activity_bluetooth);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // прозрачный фон диалогового окна
        dialog.setCancelable(false); // окно нельзя закрыть кликом за пределами диалогового окна

        // устанавливаем картинку диалогового окна ввиде коментария
        ImageView previewImg = dialog.findViewById(R.id.imageBluetooth);
        previewImg.setImageResource(R.drawable.style_btn_dark_red);


        // установка фона диалогового окна
        LinearLayout dialogFon = dialog.findViewById(R.id.bluetooth_fon);
        dialogFon.setBackgroundResource(R.drawable.style_btn_dark_red);
        dialog.show(); //показать диалоговое окно

        textStatusBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BluetoothActivity.this, MenuActivity.class);
                startActivity(intent);
            }
        });


        // Задержка перед переходом на основную активность
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(BluetoothActivity.this, MenuActivity.class);
                startActivity(intent);
                finish(); // Закрываем SplashActivity
            }
        }, 5000); // Задержка в 3 секунды
    }



}