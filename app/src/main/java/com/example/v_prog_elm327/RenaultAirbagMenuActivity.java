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

public class RenaultAirbagMenuActivity extends AppCompatActivity {

    TextView tv_ren_srs_spc, tv_ren_srs_rh850;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renault_airbag_menu);

        tv_ren_srs_spc = findViewById(R.id.tv_ren_srs_spc);
        tv_ren_srs_rh850 = findViewById(R.id.tv_ren_srs_rh850);

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
                Intent intent = new Intent(RenaultAirbagMenuActivity.this, UniversalCarRadio.class);
                startActivity(intent);
            }
        });
        /// выбираем SPC Continental
        tv_ren_srs_spc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RenaultAirbagMenuActivity.this, RenaultAirbagContinentalSpcActivity.class);
                startActivity(intent);
            }
        });

    }
}