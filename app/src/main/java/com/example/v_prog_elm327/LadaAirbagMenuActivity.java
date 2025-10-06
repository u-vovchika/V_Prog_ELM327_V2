package com.example.v_prog_elm327;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LadaAirbagMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lada_airbag_menu);

        /// принимаем сообщение от главного меню
        Intent intent = getIntent();
        String message = intent.getStringExtra("model_srs");
        TextView textView = findViewById(R.id.textViewModelEcu);
        textView.setText( message + " SRS");

        TextView textView1 = findViewById(R.id.tv_lada_srs_spc);
        textView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = textView.getText().toString();
                launcherTakataSpc(message);
            }
        });

    }

    private void launcherTakataSpc(String message) {
        Intent intent1 = new Intent(LadaAirbagMenuActivity.this, LadaAirbagTakataSPCActivity.class);
        intent1.putExtra("srs" , message);
        startActivity(intent1);
    }
}