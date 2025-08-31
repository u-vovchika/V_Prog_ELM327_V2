package com.example.v_prog_elm327;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class Help_online extends AppCompatActivity {
    private EditText editTextUid;
    private TextView textViewResult;
    private OkHttpClient client;
private Button button_back_univ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_online);

        editTextUid = findViewById(R.id.editTextUid);
        textViewResult = findViewById(R.id.textViewResult);
        button_back_univ = findViewById(R.id.button_back_univ);
        Button buttonFetch = findViewById(R.id.buttonFetch);

        client = new OkHttpClient();


        // возрат назад  ///////////////////////////////////////////////////////
        button_back_univ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.clear(); // стираем данные textViewModel
//                editor.apply(); // записываем данные после очистки textViewModel
//                finish();// завершения процесса
                Intent intent = new Intent(Help_online.this, MenuActivity.class);
                startActivity(intent);
            }
        });


        buttonFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String uid = editTextUid.getText().toString().trim();


                if (!uid.isEmpty()) {
                    fetchDataFromServer(uid);
                } else  {
                    fetchDataFromServerUpdate();
                    Toast.makeText(Help_online.this, "iPROG_PRO_HELP", Toast.LENGTH_SHORT).show();
                }
//                else {
//                    Toast.makeText(Help_online.this, "Введите номер блока!!!", Toast.LENGTH_SHORT).show();
//                }
            }
        });

    }
    private void fetchDataFromServer(String uid) {
        textViewResult.setTextColor(Color.WHITE);
        String url = "http://u-vovchikaweb.ru/IPROG_PRO_HELP/" + uid;
        //String url = "http://u-vovchikaweb.ru/IPROG_PRO_HELP/01304045";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //textViewResult.setText("Ошибка подключения: " + e.getMessage());
                        textViewResult.setText("Ошибка подключения: ");
                        textViewResult.setTextColor(Color.RED);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                final int statusCode = response.code();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (statusCode == 200) {
                            textViewResult.setText("Статус: 200 OK\n\n" + responseBody);
                        } else if (statusCode == 404) {
                            textViewResult.setText("Статус: 404\nВ базе нет такого номера(((");
                            textViewResult.setTextColor(Color.RED);
                        } else if (statusCode == 500) {
                            textViewResult.setText("Статус: 500\nInvalid Script");
                            textViewResult.setTextColor(Color.RED);
                        } else {
                            textViewResult.setText("Неизвестный статус: " + statusCode + "\n" + responseBody);
                            textViewResult.setTextColor(Color.BLUE);
                        }
                    }
                });
            }
        });
    }
    private void fetchDataFromServerUpdate() {
        textViewResult.setTextColor(Color.WHITE);
        String url = "http://u-vovchikaweb.ru/WRITE_TEST/Update_Iprog_u_vovchika.txt";
        //String url = "http://u-vovchikaweb.ru/IPROG_PRO_HELP/01304045";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ///textViewResult.setText("Ошибка подключения: " + e.getMessage());
                        textViewResult.setText("Ошибка подключения: ");
                        textViewResult.setTextColor(Color.RED);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseBody = response.body().string();
                final int statusCode = response.code();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (statusCode == 200) {
                            textViewResult.setText("Статус: 200 OK\n\n" + responseBody);
                        } else if (statusCode == 404) {
                            textViewResult.setText("Статус: 404\nВ базе нет такого номера(((");
                            textViewResult.setTextColor(Color.RED);
                        } else if (statusCode == 500) {
                            textViewResult.setText("Статус: 500\nInvalid Script");
                            textViewResult.setTextColor(Color.RED);
                        } else {
                            textViewResult.setText("Неизвестный статус: " + statusCode + "\n" + responseBody);
                            textViewResult.setTextColor(Color.BLUE);
                        }
                    }
                });
            }
        });
    }
}