package com.example.v_prog_elm327;

import static android.service.controls.actions.ControlAction.isValidResponse;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;


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
//    private static final String[] MAC_ADAPTER = {
//            "78:DB:2F:F7:9B:4A",  // MAC-адрес вашего устройства ЧЕРНЫЙ АДАПТЕРА vLinker MC
//            "00:1D:A5:05:EE:47", // MAC-адрес вашего устройства Kingbolen
//            "66:1E:11:8D:ED:7D", // MAC-адрес вашего устройства КРАСНОГО АДАПТЕРА
//            "66:1E:11:8D:FC:E9", // MAC-адрес вашего устройства КРАСНОГО АДАПТЕРА от Ришата
//            "00:1D:A5:68:98:8A", // MAC-адрес вашего устройства сининего АДАПТЕРА
//            "00:1D:A5:00:0B:A1"  // MAC-адрес вашего устройства БОЛЬШОЙ ЧЕРНЫЙ АДАПТЕРА
//    };
//    private static final String DEVICE_ADDRESS = MAC_ADAPTER[0]; // MAC-адрес вашего устройства Kingbolen

    private BluetoothDevice selectedDevice; // Выбранное устройство
    private final List<BluetoothDevice> availableDevices = new ArrayList<>();

    private Button btnDataECU, btnIdenECU, btnReadDTC, btnClearDTC;
    private ListView logListView;
    private TextView receivedDataPower;
    private ArrayAdapter<String> logAdapter;
    private ArrayList<String> logMessages = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    ImageView imageBluetooth;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        logListView = findViewById(R.id.logListView);
        logAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logMessages);
        logListView.setAdapter(logAdapter);
        btnDataECU = findViewById(R.id.btnDataECU);
        btnIdenECU = findViewById(R.id.btnIdenECU);
        btnReadDTC = findViewById(R.id.btnReadDTC);
        btnClearDTC = findViewById(R.id.btnClearDTC);
        logListView = findViewById(R.id.logListView);
        receivedDataPower = findViewById(R.id.receivedDataPower);
        imageBluetooth = findViewById(R.id.imageBluetooth);


        btnDataECU.setTextColor(Color.GRAY);
        ((ArrayAdapter) logListView.getAdapter()).clear();

        // Получаем Bluetooth адаптер
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        imageBluetooth.setColorFilter(Color.GRAY);

        /// подключение Bluetooth /////////////////////////////
        discoverBluetoothDevices();
        receivedDataPower.setOnClickListener(v -> discoverBluetoothDevices());

        receivedDataPower.setText("");
        /// кнопка чтения идентов ECU
        btnIdenECU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLog("🔍 Ident ECU ...");
                readIden();
            }
        });

        /// кнопка чтение ошибок
        btnReadDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLog("🔍 Read DTC ...");
                readDtc();
            }
        });

        /// кнопка  стирания ошибок
        btnClearDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLog("🧹 Erase DTC ...");
                clearDTC();
            }
        });

        /// /кнопка поиск ECU
        btnDataECU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ArrayAdapter) logListView.getAdapter()).clear();
                //addLog("🔍 Connect ECU ...");
                sendCommand("01421\r", response17 -> {  /// Clear DTC
                    readKm();
                });
            }
        });

    }


    private void scanECU() {
        ((ArrayAdapter) logListView.getAdapter()).clear();
        sendCommand("ATE0\r", responseATE0 -> { /// отключение эхо
            sendCommand("ATI\r", responseATI -> { /// версия прошивки
                sendCommand("AT@SN\r", responseAT_SN -> { /// Серийный номер адаптера:
                    sendCommand("ATRV\r", responseATRV -> { /// напряжение на адаптере
                        sendCommand("ATH1\r", responseATH1 -> { /// вкл-е отобр-я CAN-сообщений
                            sendCommand("ATSP0\r", responseATSP0 -> { /// автовыбор протокола
                                sendCommand("ATAT1\r", responseATAT1 -> {   /// вкл-е таймаут
                                    sendCommand("ATAL\r", responseATAL -> {  /// /разрешение длинных сообщений
                                        sendCommand("ATST64\r", responseATST64 -> {  /// 64 мс макс.время ожидания
                                            sendCommand("0100\r", response0100 -> {  /// Запрос PIDs

                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    private void clearDTC() {
        ((ArrayAdapter) logListView.getAdapter()).clear();
        sendCommand("0100\r", response12 -> {  /// Запрос PIDs
            sendCommand("ATAR\r", response122 -> {  /// Запрос PIDs
                sendCommand("04\r", response17 -> {  /// Clear DTC

                });
            });
        });
    }

    private void readDtc() {
        ((ArrayAdapter) logListView.getAdapter()).clear();
        sendCommand("0100\r", response12 -> {  /// Запрос PIDs
            sendCommand("ATAR\r", response122 -> {  /// Запрос PIDs
                addLog("\uD83D\uDD0D Текущие ошибки");
                sendCommand("03\r", response17 -> {  /// Текущие ошибки
                    addLog("\uD83D\uDD0D Сохраненные ошибки");
                    sendCommand("07\r", response18 -> {  /// Сохраненные ошибки
                        addLog("\uD83D\uDD0D Постоянные ошибки");
                        sendCommand("0A\r", response19 -> {  /// Постоянные ошибки

                        });
                    });
                });
            });
        });

    }

    private void readIden() {
        ((ArrayAdapter) logListView.getAdapter()).clear();
        sendCommand("0100\r", response12 -> {  /// Запрос PIDs
            sendCommand("ATDP\r", response122 -> {  /// Запрос PIDs
                sendCommand("0900\r", response13 -> {  /// Запрос PIDs
                    sendCommand("0902\r", response14 -> {  /// Запрос PIDs
                        sendCommand("0904\r", response15 -> {  /// Запрос PIDs
                            sendCommand("0906\r", response16 -> {  /// Запрос PIDs
                                sendCommand("1A90\r", response17 -> {  /// Запрос PIDs
                                    sendCommand("1A97\r", response1A97 -> {  /// Запрос PIDs
                                        sendCommand("1A71\r", response1A71 -> {  /// Запрос PIDs

                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    /// Сервисные данные для М86
    private void servRec() {
        sendCommand("ATSP6\r", responseATSP -> { // Установить протокол CAN 11bit 500k
            sendCommand("ATH1\r", responseATH -> { // ВКЛЮЧИТЬ заголовки (обязательн
                sendCommand("ATCM7E8\r", responseCM -> {   // Set Mask - маска
                    sendCommand("ATCF7E8\r", responseATCRA5E8 -> {  /// Set Filter - филь
                        sendCommand("ATSH7E0\r", responseATSH7E0 -> {  /// Установка адреса получателя
                            sendCommand("220001\r", responseAA031A -> {  /// Отправка запроса

                            });
                        });
                    });
                });
            });
        });
    }

    private void readKm() {
        sendCommand("ATSP6\r", responseATSP -> { // Установить протокол CAN 11bit 500k
            sendCommand("ATH1\r", responseATH -> { // ВКЛЮЧИТЬ заголовки (обязательн
                sendCommand("ATCM5E8\r", responseCM -> {   // Set Mask - маска
                    sendCommand("ATCF5E8\r", responseATCRA5E8 -> {  /// Set Filter - филь
                        sendCommand("ATSH7E0\r", responseATSH7E0 -> {  /// Установка адреса получателя
                            sendCommand("AA041A\r", responseAA031A -> {  /// Отправка запроса

                            });
                        });
                    });
                });
            });
        });
    }
    /// //////////////////////////////////////////////////////////////////////////////////
    /// Метод для поиска и отображения доступных Bluetooth устройств
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void discoverBluetoothDevices() {
        // Проверяем поддержку Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth не поддерживается", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверяем, включен ли Bluetooth
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        // Очищаем предыдущий список устройств
        availableDevices.clear();

        // Получаем уже сопряженные устройства
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            availableDevices.addAll(pairedDevices);
        }

        // Показываем диалог выбора устройства
        showDeviceSelectionDialog();
    }

    /// Диалог для выбора Bluetooth устройства /////////////////////////////////////////////////
    /// класс для создания диалоговых окон /////////////////////////////////////////////////////
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void showDeviceSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите ELM327 адаптер");
        // Устанавливаем цвет заголовка
        builder.setCustomTitle(getColoredTitle("Select the ELM327 adapter"));
        // Создаем список имен устройств
        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : availableDevices) {
            deviceNames.add(device.getName() + "\n" + device.getAddress());
        }

        // Если устройств нет, показываем сообщение
        if (deviceNames.isEmpty()) {
            deviceNames.add("No devices found");
        }

        builder.setItems(deviceNames.toArray(new String[0]), (dialog, which) -> {
            if (availableDevices.size() > which) {
                selectedDevice = availableDevices.get(which);
                connectToELM327(selectedDevice);
            }
        });

        builder.setNegativeButton("Update", (dialog, which) -> {
            // Запускаем поиск новых устройств
            startDeviceDiscovery();
        });

        builder.setNeutralButton("Cancel", null);
        //builder.show();

        AlertDialog dialog = builder.create();

        // Показываем диалог и затем настраиваем цвета кнопок
        dialog.show();

        // Устанавливаем цвета кнопок
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) {
            negativeButton.setTextColor(Color.BLUE);
        }

        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutralButton != null) {
            neutralButton.setTextColor(Color.RED);
        }

    }

    // Метод для создания цветного заголовка
    private TextView getColoredTitle(String title) {
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.BLACK);
        titleView.setTextSize(20);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setPadding(50, 50, 50, 50);
        titleView.setGravity(Gravity.CENTER);
        return titleView;
    }

    /// ////////////////////////////////////////////////////////////////////////////////////
    // Метод для начала поиска устройств
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startDeviceDiscovery() {
        // Регистрируем BroadcastReceiver для обнаружения устройств
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(discoveryReceiver, filter);

        // Также регистрируем завершение поиска
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(discoveryReceiver, filter);

        // Начинаем поиск
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Устройство найдено
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !availableDevices.contains(device)) {
                    availableDevices.add(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Поиск завершен
                unregisterReceiver(this);
                showDeviceSelectionDialog();
            }
        }
    };

    // Переделанный метод подключения к ELM327
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectToELM327(BluetoothDevice device) {
        if (device == null) {
            Toast.makeText(this, "Устройство не выбрано", Toast.LENGTH_SHORT).show();
            return;
        }

        ((ArrayAdapter) logListView.getAdapter()).clear();

        // Показываем индикатор подключения
        imageBluetooth.setColorFilter(Color.YELLOW);
        imageBluetooth.setImageResource(R.drawable.outline_bluetooth_connected_24);
        receivedDataPower.setText("Подключение...");
        receivedDataPower.setTextColor(Color.GRAY);

        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();

                runOnUiThread(() -> {
                    imageBluetooth.setColorFilter(Color.GREEN);
                    imageBluetooth.setImageResource(R.drawable.outline_bluetooth_connected_24);
                    receivedDataPower.setText("Connect");
                    receivedDataPower.setTextColor(Color.YELLOW);
                    Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                });

                Thread.sleep(700);

                // Сброс адаптера и отключение эхо
                sendCommand("ATZ\rATE0\r", response -> {
                    Thread.sleep(500);
                    sendCommand("ATE0\r", response2 -> {
                        sendCommand("ATE0\r", response3 -> {
                            sendCommand("STI\r", response4 -> {
                                // addLog(" Адаптер: ");
                                scanECU();
                            });
                        });
                    });
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    imageBluetooth.setColorFilter(Color.RED);
                    imageBluetooth.setImageResource(R.drawable.outline_bluetooth_disabled_24);
                    receivedDataPower.setText("Ошибка подключения");
                    receivedDataPower.setTextColor(Color.RED);
                    btnIdenECU.setTextColor(Color.GRAY);
                    btnDataECU.setTextColor(Color.GRAY);
                    btnReadDTC.setTextColor(Color.GRAY);
                    btnClearDTC.setTextColor(Color.GRAY);
                    btnIdenECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                    Toast.makeText(this, "Ошибка подключения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                Log.e("Bluetooth", "Ошибка подключения", e);
            }
        }).start();
    }

    /// //////////////////////////////////////////////////////////////////////////////////////////////////////////////
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
        void onResponse(String response) throws InterruptedException;
    }

    // Парсинг поддерживаемых PIDs
    private void parseSupportedPIDs(String response) {
        String[] parts = response.split(" ");
        if (parts.length >= 6) {
            long pidMask = Long.parseLong(parts[2] + parts[3] + parts[4] + parts[5], 16);
            // Здесь можно добавить анализ битовой маски
        }
    }

    private String convertHexToAscii(String hexStr) {
        try {
            byte[] bytes = new byte[hexStr.length() / 2];
            for (int i = 0; i < bytes.length; i++) {
                int index = i * 2;
                bytes[i] = (byte) Integer.parseInt(hexStr.substring(index, index + 2), 16);
            }
            return new String(bytes, "UTF-8"); // или "ISO-8859-1"
        } catch (Exception e) {
            return hexStr; // возвращаем оригинал при ошибке
        }
    }

    private String extractVinFromResponse(String responseStr) {
        // Удаляем все пробелы и непечатные символы
        String clean = responseStr.replaceAll("\\s+", "");

        // Извлекаем HEX данные и конвертируем в ASCII
        String hexData = clean.replace("7EB", "");

        return convertHexToAscii(hexData);
        //return convertHexToAscii(clean);
    }


    private StringBuilder vinBuilder = new StringBuilder();

    @SuppressLint({"UseCompatTextViewDrawableApis", "SetTextI18n"})
    private void sendCommand(String command, ResponseCallback callback) {
        if (outputStream == null) {
            showToast("❌ No connection!");
            return;
        }
        // Создаем Handler для основного потока, если он еще не был создан
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        new Thread(() -> {
            // Получаем drawable из ресурсов, если его нет
            final Drawable[] leftDrawable = {btnIdenECU.getCompoundDrawables()[0]};
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
                Thread.sleep(500); // Даем адаптеру время на ответ
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

                        if (responseStr.contains(">")) {
                            receivedDataPower.setText("Connect");
                            receivedDataPower.setTextColor(Color.GREEN);
                            Toast.makeText(this, "✅ Adapter connect !!!", Toast.LENGTH_SHORT).show();
                        } else {
                            receivedDataPower.setText("No Connect");
                            receivedDataPower.setTextColor(Color.GRAY);
                            addLog("❌ No Connect ELM327" + responseStr);
                        }
                        //receivedDataTextView.setText(words[0]);
                    }
                    if (command.startsWith("ATAR")) {
                        if (responseStr.startsWith("OK")) {
                            //addLog("Auto Reconnect OK.");
                        } else {
                            addLog("❌ Auto Reconnect ERROR.");
                        }
                    }

                    /// ответ информация об адапторе
                    if (command.equals("ATI\r")) {
                        //addLog(">> " + command);
                        addLog("✅ Адаптер " + responseStr);
                        receivedDataPower.setText(words[0]);
                    }

                    if (command.equals("AT@SN\r")) {
                        /// ответ серийном номере адаптора
                        if (words[0].equals("?") || words[0].equals("OK")) {
                            addLog("❌ Not the original adapter");
                            receivedDataPower.setTextColor(Color.RED); /// не оригинальный адаптер
                        } else {
                            addLog("✅ The original adapter" + words[0]);
                            receivedDataPower.setTextColor(Color.GREEN); /// оригинальный адаптер
                        }
                    }

                    /// напряжение адапторе
                    if (command.equals("ATRV\r")) {
                        addLog("✅ Voltage: " + responseStr);
                    }

                    /// напряжение ECU
                    if (command.equals("01421\r")) {
                        String[] wordss = responseStr.split(" ");
                        if(Objects.equals(wordss[2], "41")){
                            String hex = responseStr.split(" ")[4] + responseStr.split(" ")[5];
                            addLog("✅ Voltage: " + String.format("%.2fV", Integer.parseInt(hex, 16) / 1000.0));
                        } else {
                            addLog("❌ Voltage - N/A 0x" + wordss[2]);
                        }
                    }

                    /// Clear DTC/////////////
                    if (command.startsWith("04\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            if (Objects.equals(wordss[2], "44")) {
                                addLog("✅ Erase DTC ... OK.");
                            } else {
                                addLog("❌ Erase DTC ... ERROR.");
                            }
                        } else {
                            addLog("❌ Erase DTC - N/A");
                        }
                    }
                    /// Read DTC/////////////
                    if (command.startsWith("03\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            if (wordss.length > 5) {
                                if(Objects.equals(wordss[2], "7F")){
                                    addLog("❌ DTC - Answer Error    " + wordss[2]);
                                } else {
                                    addLog("\uD83D\uDD39 P" + wordss[4] + wordss[5]);
                                }

                            } else {
                                addLog("✅ DTC ... No Errors.");
                            }
                        } else {
                            addLog("❌ DTC - N/A");
                        }
                    }

                    if (command.startsWith("07\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            if (wordss.length > 5) {
                                if(Objects.equals(wordss[2], "7F")){
                                    addLog("❌ DTC - Answer Error    " + wordss[2]);
                                } else {
                                    addLog("\uD83D\uDD39 P" + wordss[4] + wordss[5]);
                                }
                            } else {
                                addLog("✅ DTC ... No Errors.");
                            }
                        } else {
                            addLog("❌ DTC - N/A");
                        }
                    }
                    if (command.startsWith("0A\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            if (wordss.length > 5) {
                                if(Objects.equals(wordss[2], "7F")){
                                    addLog("❌ DTC - Answer Error    " + wordss[2]);
                                } else {
                                    addLog("\uD83D\uDD39 P" + wordss[4] + wordss[5]);
                                }
                            } else {
                                addLog("✅ DTC ... No Errors.");
                            }
                        } else {
                            addLog("❌ DTC - N/A");
                        }
                    }
/// /////////////////////////////////////////////////////////////////////////////////////////////////
                    if (command.startsWith("0900\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 6; i < wordss.length; i++) {
                                if (!wordss[i].startsWith("7E8") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("7E8", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullNum = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ ECU id: " + wordss[4]);
                        } else {
                            addLog("❌ ECU id - N/A");
                        }
                    }


                    if (command.startsWith("0902\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 6; i < wordss.length; i++) {
                                if (!wordss[i].startsWith("7E8") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("7E8", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullVin = vinBuilder.toString();
                            //receivedDataTextView.setText("VIN: " + fullVin);
                            vinBuilder.setLength(0);
                            addLog("✅ VIN: " + fullVin);
                        } else {
                            addLog("❌ VIN - N/A  - 0902");
                        }
                    }

                    if (command.startsWith("0904\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 6; i < wordss.length; i++) {
                                if (!wordss[i].startsWith("7E8") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("7E8", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullCalib = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ Calibration ID: " + fullCalib + "\n");
                        } else {
                            addLog("❌ Calibration ID - N/A");
                        }
                    }

                    if (command.startsWith("0906\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 5; i < wordss.length; i++) {
                                if (!wordss[i].startsWith("7E8") &&
                                        !wordss[i].startsWith("21") &&
                                        // !wordss[i].startsWith("55") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("7E8", "");
                                    ;
                                    vinBuilder.append(cleanedString);
                                }
                            }
                            String fullReadiness = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ Calibration Verify Num: " + fullReadiness + "\n");
                        } else {
                            addLog("❌ Calibration Verify Num - N/A");
                        }
                    }

                    if (command.startsWith("1A90\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 5; i < wordss.length - 1; i++) {
                                if (!wordss[i].startsWith("7E8") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("7E8", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullReadiness = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ VIN: " + fullReadiness + "\n");
                        } else {
                            addLog("❌ VIN - N/A");
                        }
                    }
                    ////////////////////////////////////////////////////////////////////////////////////////////////////////
                    /// ДЛЯ GM // Z18XER //////////////////
                    if (command.startsWith("1A97\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 5; i < wordss.length - 1; i++) {
                                if (!wordss[i].startsWith("7E8") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("7E8", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullReadiness = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ ECU: " + fullReadiness + "\n");
                        } else {
                            addLog("❌ ECU - N/A");
                        }
                    }
                    /// ДЛЯ GM // Z18XER //////////////////
                    if (command.startsWith("1A71\r")) {
                        if (responseStr.startsWith("7E8")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 5; i < wordss.length - 1; i++) {
                                if (!wordss[i].startsWith("7E8") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("7E8", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullReadiness = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ ECU: " + fullReadiness + "\n");
                        } else {
                            addLog("❌ ECU - N/A");
                        }
                    }
                    /// ДЛЯ GM // odometer //////////////////
                    if (command.startsWith("AA041A\r")) {
                        if (responseStr.startsWith("5E8")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 0; i < wordss.length - 1; i++) {
                                if (!wordss[i].startsWith("5E8") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("5E8", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullReadiness = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            // Предположим, wordss содержит HEX значения как строки
                            int km = (Integer.parseInt(wordss[2], 16) << 24) |
                                    (Integer.parseInt(wordss[3], 16) << 16) |
                                    (Integer.parseInt(wordss[4], 16) << 8) |
                                    Integer.parseInt(wordss[5], 16);
                            addLog("✅ ODO: " + km  + " Km.\n");
                        } else {
                            addLog("❌ ODO - N/A");
                        }
                    }
/// ///////////////////////////////////////////////////////////////////////////////////////////////////
                    String[] pid;
                    if (command.startsWith("0100\r")) {
                        if (responseStr.startsWith("SEARCHING...\r7E8")) {
                            btnIdenECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.YELLOW));
                            btnIdenECU.setTextColor(Color.WHITE);
                            btnDataECU.setTextColor(Color.WHITE);
                            btnReadDTC.setTextColor(Color.WHITE);
                            btnClearDTC.setTextColor(Color.WHITE);
                            filterResponse(responseStr);
                            pid = responseStr.split(" ");

                            if (!Objects.equals(pid[2], "41")) {
                                addLog("❌ ECU No Connect 1");
                                btnIdenECU.setTextColor(Color.GRAY);
                                btnDataECU.setTextColor(Color.GRAY);
                                btnReadDTC.setTextColor(Color.GRAY);
                                btnClearDTC.setTextColor(Color.GRAY);
                                btnIdenECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                                return;
                            }
                            addLog("✅ ECU Connect OK.");
                            // Вызов функции для анализа поддерживаемых PID
                            if (pid.length >= 8) {
                                analyzeSupportedPIDs(pid[4], pid[5], pid[6], pid[7]);
                            } else {
                                addLog("❌ Недостаточно данных для анализа PID");
                            }
                        } else if (responseStr.startsWith("7E8")) {
                            pid = responseStr.split(" ");
                            if (!Objects.equals(pid[2], "41")) {
                                addLog("❌ ECU No Connect");
                                btnIdenECU.setTextColor(Color.GRAY);
                                btnDataECU.setTextColor(Color.GRAY);
                                btnReadDTC.setTextColor(Color.GRAY);
                                btnClearDTC.setTextColor(Color.GRAY);
                                btnIdenECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                                return;
                            }
                            addLog("✅ ECU Connect OK.");
                            String[] wordss = responseStr.split(" ");
                            addLog("✅ ECU address/CAN id: " + wordss[0] + "\n");
                            btnIdenECU.setTextColor(Color.WHITE);
                            btnDataECU.setTextColor(Color.WHITE);
                            btnReadDTC.setTextColor(Color.WHITE);
                            btnClearDTC.setTextColor(Color.WHITE);
                            filterResponse(responseStr);
                            //receivedDataTextView.setText(responseStr);
                            //pid = responseStr.split(" ");
                            //receivedDataTextView.setText(pid[0]);
                        } else {
                            addLog("❌ ECU No Connect 2");
                            btnIdenECU.setTextColor(Color.GRAY);
                            btnDataECU.setTextColor(Color.GRAY);
                            btnReadDTC.setTextColor(Color.GRAY);
                            btnClearDTC.setTextColor(Color.GRAY);
                            btnIdenECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                            return;
                        }

                    }

                    if (command.startsWith("ATDP\r")) {
                        if (responseStr.startsWith("AU")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 1; i < wordss.length; i++) {
                                if (!wordss[i].startsWith("7E8") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("7E8", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullVin = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ Protocol: " + fullVin);
                        } else {
                            addLog("❌ Protocol - N/A  - 0902");
                        }
                    }

                    if (callback != null) {
                        try {
                            callback.onResponse(responseStr);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });


            } catch (IOException | InterruptedException e) {
                // Обработка ошибок также через handler
                handler.post(() -> showToast("❌ Ошибка: " + e.getMessage()));
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
            supportedPids.append("✅ Поддерживаемые PID:\n");

            // Байт 1: PIDs 01-08
            supportedPids.append("✅ Байт 1 (").append(byte1Hex).append(") - PIDs 01-08:\n");
            analyzeBytePIDs(binaryString1, 1, supportedPids);

            // Байт 2: PIDs 09-16
            supportedPids.append("\n✅ Байт 2 (").append(byte2Hex).append(") - PIDs 09-16:\n");
            analyzeBytePIDs(binaryString2, 9, supportedPids);

            // Байт 3: PIDs 17-1F (17-31)
            supportedPids.append("\n✅ Байт 3 (").append(byte3Hex).append(") - PIDs 17-1F:\n");
            analyzeBytePIDs(binaryString3, 17, supportedPids);

            // Байт 4: PIDs 20-27
            supportedPids.append("\n✅ Байт 4 (").append(byte4Hex).append(") - PIDs 20-27:\n");
            analyzeBytePIDs(binaryString4, 20, supportedPids);

            // Выводим результат
            addLog(supportedPids.toString());

        } catch (NumberFormatException e) {
            addLog("❌ Ошибка преобразования HEX данных: " + byte1Hex + " " + byte2Hex + " " + byte3Hex + " " + byte4Hex);
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
                result.append("\uD83D\uDD39 PID ").append(String.format("%02X", currentPid))
                        .append(" (").append(currentPid).append(") - ")
                        .append(getPidDescription(currentPid)).append("\n");
            }
        }
    }

    // Функция для получения описания PID
    private String getPidDescription(int pid) {
        switch (pid) {
            case 1:
                return "Мониторинг статуса";
            case 2:
                return "Freeze DTC (замороженные коды ошибок)";
            case 3:
                return "Топливная система";
            case 4:
                return "Расчётная нагрузка";
            case 5:
                return "Температура охлаждающей жидкости";
            case 6:
                return "Краткосрочная коррекция топлива";
            case 7:
                return "Долгосрочная коррекция топлива";
            case 8:
                return "Положение дроссельной заслонки";
            case 9:
                return "Давление топлива";
            case 10:
                return "Абсолютное давление в коллекторе";
            case 11:
                return "Обороты двигателя (RPM)";
            case 12:
                return "Скорость автомобиля";
            case 13:
                return "Угол опережения зажигания";
            case 14:
                return "Температура впускного воздуха";
            case 15:
                return "Расход воздуха";
            case 16:
                return "Положение педали газа";
            case 17:
                return "Напряжение кислородного датчика (банк 1)";
            case 18:
                return "Напряжение кислородного датчика (банк 2)";
            case 19:
                return "Напряжение кислородного датчика (банк 3)";
            case 20:
                return "Напряжение кислородного датчика (банк 4)";
            case 21:
                return "Пробег с последнего сброса ошибок";
            case 22:
                return "Давление в топливной рампе";
            case 23:
                return "Давление в топливной системе";
            case 24:
                return "Соотношение воздух/топливо (банк 1)";
            case 25:
                return "Соотношение воздух/топливо (банк 2)";
            case 26:
                return "Соотношение воздух/топливо (банк 3)";
            case 27:
                return "Соотношение воздух/топливо (банк 4)";
            case 28:
                return "Температура катализатора";
            case 29:
                return "Температура катализатора (банк 2)";
            case 30:
                return "Время работы двигателя";
            case 31:
                return "Дистанция с последнего сброса ошибок";
            default:
                return "Неизвестный параметр";
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
                //runOnUiThread(() -> receivedDataTextView.setText(receivedMessage)); // Обновление UI
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














