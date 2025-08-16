package com.example.v_prog_elm327;

import static android.service.controls.actions.ControlAction.isValidResponse;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
//import java.util.logging.Handler;
import java.util.function.Consumer;
import java.util.logging.LogRecord;

import android.os.Handler;
import android.os.Looper;


public class BluetoothActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2; // Код для запроса разрешений
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    // UUID для SPP (Serial Port Profile)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID ELM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //private static final String DEVICE_ADDRESS = "98:DA:50:01:B4:7C"; // MAC-адрес вашего устройства
    private static final String DEVICE_ADDRESS = "00:1D:A5:05:EE:47"; // MAC-адрес вашего устройства Kingbolen
    //private static final String DEVICE_ADDRESS = "66:1E:11:8D:ED:7D"; // MAC-адрес вашего устройства КРАСНОГО АДАПТЕРА
    private Button btnScanECU, btnGetVoltage;
    private ListView logListView;
    private TextView receivedDataTextView, receivedDataPower;
    private ArrayAdapter<String> logAdapter;
    private ArrayList<String> logMessages = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    ImageView imageBluetooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        logListView = findViewById(R.id.logListView);
        logAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logMessages);
        logListView.setAdapter(logAdapter);
        btnScanECU = findViewById(R.id.btnScanECU);
        btnGetVoltage = findViewById(R.id.btnGetVoltage);
        logListView = findViewById(R.id.logListView);
        receivedDataTextView = findViewById(R.id.receivedDataTextView);
        receivedDataPower = findViewById(R.id.receivedDataPower);
        imageBluetooth = findViewById(R.id.imageBluetooth);
        // Получаем Bluetooth адаптер
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        receivedDataTextView.setText("");
        // Проверка поддержки Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_LONG).show();
            return;
        }

        imageBluetooth.setColorFilter(Color.GRAY);

        //connectToELM327();
        imageBluetooth.setOnClickListener(v -> connectToELM327());
        //btnScanECU.setOnClickListener(v -> sendCommand_old("0900")); // Поиск ECU
        //btnScanECU.setOnClickListener(v -> sendCommand("090C",response5 ->{})); // Поиск ECU

        btnGetVoltage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receivedDataPower.setText("");
                receivedDataTextView.setText("");

                sendCommand("ATE0\nATRV\r", response11 -> { /// автовыбор протокола
                    addLog(" Напряжение на адаптере ");
                });

            }
        });
        btnScanECU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                receivedDataPower.setText("");
                receivedDataTextView.setText("");
                sendCommand("ATE0\r", response -> { /// отключение эхо
                    sendCommand("ATI\r", response2 -> { /// версия прошивки

                        sendCommand("AT@SN\r", response3 -> { /// вывод сер.номера ориг адаптера
                            addLog(" Серийный номер адаптера: ");

                            sendCommand("ATRV\r", response4 -> { /// вывод сер.номера ориг адаптера
                                addLog(" напряжение на адаптере: ");

                                sendCommand("ATH1\r", response5 -> { /// вкл-е отобр-я CAN-сообщений
                                    addLog(" Вкл-е отображения CAN-сообщ-й ");

                                    sendCommand("ATSP0\r", response6 -> { /// автовыбор протокола
                                        addLog(" автовыбор протокола ");

                                        //sendCommand("ATM0\r", response7 -> {
                                        //    addLog(" вывод ответов без пробелов ");

                                        //sendCommand("ATS0\r", response8 -> {
                                        //   addLog(" компактный режим ");

                                        sendCommand("ATAT1\r", response9 -> {
                                            addLog(" вкл-е таймаут ");

                                            sendCommand("ATAL\r", response10 -> {
                                                addLog(" разрешение длинных сообщений ");

                                                sendCommand("ATST64\r", response11 -> {
                                                    addLog(" 64 мс макс.время ожидания ");

                                                    sendCommand("0100\r", response12 -> {
                                                        addLog(" Запрос PIDs ");


                                                    });
                                                });
                                            });
                                        });
                                        //   });
                                        // });
                                    });
                                });
                            });
                        });
                    });
                });
            }
        });

    }


    // Улучшенный метод отправки команд с таймаутом
    private void sendCommand2(String command, Consumer<String> callback) {
        if (outputStream == null) {
            showToast("Нет подключения!");
            return;
        }

        new Thread(() -> {
            try {
                // Очистка буфера перед отправкой
                while (inputStream.available() > 0) {
                    inputStream.read();
                }

                outputStream.write((command + "\r\n").getBytes());
                outputStream.flush();

                StringBuilder response = new StringBuilder();
                byte[] buffer = new byte[1024];
                long startTime = System.currentTimeMillis();
                int timeout = command.startsWith("09") ? 3000 : 1500; // Увеличенный таймаут для 09xx команд

                // Чтение с таймаутом
                while (System.currentTimeMillis() - startTime < timeout) {
                    if (inputStream.available() > 0) {
                        int bytes = inputStream.read(buffer);
                        response.append(new String(buffer, 0, bytes));

                        // Проверка на завершающий символ '>'
                        if (response.toString().contains(">")) {
                            break;
                        }
                    }
                    Thread.sleep(50);
                }

                final String responseStr = filterResponse(response.toString());
                runOnUiThread(() -> {
                    addLog(">> " + command);
                    addLog("<< " + responseStr);
                    if (callback != null) {
                        callback.accept(responseStr);
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> showToast("Ошибка " + command + ": " + e.getMessage()));
            }
        }).start();
    }


    // Фильтрация ответа
    private String filterResponse(String response) {
        return response.replaceAll("\r", "")
                .replaceAll("\n", " ")
                .replaceAll(">", "")
                .replaceAll("SEARCHING...", "")
                .trim();
    }


    // Парсинг VIN (модифицированная версия)
    private String parseVIN(String response) {
        try {
            String[] parts = response.split("4900");
            if (parts.length < 2) return "Invalid format";

            String hexData = parts[1].replaceAll(" ", "");
            if (hexData.length() < 64) return "Incomplete data";

            StringBuilder vin = new StringBuilder();
            for (int i = 0; i < 64; i += 2) {
                String hex = hexData.substring(i, i + 2);
                vin.append((char) Integer.parseInt(hex, 16));
            }
            return vin.toString().trim();
        } catch (Exception e) {
            return "VIN parse error: " + e.getMessage();
        }
    }


    private void connectToELM327() {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            receivedDataTextView.setTextColor(Color.parseColor("#FF00FF00"));
            imageBluetooth.setColorFilter(Color.GREEN);
            imageBluetooth.setImageResource(R.drawable.outline_bluetooth_connected_24);
            Toast.makeText(this, "Подключено", Toast.LENGTH_SHORT).show();
            receivedDataPower.setText("Connect");
            receivedDataPower.setTextColor(Color.GREEN);
            Thread.sleep(200);
            /// сброс адаптера и отключение эхо ////////////////////////
            sendCommand("ATZ\rATE0\r", response -> {
                sendCommand("ATE0\r", response2 -> {
                    sendCommand("STI\r", response3 -> {
                    });
                });
            });



        } catch (Exception e) {
            imageBluetooth.setColorFilter(Color.GRAY);
            imageBluetooth.setImageResource(R.drawable.outline_bluetooth_disabled_24);
            receivedDataPower.setText("No adapter");
            receivedDataPower.setTextColor(Color.GRAY);
            btnScanECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
            Log.e("Bluetooth", "Ошибка подключения", e);
            Toast.makeText(this, "Ошибка подключения", Toast.LENGTH_SHORT).show();
        }
    }

    // Проверка поддерживаемых сервисов
    private void checkSupportedServices() {
        sendCommand("0100", response -> {
            String filtered = filterResponse(response);
            addLog("Поддержка PIDs 01-20: " + filtered);

            if (isValidResponse(filtered, "4100")) {
                parseSupportedPIDs(filtered);
                requestVehicleInfo();
            } else {
                addLog("Ошибка: ECU не поддерживает стандартные PIDs");
            }
        });
    }

    // Запрос информации об автомобиле
    private void requestVehicleInfo() {
        sendCommand("0900", response -> {
            String filtered = filterResponse(response);
            addLog("Данные автомобиля: " + filtered);

            if (filtered.contains("4900")) {
                String vin = parseVIN(filtered);
                addLog("VIN: " + vin);
            }

            sendCommand("0902", response2 -> {
                String ecuInfo = filterResponse(response2);
                addLog("ECU ID: " + ecuInfo);
                parseECUInfo(ecuInfo);
            });
        });
    }

    // Проверка валидности ответа
    private boolean isValidResponse(String response, String expectedPrefix) {
        return response.startsWith(expectedPrefix);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    interface ResponseCallback {
        void onResponse(String response);
    }

    // Парсинг поддерживаемых PIDs
    private void parseSupportedPIDs(String response) {
        String[] parts = response.split(" ");
        if (parts.length >= 6) {
            long pidMask = Long.parseLong(parts[2] + parts[3] + parts[4] + parts[5], 16);
            // Здесь можно добавить анализ битовой маски
        }
    }


    private void sendCommand(String command, ResponseCallback callback) {
        if (outputStream == null) {
            showToast("Нет подключения!");
            return;
        }
        // Создаем Handler для основного потока, если он еще не был создан
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        new Thread(() -> {

            // Получаем drawable из ресурсов, если его нет
            final Drawable[] leftDrawable = {btnScanECU.getCompoundDrawables()[0]};
            if (leftDrawable[0] == null) {
                leftDrawable[0] = ContextCompat.getDrawable(this, R.drawable.outline_check_circle_unread_24);
            }
            try {
                // Отправка команды
                outputStream.write((command + "\r").getBytes());
                outputStream.flush();
                // Чтение ответа
                StringBuilder response = new StringBuilder();
                byte[] buffer = new byte[1024];
                int bytes;

                // Ждем ответа (можно добавить таймаут)
                Thread.sleep(200); // Даем адаптеру время на ответ
                while (inputStream.available() > 0) {
                    bytes = inputStream.read(buffer);
                    response.append(new String(buffer, 0, bytes));
                }

                final String responseStr = response.toString().trim();
                // Используем handler для обновления UI из фонового потока
                handler.post(() -> {
//                    addLog(">> " + command);
//                    addLog("<< " + responseStr);

                    filterResponse(responseStr);
                    String[] words = responseStr.split("\r");
                    receivedDataTextView.setText(words[0]);

                    /// ответ информация об адапторе при коннекте с адаптером
                    if (command.equals("STI\n")) {

                        //  if (responseStr.startsWith("ELM")) {
                        addLog("!!! " + responseStr + " !!!");
                        receivedDataTextView.setText(words[0]);
                        //receivedDataPower.setTextColor(Color.GREEN);
                        // }
                    }

                    /// ответ информация об адапторе
                    if (command.equals("ATI\r")) {
                        receivedDataPower.setText(words[0]);
                        //receivedDataPower.setTextColor(Color.GREEN);

                    }

                    if (command.equals("AT@SN\r")) {
                        addLog(">> " + command);
                        addLog("<< " + responseStr);
                        /// ответ серийном номере адаптора
                        if (words[0].equals("?") || words[0].equals("OK")) {
                            addLog(" Подделка ");
                            receivedDataPower.setTextColor(Color.RED); /// не оригинальный адаптер
                        } else {
                            addLog(" Оригинал ");
                            receivedDataPower.setTextColor(Color.GREEN); /// оригинальный адаптер
                        }
                    }


                    if (command.equals("0100\r")) {
                        if (responseStr.startsWith("SEARCHING...\r7E8")) {
                            addLog(" ECU Connect");
                            btnScanECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.YELLOW));

                            filterResponse(responseStr);
                            receivedDataTextView.setText(responseStr);
                            String[] pid = responseStr.split(" ");
                            receivedDataTextView.setText(pid[0]);
                            addLog("  >1> " + pid[4] + "  >2> " + pid[5] + "  >3> " + pid[6] + "  >4> " + pid[7]);

                            // Преобразуем pid[4] в бинарный формат
                            try {
                                int decimalValue1 = Integer.parseInt(pid[4], 16); // Предполагаем, что это HEX значение
                                int decimalValue2 = Integer.parseInt(pid[5], 16); // Предполагаем, что это HEX значение
                                int decimalValue3 = Integer.parseInt(pid[6], 16); // Предполагаем, что это HEX значение
                                int decimalValue4 = Integer.parseInt(pid[7], 16); // Предполагаем, что это HEX значение
                                String binaryString1 = Integer.toBinaryString(decimalValue1);
                                String binaryString2 = Integer.toBinaryString(decimalValue2);
                                String binaryString3 = Integer.toBinaryString(decimalValue3);
                                String binaryString4 = Integer.toBinaryString(decimalValue4);

                                // Добавляем ведущие нули для 8-битного представления
                                binaryString1 = String.format("%8s", binaryString1).replace(' ', '0');

                                addLog(" >1>" + pid[4]+ "> " + binaryString1 + " \n >2>" + pid[5]+ "> "  + binaryString2 + " \n >3>" +pid[6]+ "> "  + binaryString3 + " \n >4>" +pid[7]+ "> "  + binaryString4);
                            } catch (NumberFormatException e) {
                                addLog(" >1> [Ошибка преобразования] >2> " + pid[5] + " >3> " + pid[6] + " >4> " + pid[7]);
                                e.printStackTrace();
                            }


                        } else {
                            addLog(" ECU No Connect");
                            btnScanECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                        }
                    }



                    if (callback != null) {
                        callback.onResponse(responseStr);
                    }
                });


            } catch (IOException | InterruptedException e) {
                // Обработка ошибок также через handler
                handler.post(() -> showToast("Ошибка: " + e.getMessage()));
            }
        }).start();
    }


    // Разбор идентификационной информации ECU
    private void parseECUInfo(String ecuResponse) {
        // Пример простого разбора ответа на команду 0902
        if (ecuResponse.contains(":")) {
            String[] parts = ecuResponse.split(":");
            if (parts.length > 1) {
                String ecuId = parts[1].trim()
                        .replaceAll(" ", "")
                        .replaceAll("\r", "")
                        .replaceAll("\n", "");

                if (!ecuId.isEmpty()) {
                    showToast("Найден ECU ID: " + ecuId);
                    // Здесь можно сохранить ID или выполнить другие действия
                }
            }
        }
    }

    //    private String parseVIN(String response) {
