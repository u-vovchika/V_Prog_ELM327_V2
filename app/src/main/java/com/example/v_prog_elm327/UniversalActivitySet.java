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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_set);

        /// принимаем сообщение от предыдущего меню
        Intent intent = getIntent();
        String message = intent.getStringExtra("model_auto");
        TextView textView = findViewById(R.id.textViewModelEcu);
        textView.setText(message);

        if(textView.getText().toString().equals("Lada")){
            /// LadaAirbagMenuActivity ///////////////////////////////////////////////////////////
            TextView tv_lada_srs_set = findViewById(R.id.tv_srs_set);
            tv_lada_srs_set.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = textView.getText().toString();
                    launcherLadaSrs(message);
                }
            });
            /// textViewCarRadio ///////////////////////////////////////////////////////////
            TextView textViewCarRadio = findViewById(R.id.textViewCarRadio);
            textViewCarRadio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = textView.getText().toString();
                    launcherRenaultRadio(message);
                }
            });
        }

        if(textView.getText().toString().equals("Renault")){
            /// RenaultAirbagMenuActivity ///////////////////////////////////////////////////////////
            TextView tv_ren_srs_set = findViewById(R.id.tv_srs_set);
            tv_ren_srs_set.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = textView.getText().toString();
                    launcherRenaultSrs(message);
                }
            });
            /// textViewCarRadio ///////////////////////////////////////////////////////////
            TextView textViewCarRadio = findViewById(R.id.textViewCarRadio);
            textViewCarRadio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String message = textView.getText().toString();
                    launcherRenaultRadio(message);
                }
            });
        }
//        // строка состояния убирается
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // возрат назад  ///////////////////////////////////////////////////////
        Button buttonBack = findViewById(R.id.button_back_univ);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UniversalActivitySet.this, MenuActivity.class);
                startActivity(intent);
            }
        });
        /// /////////////////////////////////////////////////////////////////////
        textViewModel = findViewById(R.id.textViewModelEcu);
    }

    private void launcherRenaultRadio(String message) {
        Intent intent = new Intent(UniversalActivitySet.this, UniversalCarRadio.class);
        intent.putExtra("model_radio", message);
        startActivity(intent);
        Toast.makeText(UniversalActivitySet.this, "Renault CarRadio", Toast.LENGTH_SHORT).show();
    }


    /// ///////////////////////////////////////////////////////////////////////////////////
    private void launcherRenaultSrs(String message) {
        Intent intent = new Intent(UniversalActivitySet.this, RenaultAirbagMenuActivity.class);
        intent.putExtra("model_srs", message);
        startActivity(intent);
        Toast.makeText(UniversalActivitySet.this, "Renault Airbag Menu", Toast.LENGTH_SHORT).show();
    }

    private void launcherLadaSrs(String message) {
        Intent intent = new Intent(UniversalActivitySet.this, LadaAirbagMenuActivity.class);
        intent.putExtra("model_srs", message);
        startActivity(intent);
        Toast.makeText(UniversalActivitySet.this, "Lada Airbag Menu", Toast.LENGTH_SHORT).show();
    }

}