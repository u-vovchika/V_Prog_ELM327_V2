package com.example.v_prog_elm327;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       ///  проявление картинки //////////////
        ImageView main_background = findViewById(R.id.main_background);
        Animation out = AnimationUtils.loadAnimation(MainActivity.this, R.anim.anim_alpha);
        main_background.startAnimation(out);

        // Задержка перед переходом на основную активность
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(MainActivity.this, MenuActivity.class);
                startActivity(intent);
                finish(); // Закрываем SplashActivity
            }
        }, 5000); // Задержка в 3 секунды

    }
}