package com.example.v_prog_elm327;

import static android.service.controls.actions.ControlAction.isValidResponse;

import android.Manifest;
import android.annotation.SuppressLint;
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
    private static final int REQUEST_PERMISSIONS = 2;
    private static final int REQUEST_BLUETOOTH_SCAN = 3;
    private static final int REQUEST_BLUETOOTH_CONNECT = 4;
    private static final int REQUEST_LOCATION = 5;


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;

    private ArrayAdapter<String> devicesAdapter;
    private ArrayList<String> devicesList = new ArrayList<>();
    private ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();

    // UUID для SPP (Serial Port Profile)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String[] MAC_ADAPTER = {
            "00:1D:A5:05:EE:47",// MAC-адрес вашего устройства Kingbolen
            "66:1E:11:8D:ED:7D",//MAC-адрес вашего устройства КРАСНОГО АДАПТЕРА
            "66:1E:11:8D:FC:E9",// MAC-адрес вашего устройства КРАСНОГО АДАПТЕРА от Ришата
            "00:1D:A5:68:98:8A",// MAC-адрес вашего устройства сининего АДАПТЕРА
            "00:1D:A5:00:0B:A1" // MAC-адрес вашего устройства БОЛЬШОЙ ЧЕРНЫЙ АДАПТЕРА
    };
    private static final String DEVICE_ADDRESS = MAC_ADAPTER[0]; // MAC-адрес вашего устройства Kingbolen

    private Button btnScanECU, btnIdenECU, btnReadDTC, btnClearDTC;
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
        btnIdenECU = findViewById(R.id.btnIdenECU);

        btnReadDTC = findViewById(R.id.btnReadDTC);
        btnClearDTC = findViewById(R.id.btnClearDTC);

        logListView = findViewById(R.id.logListView);
        receivedDataTextView = findViewById(R.id.receivedDataTextView);
        receivedDataPower = findViewById(R.id.receivedDataPower);
        imageBluetooth = findViewById(R.id.imageBluetooth);


        btnIdenECU.setTextColor(Color.GRAY);
        ((ArrayAdapter) logListView.getAdapter()).clear();

        // Получаем Bluetooth адаптер
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        receivedDataTextView.setText("");
        // Проверка поддержки Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_LONG).show();
            return;
        }

        imageBluetooth.setColorFilter(Color.GRAY);

        connectToELM327();
        receivedDataPower.setOnClickListener(v -> connectToELM327());

        /// кнопка чтения идентов ECU
        btnIdenECU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ArrayAdapter) logListView.getAdapter()).clear();
                sendCommand("0900\r", response1 -> {  /// Запрос информации об ECU
//                    sendCommand("0902\r", response2 -> {  /// Запрос информации об ECU
//
//                    });
                });
            }
        });
        /// кнопка чтение ошибок
        btnReadDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ArrayAdapter) logListView.getAdapter()).clear();
            }
        });
        /// кнопка  стирания ошибок
        btnClearDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ArrayAdapter) logListView.getAdapter()).clear();
            }
        });
        /// /кнопка поиск ECU
        btnScanECU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ((ArrayAdapter) logListView.getAdapter()).clear();
                receivedDataPower.setText("");
                receivedDataTextView.setText("");
                sendCommand("ATE0\r", response -> { /// отключение эхо
                    sendCommand("ATI\r", response2 -> { /// версия прошивки
                        sendCommand("AT@SN\r", response3 -> { /// Серийный номер адаптера:
                            sendCommand("ATRV\r", response4 -> { /// напряжение на адаптере
                                sendCommand("ATH1\r", response5 -> { /// вкл-е отобр-я CAN-сообщений
                                    sendCommand("ATSP0\r", response6 -> { /// автовыбор протокола
                                        //sendCommand("ATM0\r", response7 -> {    ////вывод ответов без пробелов
                                        //sendCommand("ATS0\r", response8 -> {    //// компактный режим
                                        sendCommand("ATAT1\r", response9 -> {   /// вкл-е таймаут
                                            sendCommand("ATAL\r", response10 -> {  /// /разрешение длинных сообщений
                                                sendCommand("ATST64\r", response11 -> {  /// 64 мс макс.время ожидания
                                                    sendCommand("0100\r", response12 -> {  /// Запрос PIDs


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
            sendCommand("ATZ\r", response -> {
                sendCommand("ATE0\r", response2 -> {
                    sendCommand("STI\r", response3 -> {
                        // addLog(" Адаптер: ");
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


    @SuppressLint("UseCompatTextViewDrawableApis")
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
                   // receivedDataTextView.setText(words[0]);

                    /// ответ информация об адапторе при коннекте с адаптером
                    if (command.startsWith("STI")) {
                        receivedDataTextView.setText(words[0]);
                    }


                    /// ответ информация об адапторе
                    if (command.equals("ATI\r")) {
                        //addLog(">> " + command);
                        addLog("Адаптер " + responseStr);
                        receivedDataPower.setText(words[0]);
                    }

                    if (command.equals("AT@SN\r")) {
                        /// ответ серийном номере адаптора
                        if (words[0].equals("?") || words[0].equals("OK")) {
                            addLog(" Подделка " + words[0]);
                            receivedDataPower.setTextColor(Color.RED); /// не оригинальный адаптер
                        } else {
                            addLog(" Оригинал " + words[0]);
                            receivedDataPower.setTextColor(Color.GREEN); /// оригинальный адаптер
                        }
                    }


                    /// напряжение адапторе
                    if (command.equals("ATRV\r")) {
                        addLog("Напряжение: " + responseStr);
                    }


                    if (command.startsWith("0900\r")) {
                        if (responseStr.startsWith("7E8")) {
                            addLog(" P/N:  " + responseStr);
                            btnScanECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.YELLOW));

                            filterResponse(responseStr);
                            receivedDataTextView.setText(responseStr);
                            String[] pNum = responseStr.split(" ");
                            receivedDataTextView.setText(responseStr + " !!!");

                            addLog("  >1> " + words[4] + "  >2> " + words[5] + "  >3> " + words[6] + "  >4> " + words[7]);

                        } else {
                            addLog(" P/N - N/A");
                            receivedDataTextView.setText(responseStr + " !!!!!!");
                            btnScanECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                        }
                    }

                    if (command.startsWith("0100\r")) {
                        if (responseStr.startsWith("SEARCHING...\r7E8")) {
                            addLog(" ECU Connect" + responseStr);
                            btnScanECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.YELLOW));
                            btnIdenECU.setTextColor(Color.WHITE);
                            btnReadDTC.setTextColor(Color.WHITE);
                            btnClearDTC.setTextColor(Color.WHITE);
                            filterResponse(responseStr);
                            receivedDataTextView.setText(responseStr);
                            String[] pid = responseStr.split(" ");
                            receivedDataTextView.setText(pid[0]);

                            // Вызов функции для анализа поддерживаемых PID
                            if (pid.length >= 8) {
                                analyzeSupportedPIDs(pid[4], pid[5], pid[6], pid[7]);
                            } else {
                                addLog(" Недостаточно данных для анализа PID");
                            }
                        } else {
                            addLog(" ECU No Connect");
                            btnIdenECU.setTextColor(Color.GRAY);
                            btnReadDTC.setTextColor(Color.GRAY);
                            btnClearDTC.setTextColor(Color.GRAY);
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







    // Функция для анализа поддерживаемых PID
    private void analyzeSupportedPIDs(String byte1Hex, String byte2Hex, String byte3Hex, String byte4Hex) {
        try {
            // Преобразуем HEX в бинарный формат
            String binaryString1 = String.format("%8s", Integer.toBinaryString(Integer.parseInt(byte1Hex, 16))).replace(' ', '0');
            String binaryString2 = String.format("%8s", Integer.toBinaryString(Integer.parseInt(byte2Hex, 16))).replace(' ', '0');
            String binaryString3 = String.format("%8s", Integer.toBinaryString(Integer.parseInt(byte3Hex, 16))).replace(' ', '0');
            String binaryString4 = String.format("%8s", Integer.toBinaryString(Integer.parseInt(byte4Hex, 16))).replace(' ', '0');

//            addLog(" >1>" + byte1Hex + "> " + binaryString1 +
//                    " \n >2>" + byte2Hex + "> " + binaryString2 +
//                    " \n >3>" + byte3Hex + "> " + binaryString3 +
//                    " \n >4>" + byte4Hex + "> " + binaryString4);

            // Анализ поддерживаемых PID
            StringBuilder supportedPids = new StringBuilder();
            supportedPids.append("Поддерживаемые PID:\n");

            // Байт 1: PIDs 01-08
            supportedPids.append("Байт 1 (").append(byte1Hex).append(") - PIDs 01-08:\n");
            analyzeBytePIDs(binaryString1, 1, supportedPids);

            // Байт 2: PIDs 09-16
            supportedPids.append("\nБайт 2 (").append(byte2Hex).append(") - PIDs 09-16:\n");
            analyzeBytePIDs(binaryString2, 9, supportedPids);

            // Байт 3: PIDs 17-1F (17-31)
            supportedPids.append("\nБайт 3 (").append(byte3Hex).append(") - PIDs 17-1F:\n");
            analyzeBytePIDs(binaryString3, 17, supportedPids);

            // Байт 4: PIDs 20-27
            supportedPids.append("\nБайт 4 (").append(byte4Hex).append(") - PIDs 20-27:\n");
            analyzeBytePIDs(binaryString4, 20, supportedPids);

            // Выводим результат
            addLog(supportedPids.toString());

        } catch (NumberFormatException e) {
            addLog(" Ошибка преобразования HEX данных: " + byte1Hex + " " + byte2Hex + " " + byte3Hex + " " + byte4Hex);
            e.printStackTrace();
        }
    }


    // Вспомогательная функция для анализа байта
    private void analyzeBytePIDs(String binaryString, int startPid, StringBuilder result) {
        // Идем по битам справа налево (младший бит первый)
        for (int i = 7; i >= 0; i--) {
            char bit = binaryString.charAt(7 - i); // Инвертируем порядок битов
            int currentPid = startPid + i;

            if (bit == '1') {
                result.append("  PID ").append(String.format("%02X", currentPid))
                        .append(" (").append(currentPid).append(") - ")
                        .append(getPidDescription(currentPid)).append("\n");
            }
        }
    }
    // Функция для получения описания PID
    private String getPidDescription(int pid) {
        switch (pid) {
            case 1: return "Мониторинг статуса";
            case 2: return "Freeze DTC (замороженные коды ошибок)";
            case 3: return "Топливная система";
            case 4: return "Расчётная нагрузка";
            case 5: return "Температура охлаждающей жидкости";
            case 6: return "Краткосрочная коррекция топлива";
            case 7: return "Долгосрочная коррекция топлива";
            case 8: return "Положение дроссельной заслонки";
            case 9: return "Давление топлива";
            case 10: return "Абсолютное давление в коллекторе";
            case 11: return "Обороты двигателя (RPM)";
            case 12: return "Скорость автомобиля";
            case 13: return "Угол опережения зажигания";
            case 14: return "Температура впускного воздуха";
            case 15: return "Расход воздуха";
            case 16: return "Положение педали газа";
            case 17: return "Напряжение кислородного датчика (банк 1)";
            case 18: return "Напряжение кислородного датчика (банк 2)";
            case 19: return "Напряжение кислородного датчика (банк 3)";
            case 20: return "Напряжение кислородного датчика (банк 4)";
            case 21: return "Пробег с последнего сброса ошибок";
            case 22: return "Давление в топливной рампе";
            case 23: return "Давление в топливной системе";
            case 24: return "Соотношение воздух/топливо (банк 1)";
            case 25: return "Соотношение воздух/топливо (банк 2)";
            case 26: return "Соотношение воздух/топливо (банк 3)";
            case 27: return "Соотношение воздух/топливо (банк 4)";
            case 28: return "Температура катализатора";
            case 29: return "Температура катализатора (банк 2)";
            case 30: return "Время работы двигателя";
            case 31: return "Дистанция с последнего сброса ошибок";
            default: return "Неизвестный параметр";
        }
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














