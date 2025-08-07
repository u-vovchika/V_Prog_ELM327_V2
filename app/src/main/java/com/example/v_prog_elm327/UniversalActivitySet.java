package com.example.v_prog_elm327;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UniversalActivitySet extends AppCompatActivity {

    TextView textViewModel;

    SharedPreferences sharedPreferences;
    private static final String SHARED_PREV_MODEL = "mypref";
    private static final String KEY_MODEL = "model";
     private static final String KEY_ECU = "ecu";
    private static final String KEY_SRS = "srs";
    private static final String KEY_RADIO = "CarRadio";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_set);

        // строка состояния убирается
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // возрат назад  ///////////////////////////////////////////////////////
        Button buttonBack = findViewById(R.id.button_back_univ);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear(); // стираем данные textViewModel
                editor.apply(); // записываем данные после очистки textViewModel
                finish();// завершения процесса
                Intent intent = new Intent(UniversalActivitySet.this, MenuActivity.class);
                startActivity(intent);
            }
        });
        /// /////////////////////////////////////////////////////////////////////
        textViewModel = findViewById(R.id.textViewModelEcu);
        /////// Выводим модель ///////////////////////////////////////
        sharedPreferences = getSharedPreferences(SHARED_PREV_MODEL, MODE_PRIVATE);
        String name = sharedPreferences.getString(KEY_MODEL, null);
        if(name != null){
            textViewModel.setText(name);
        }


        /// ///textViewCarRadio///////////////////////////////////////////////////////////
        TextView textViewCarRadio = findViewById(R.id.textViewCarRadio);

        textViewCarRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_RADIO, textViewCarRadio.getText().toString());
                editor.apply();

                Intent intent = new Intent(UniversalActivitySet.this, UniversalCarRadio.class);
                startActivity(intent);
                Toast.makeText(UniversalActivitySet.this, "Model Radio success", Toast.LENGTH_SHORT).show();
            }
        });

    }
}