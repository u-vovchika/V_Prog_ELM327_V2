package com.example.v_prog_elm327;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UniversalCarRadio extends AppCompatActivity {

    private static final String SHARED_PREV_MODEL = "mypref";
    private static final String KEY_RADIO = "CarRadio Renault";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_car_radio);

        // строка состояния убирается
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // возрат назад  ///////////////////////////////////////////////////////
        Button buttonBack = findViewById(R.id.button_back_univ);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.clear(); // стираем данные textViewModel
//                editor.apply(); // записываем данные после очистки textViewModel
//                finish();// завершения процесса
                Intent intent = new Intent(UniversalCarRadio.this, MenuActivity.class);
                startActivity(intent);
            }
        });
        /// /////////////////////////////////////////////////////////////////////
        TextView tv_car_radio = findViewById(R.id.tv_car_radio);
        /////// Выводим модель ///////////////////////////////////////
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREV_MODEL, MODE_PRIVATE);
        String name = sharedPreferences.getString(KEY_RADIO, null);
        if(name != null){
            tv_car_radio.setText(name);
        }
        /// Выбор подушек на рено ////////////////////////////////////////////////////
        /// ////  выбираем Renault_final /////////////////////////////////////////////////////////
        TextView textRenaultXC = findViewById(R.id.textRenaultXC);
        textRenaultXC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_RADIO, textRenaultXC.getText().toString());
                editor.apply();
                Intent intent = new Intent(UniversalCarRadio.this, RenaultCarradioFinal.class);
                startActivity(intent);
            }
        });


    }
}