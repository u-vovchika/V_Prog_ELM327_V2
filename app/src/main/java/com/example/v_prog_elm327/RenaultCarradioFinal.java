package com.example.v_prog_elm327;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class RenaultCarradioFinal extends AppCompatActivity {
    private EditText letterEditText;
    private EditText numberEditText;
    private TextView resultTextView;
    private TextView securityCode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renault_carradio_final);

        // строка состояния убирается
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // возрат назад  ///////////////////////////////////////////////////////
        Button buttonBack = findViewById(R.id.button_back_univ);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RenaultCarradioFinal.this, UniversalCarRadio.class);
                startActivity(intent);
            }
        });


        letterEditText = findViewById(R.id.letterEditText);
        numberEditText = findViewById(R.id.numberEditText);
        resultTextView = findViewById(R.id.resultTextView);
        securityCode = findViewById(R.id.securityCode);
        Button buttonHelp = findViewById(R.id.buttonHelp);

        buttonHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RenaultCarradioFinal.this, RenaultCarradioFinalHelp.class);
                startActivity(intent);
            }
        });

        Button calculateButton = findViewById(R.id.calculateButton);

        calculateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calculateCode();
            }
        });

    }

    private void calculateCode() {
        String letterInput = letterEditText.getText().toString();
        String numberInput = numberEditText.getText().toString();


        if (letterInput.length() != 1 || !Character.isUpperCase(letterInput.charAt(0))) {
            Toast.makeText(this, "Ошибка: Введите заглавную букву.", Toast.LENGTH_SHORT).show();
        }
        if (numberInput.length() != 3) {
            Toast.makeText(this, "Ошибка: Введите три цифры.", Toast.LENGTH_SHORT).show();
        }

        char letter = letterInput.charAt(0);
        // int v0 = letter - 'A';
        int v0 = 0;
//        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; // Пример строки букв
//
//        for (char letterR : letters.toCharArray()) {
//            v0 = -1; // Инициализация переменной v0
//            // Проверка, находится ли буква в диапазоне от 'A' до 'Z'
//            if (letterR >= 'A' && letterR <= 'Z') {
//                v0 = letterR - 'A'; // Вычисляем значение v0
//            }
//            //System.out.println("Значение v0 для буквы " + letterR + ": " + v0);
//        }
        String secCode = "" + letterInput + numberInput;
        securityCode.setText("Security CODE: " + secCode);
        if (letter == 'A') v0 = 0;
        if (letter == 'B') v0 = 1;
        if (letter == 'C') v0 = 2;
        if (letter == 'D') v0 = 3;
        if (letter == 'E') v0 = 4;
        if (letter == 'F') v0 = 5;
        if (letter == 'G') v0 = 6;
        if (letter == 'H') v0 = 7;
        if (letter == 'I') v0 = 8;
        if (letter == 'J') v0 = 9;
        if (letter == 'K') v0 = 10;
        if (letter == 'L') v0 = 11;
        if (letter == 'M') v0 = 12;
        if (letter == 'N') v0 = 13;
        if (letter == 'O') v0 = 14;
        if (letter == 'P') v0 = 15;
        if (letter == 'Q') v0 = 16;
        if (letter == 'R') v0 = 17;
        if (letter == 'S') v0 = 18;
        if (letter == 'T') v0 = 19;
        if (letter == 'U') v0 = 20;
        if (letter == 'V') v0 = 21;
        if (letter == 'W') v0 = 22;
        if (letter == 'X') v0 = 23;
        if (letter == 'Y') v0 = 24;
        if (letter == 'Z') v0 = 25;

        v0 += 65;
        int v10 = Integer.parseInt(numberInput);

        // Логика расчета кода
        int var3 = v10 % 10;
        v10 /= 10;
        int var2 = v10 % 10;
        v10 /= 10;
        int var1 = v10 % 10;

        var3 += 48;
        var2 += 48;
        var1 += 48;


        int var5 = var1 + (10 * v0) - 698;
        if (var5 <= 0) {
            var5 = 1;
        }

        int var14 = (7 * (var5 + (10 * var2) + var3 - 528)) % 100;
        int var16 = var14 / 10;
        int var11 = var14 % 10;
        var11 *= 10;
        var16 += var11;

        int var4 = 259 % var5;
        var4 %= 100;
        var4 *= 0x64;
        var4 += var16;

        int w4 = (var4 % 10);
        var4 /= 10;
        int w3 = (var4 % 10);
        var4 /= 10;
        int w2 = (var4 % 10);
        var4 /= 10;
        int w1 = (var4 % 10);

        String resultCode = "" + w1 + w2 + w3 + w4;
        resultTextView.setText("CODE: " + resultCode);

    }


}