//        // Ответ на 0900 приходит в формате:
//        // 49 00 BE 3F B8 13 00 ...
//        // Нужно извлечь HEX данные и конвертировать в VIN
//
//        String hexData = response.replaceAll("49 00", "")
//                .replaceAll(" ", "")
//                .replaceAll("\r", "")
//                .replaceAll("\n", "")
//                .trim();
//
//        if (hexData.length() < 14) return "Неверный формат VIN";
//
//        try {
//            String vinHex = hexData.substring(0, 32); // Первые 17 байт (34 символа)
//            return hexToAscii(vinHex);
//        } catch (Exception e) {
//            return "Ошибка разбора VIN: " + e.getMessage();
//        }
//    }
    private String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    private void receiveData() {
        byte[] buffer = new byte[2048]; // Буфер для хранения данных
        int bytes; // Количество прочитанных байтов

        while (true) {
            try {
                bytes = inputStream.read(buffer); // Чтение данных
                String receivedMessage = new String(buffer, 0, bytes); // Преобразование байтов в строку
                runOnUiThread(() -> receivedDataTextView.setText(receivedMessage)); // Обновление UI
                /// ////////////////////////////////////////////////////////////////////////////////
                if (bytes == -1) {
                    // Если bytes равен -1, это означает конец потока
                    Log.e("Bluetooth", "Конец потока данных");
                    break;
                }
            } catch (Exception e) {
                Log.e("Bluetooth", "Ошибка приема данных", e);
                break;
            }
        }
    }

    private void addLog(String message) {
        logMessages.add(message);
        logAdapter.notifyDataSetChanged();
        logListView.smoothScrollToPosition(logMessages.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            Log.e("ELM327", "Ошибка закрытия соединения", e);
        }
    }
}














