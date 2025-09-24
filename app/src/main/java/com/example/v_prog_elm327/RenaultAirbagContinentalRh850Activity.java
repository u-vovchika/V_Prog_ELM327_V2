package com.example.v_prog_elm327;

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

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class RenaultAirbagContinentalRh850Activity extends AppCompatActivity {

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

    private Button btnIdenECU, btnReadDTC, btnClearDTC, btn_erase_crash, btn_lock, btn_unlock;
    private ListView logListView;
    private TextView tv_Connect;
    private ArrayAdapter<String> logAdapter;
    private ArrayList<String> logMessages = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper());
    ImageView imageBluetooth, imageViewLogo;

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_renault_airbag_continental_rh850);

        logListView = findViewById(R.id.logListView);
        logAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, logMessages);
        logListView.setAdapter(logAdapter);
        btn_lock = findViewById(R.id.btn_lock);
        btn_unlock = findViewById(R.id.btn_unlock);
        btnIdenECU = findViewById(R.id.btnIdenECU);
        btnReadDTC = findViewById(R.id.btnReadDTC);
        btnClearDTC = findViewById(R.id.btnClearDTC);
        btn_erase_crash = findViewById(R.id.btn_erase_crash);
        logListView = findViewById(R.id.logListView);
        tv_Connect = findViewById(R.id.tv_Connect);
        imageBluetooth = findViewById(R.id.imageBluetooth);
        imageViewLogo = findViewById(R.id.imageViewLogo);


        // возрат назад  ///////////////////////////////////////////////////////
        Button buttonBack = findViewById(R.id.button_back_univ);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.clear(); // стираем данные textViewModel
//                editor.apply(); // записываем данные после очистки textViewModel
//                finish();// завершения процесса
                Intent intent = new Intent(RenaultAirbagContinentalRh850Activity.this, RenaultAirbagMenuActivity.class);
                startActivity(intent);
            }
        });


        /// кнопка для перехода в хелп
        imageViewLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // setContentView(R.layout.lada_airbag_takata_spc_help); // Ваш XML-файл
                Intent intent = new Intent(RenaultAirbagContinentalRh850Activity.this, RenaultAirbagContinentalSpcHelp.class);
                startActivity(intent);
            }
        });


        ((ArrayAdapter) logListView.getAdapter()).clear();

        // Получаем Bluetooth адаптер
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        imageBluetooth.setColorFilter(Color.GRAY);

        /// подключение Bluetooth /////////////////////////////
        discoverBluetoothDevices();
        tv_Connect.setOnClickListener(v -> discoverBluetoothDevices());

        tv_Connect.setText("");
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
                ((ArrayAdapter) logListView.getAdapter()).clear();
                addLog("🔍 Read DTC ...");
                readDtc();
            }
        });
        /// кнопка  стирания ошибок
        btnClearDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((ArrayAdapter) logListView.getAdapter()).clear();
                addLog("🧹 Erase DTC ...");
                clearDTC();
            }
        });
        /// /кнопка LOCK
        btn_lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ArrayAdapter) logListView.getAdapter()).clear();
                EcuLock();
            }
        });
        /// /кнопка UNLOCK
        btn_unlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ArrayAdapter) logListView.getAdapter()).clear();
                EcuUnLock();
            }
        });
        /// /кнопка поиск ECU
        btn_erase_crash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((ArrayAdapter) logListView.getAdapter()).clear();
                addLog("🧹 Erase CRASH ...");
                eraseCRASH();
            }
        });
    }


    private void EcuUnLock() {
        sendCommand("1003", resp10C0 -> {  /// начальная идентификация
            Thread.sleep(700);
            sendCommand("2EA01000", response122 -> {  ///
                Thread.sleep(700);

            });
        });
    }

    private void EcuLock() {

        sendCommand("1003", resp10C0 -> {  /// начальная идентификация
            Thread.sleep(700);
            sendCommand("2EA010FF", response122 -> {  ///
                Thread.sleep(700);

            });
        });
    }

    private void eraseCRASH() {


        sendCommand("1003", resp10C0 -> {  /// начальная идентификация
            Thread.sleep(500);
            sendCommand("22A002", response122 -> {  ///
                Thread.sleep(700);
                sendCommand("2EA02A12071969", resp2EA02A12071969 -> {  /// сТИРАЕМ краш
                    Thread.sleep(700);
                    sendCommand("14FFFFFF", resp14FFFFFF -> {  /// Clear DTC
                        //addLog("\uD83D\uDD0D 1_ " + resp14FFFFFF);
                        Thread.sleep(200);
                        sendCommand("14FFFFFF", resp14FFFF -> {  /// Clear DTC
                            //addLog("\uD83D\uDD0D 2_ " + resp14FFFF);
                            Thread.sleep(200);

                        });
                    });
                });
            });
        });
    }


    private void scanECU() {
        //((ArrayAdapter) logListView.getAdapter()).clear();
        addLog("\uD83D\uDEE0 Config the Adapter ...");
        sendCommand("ATH1", responseATH1 -> {  /// начальная идентификация
            //addLog("\uD83D\uDD0D ATH1 Вкл-ть загол-ки: " + responseATH1);
            Thread.sleep(200);
            sendCommand("ATS1", responseATS1 -> {  /// начальная идентификация
                //addLog("\uD83D\uDD0D ATS1 станд-е форм-е: " + responseATS1);
                Thread.sleep(200);
                sendCommand("ATSP6", responseATSP6 -> {  /// Установка протокола CAN 11 бит 500 кбит/сек
                    //addLog("\uD83D\uDD0D ATSP6 CAN 500 кбит/с: " + responseATSP6);
                    Thread.sleep(200);
                    sendCommand("ATSH752", responseATSH752 -> {  /// Установка протокола CAN 11 бит 500 кбит/сек
                        //addLog("\uD83D\uDD0D ATSH752 ID отпр-ля: " + responseATSH752);
                        Thread.sleep(200);
                        //sendCommand("ATCRA772", responseATCRA772 -> {  /// Установка протокола CAN 11 бит 500 кбит/сек
                        sendCommand("ATCRA000", responseATCRA772 -> {  /// Установка протокола CAN 11 бит 500 кбит/сек
                            //addLog("\uD83D\uDD0D ATCRA772 ID ответ: " + responseATCRA772);
                            Thread.sleep(200);
                            sendCommand("ATCF772", responseATCF772 -> {  /// Установка протокола CAN 11 бит 500 кбит/сек
                                //addLog("\uD83D\uDD0D ATCF772 фильтр 772: " + responseATCF772);
                                Thread.sleep(200);
                                sendCommand("ATAR", responseATAR -> {  /// Включить автоматическое распознавание получателя
                                    //addLog("\uD83D\uDD0D ATAR фильтр 772: " + responseATCF772);
                                    Thread.sleep(200);
                                    sendCommand("ATAL", responseATAL -> {  /// Включить автоматическую обработку многокадровой передачи (ISO 15765-2)
                                        //addLog("\uD83D\uDD0D (ISO 15765-2): " + responseATAL);
                                        Thread.sleep(200);
                                        sendCommand("ATCAF1", responseATCAF1 -> {  /// Включить автоматический форматирование длинных ответов
                                            //addLog("\uD83D\uDD0D ATCAF1 длинных ответов:  " + responseATCAF1);
                                            Thread.sleep(200);
                                            sendCommand("ATSTFFFF", responseATST2000 -> {  /// Установить таймаут ожидания ответа.
                                                //addLog("\uD83D\uDD0D ATST90 таймаут 90:  " + responseATST2000);
                                                Thread.sleep(200);
                                                sendCommand("ATFCSD10", responseATFCSD500 -> {  /// Установить таймаут между кадрами ответа.
                                                    //addLog("\uD83D\uDD0D ATFCSD50 таймаут 50:  " + responseATFCSD500);
                                                    addLog("✅ Config the Adapter ... OK.");
                                                    Thread.sleep(200);
                                                    readIden();
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
        });
    }

    private void clearDTC() {
        sendCommand("1003", resp1003 -> {  /// начальная идентификация
            //sendCommand("3E00", resp3E -> {  /// начальная идентификация
            Thread.sleep(300);
            sendCommand("14FFFFFF", resp14FFFFFF -> {  /// Clear DTC
                //addLog("\uD83D\uDD0D 1_ " + resp14FFFFFF);
                Thread.sleep(200);
                sendCommand("14FFFFFF", resp14FFFF -> {  /// Clear DTC
                    //addLog("\uD83D\uDD0D 2_ " + resp14FFFF);
                    Thread.sleep(200);

                });
            });

        });


    }

    private void readDtc() {


        sendCommand("1003", response1003 -> {  /// начальная идентификация
            //sendCommand("3E01", response1003 -> {  /// начальная идентификация
            //addLog("\uD83D\uDD0D Connect ECU ..." + response1003);
            Thread.sleep(300);
            addLog("\uD83D\uDD0D Текущие ошибки");
            sendCommand("19023B", resp190201 -> {  /// Текущие ошибки
                //addLog("\uD83D\uDD0D DTC ..." + resp190201);
                Thread.sleep(500);
            });
        });


    }

    private void continuation() {
        sendCommand("30000000", response30 -> {  ///
            addLog(" " + response30);
            // Продолжаем запрашивать данные

        });
    }

    private void readIden() {
        ((ArrayAdapter) logListView.getAdapter()).clear();
        sendCommand("ATRV", respATRV -> {  ///
            sendCommand("1003", response1003 -> {  ///
//            //addLog("1003:  " + response1003);
                Thread.sleep(300);
                sendCommand("22F187", resp22F187 -> {  ///
                    //addLog("22F187:  " + resp22F187);
                    Thread.sleep(700);
                    sendCommand("22F190", resp22F190 -> {  ///
                        //addLog("22F190:  " + resp22F190);
                        Thread.sleep(700);

                    });
                });
            });

        });

    }

    private void condition() {
        sendCommand("22D100", response22D100 -> {  ///
            // addLog("Condition:  " + response22D100);
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
        tv_Connect.setText("\uD83D\uDEE0 Connect ...");
        tv_Connect.setTextColor(Color.GRAY);

        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                inputStream = bluetoothSocket.getInputStream();

                runOnUiThread(() -> {
                    imageBluetooth.setColorFilter(Color.GREEN);
                    imageBluetooth.setImageResource(R.drawable.outline_bluetooth_connected_24);
                    tv_Connect.setText("\uD83D\uDEE0 Connect");
                    tv_Connect.setTextColor(Color.YELLOW);
                    Toast.makeText(this, "\uD83D\uDEE0 Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
                });

                Thread.sleep(700);


                // Сброс адаптера и отключение эхо
                sendCommand("ATZ\rATE0\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r", respATZATE0 -> {
                    Thread.sleep(1000);
                    sendCommand("ATE0", respATE0 -> {
                        addLog(" Адаптер: " + respATE0);
                        Thread.sleep(200);
                        sendCommand("STI", respSTI2 -> {
                            if (Objects.equals(respSTI2, "?\r\r>")) {
                                addLog(" Адаптер: Not the original");
                            } else {
                                addLog(" Адаптер: Original ");
                            }
                            Thread.sleep(200);
                            scanECU();
                        });
                    });
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    imageBluetooth.setColorFilter(Color.RED);
                    imageBluetooth.setImageResource(R.drawable.outline_bluetooth_disabled_24);
                    tv_Connect.setText("Ошибка подключения");
                    tv_Connect.setTextColor(Color.RED);
                    btnIdenECU.setTextColor(Color.GRAY);
                    btn_unlock.setTextColor(Color.GRAY);
                    btn_lock.setTextColor(Color.GRAY);
                    btnReadDTC.setTextColor(Color.GRAY);
                    btnClearDTC.setTextColor(Color.GRAY);
                    btn_erase_crash.setTextColor(Color.GRAY);
                    btnIdenECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                    Toast.makeText(this, "❌ Ошибка подключения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                Log.e("Bluetooth", "❌ Ошибка подключения", e);
            }
        }).start();
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
        String hexData = clean.replace("772", "");

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
                //outputStream.write((command + "\r").getBytes();
                outputStream.write((command + "\r").getBytes(StandardCharsets.US_ASCII));
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

                    String[] words = responseStr.split("\r");
                    // receivedDataTextView.setText(words[0]);

                    /// ответ информация об адапторе при коннекте с адаптером
                    if (command.startsWith("STI")) {

                        if (responseStr.contains(">")) {
                            tv_Connect.setText("Connect  Adapter");
                            tv_Connect.setTextColor(Color.GREEN);
                            Toast.makeText(this, "✅ Adapter connect !!!", Toast.LENGTH_SHORT).show();
                        } else {
                            tv_Connect.setText("No Connect");
                            tv_Connect.setTextColor(Color.GRAY);
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
                    if (command.equals("ATI")) {
                        //addLog(">> " + command);
                        addLog("✅ Адаптер " + responseStr);
                        tv_Connect.setText(words[0]);
                    }

                    if (command.equals("AT@SN")) {
                        /// ответ серийном номере адаптора
                        if (words[0].equals("?") || words[0].equals("OK")) {
                            addLog("❌ Not the original adapter");
                            tv_Connect.setTextColor(Color.RED); /// не оригинальный адаптер
                        } else {
                            addLog("✅ The original adapter" + words[0]);
                            tv_Connect.setTextColor(Color.GREEN); /// оригинальный адаптер
                        }
                    }

                    /// напряжение адапторе
                    if (command.equals("ATRV")) {
                        addLog("✅ Voltage: " + responseStr);
                    }

                    /// напряжение ECU
                    if (command.equals("01421")) {
                        String[] wordss = responseStr.split(" ");
                        if (Objects.equals(wordss[2], "41")) {
                            String hex = responseStr.split(" ")[4] + responseStr.split(" ")[5];
                            addLog("✅ Voltage: " + String.format("%.2fV", Integer.parseInt(hex, 16) / 1000.0));
                        } else {
                            addLog("❌ Voltage - N/A 0x" + wordss[2]);
                        }
                    }

//                    /// Clear DTC/////////////
//                    if (command.startsWith("14FFFFFF")) {
//                        if (responseStr.startsWith("772")) {
//                            String[] wordss = responseStr.split(" ");
//                            if (Objects.equals(wordss[2], "54")) {
//                                addLog("✅ Erase DTC ... OK.");
//                            } else {
//                                addLog("❌ Erase DTC ... ERROR.  \n⚠\uFE0F " + wordss[1] + wordss[2] + wordss[3] + wordss[4]);
//                            }
//                        } else {
//                            addLog("❌ Erase DTC - N/A");
//                        }
//                    }
                    /// Clear DTC /////////////
                    if (command.startsWith("14FFFFFF")) {
                        if (responseStr.startsWith("772")) {
                            String[] wordss = responseStr.split(" ");
                            if (wordss.length > 3) {
                                if (Objects.equals(wordss[2], "7F")) {
                                    if (Objects.equals(wordss[4], "78")) {
//                                        if (Objects.equals(wordss[8], "54")) {
//                                            addLog("✅ Erase DTC ... OK.");
//                                        }
                                    }
                                }
                                if (Objects.equals(wordss[2], "54")) {
                                    addLog("✅ Erase DTC ... OK.");
                                }
                            }
                        } else {
                            addLog("❌ Erase DTC - N/A");
                        }
                    }


                    /// Read DTC/////////////
                    if (command.startsWith("19023B")) {
                        if (responseStr.startsWith("772")) {
                            String[] wordss = responseStr.split(" ");
                            if (wordss.length > 3) {
                                if (Objects.equals(wordss[2], "7F")) {
                                    if (Objects.equals(wordss[4], "78")) {
                                        if (Objects.equals(wordss[8], "59")) {
                                            addLog("\uD83D\uDD39 " + wordss[11] + wordss[12]);
                                        }
                                        if (Objects.equals(wordss[8], "10")) {
                                            addLog("\uD83D\uDD39 " + wordss[13] + wordss[14] + "-" + wordss[8]);
                                            //continuation();
                                        }
                                    } else {
                                        addLog("❌ DTC - Answer Error    " + wordss[2]);
                                    }
                                } else {
                                    if (Objects.equals(wordss[1], "59")) {
                                        addLog("\uD83D\uDD39 " + wordss[4] + wordss[5]);
                                    }
                                    if (Objects.equals(wordss[1], "10")) {
                                        addLog("\uD83D\uDD39 " + wordss[6] + wordss[7] + "-" + wordss[8]);
                                        //continuation();
                                    }
                                }
                            } else {
                                addLog("✅ DTC ... No Errors.");
                            }
                        } else {
                            addLog("❌ DTC - N/A");
                        }
                    }
                    /// ERASE CRASH /////////////
                    if (command.startsWith("2EA02A12071969")) {
                        if (responseStr.startsWith("772")) {
                            String[] wordss = responseStr.split(" ");
                            //if (wordss.length > 5) {
                            if (Objects.equals(wordss[2], "7F")) {
                                addLog("❌ Erase CRASH Error.    " + wordss[2]);
                            }
                            if (Objects.equals(wordss[2], "6E")) {
                                addLog("✅ Erase CRASH OK. ");
                            }
                        } else {
                            addLog("❌ Erase CRASH - N/A");
                        }
                    }


                    /// LOCK /////////////
                    if (command.startsWith("2EA010FF")) {
                        if (responseStr.startsWith("772")) {
                            String[] wordss = responseStr.split(" ");
                            //if (wordss.length > 5) {
                            if (Objects.equals(wordss[2], "7F")) {
                                addLog("❌ ECU LOCK Error.    " + wordss[2]);
                            }
                            if (Objects.equals(wordss[2], "6E")) {
                                addLog("✅ ECU LOCK OK. ");
                            }
                        } else {
                            addLog("❌ ECU LOCK - N/A");
                        }
                    }
                    /// UNLOCK /////////////
                    if (command.startsWith("2EA01000")) {
                        if (responseStr.startsWith("772")) {
                            String[] wordss = responseStr.split(" ");
                            //if (wordss.length > 5) {
                            if (Objects.equals(wordss[2], "7F")) {
                                addLog("❌ ECU UNLOCK Error.    " + wordss[2]);
                            }
                            if (Objects.equals(wordss[2], "6E")) {
                                addLog("✅ ECU UNLOCK OK. ");
                            }
                        } else {
                            addLog("❌ ECU UNLOCK - N/A");
                        }
                    }
/// /////////////////////////////////////////////////////////////////////////////////////////////////
                    if (command.startsWith("22D100")) {
                        if (responseStr.startsWith("772")) {
                            String[] wordss = responseStr.split(" ");
//                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
//                            for (int i = 6; i < wordss.length; i++) {
//                                if (!wordss[i].startsWith("772") &&
//                                        !wordss[i].startsWith("21") &&
//                                        !wordss[i].startsWith("22") &&
//                                        !wordss[i].startsWith("55")) {
//                                    // Удаляем ВСЕ вхождения "7E8" из строки
//                                    String cleanedString = wordss[i].replace("772", "");
//                                    vinBuilder.append(extractVinFromResponse(cleanedString));
//                                }
//                            }
//                            String fullNum = vinBuilder.toString();
//                            vinBuilder.setLength(0);
                            if (Objects.equals(wordss[5], "A5")) {
                                addLog("✅ Condition:  Normal");
                            } else if (Objects.equals(wordss[5], "5A")) {
                                addLog("\uD83D\uDD01 Condition:  Factory");
                            } else {
                                addLog("❌ Condition - N/A ");
                            }

                        } else {
                            addLog("❌ ECU id - N/A");
                        }
                    }

                    /// идентификация ///////////////////////////////////////////////////////////////////////////////////////
                    if (command.startsWith("22F187")) {
                        if (responseStr.startsWith("772")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 6; i < wordss.length; i++) {
                                if (!wordss[i].startsWith("772") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "772" из строки
                                    String cleanedString = wordss[i].replace("772", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullVin = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ Part Num:  " + fullVin);
                        } else {
                            addLog("❌ Part Num - N/A");
                        }
                    }

                    if (command.startsWith("22F190")) {
                        if (responseStr.startsWith("772")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 6; i < wordss.length; i++) {
                                if (!wordss[i].startsWith("772") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "7E8" из строки
                                    String cleanedString = wordss[i].replace("772", "");
                                    vinBuilder.append(extractVinFromResponse(cleanedString));
                                }
                            }
                            String fullCalib = vinBuilder.toString();
                            vinBuilder.setLength(0);
                            addLog("✅ VIN:  " + fullCalib);
                        } else {
                            addLog("❌ VIN - N/A");
                        }
                    }
/// ///////////////////////////////////////////////////////////////////////////////////////////////////
                    String[] pid;
                    if (command.startsWith("1003")) {
                        if (responseStr.startsWith("772")) {
                            pid = responseStr.split(" ");
                            if (!Objects.equals(pid[2], "50")) {
                                addLog("❌ ECU No Connect");
                                btnIdenECU.setTextColor(Color.GRAY);
                                btn_lock.setTextColor(Color.GRAY);
                                btn_unlock.setTextColor(Color.GRAY);
                                btnReadDTC.setTextColor(Color.GRAY);
                                btnClearDTC.setTextColor(Color.GRAY);
                                btn_erase_crash.setTextColor(Color.GRAY);
                                btnIdenECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                                return;
                            } else if (Objects.equals(pid[2], "50")) {
                                addLog("✅ ECU Connect OK.");
                                String[] wordss = responseStr.split(" ");
                                //addLog("✅ ECU address/CAN id: " + wordss[0] + "\n");
                                btnIdenECU.setTextColor(Color.WHITE);
                                btn_lock.setTextColor(Color.WHITE);
                                btn_unlock.setTextColor(Color.WHITE);
                                btnReadDTC.setTextColor(Color.WHITE);
                                btnClearDTC.setTextColor(Color.WHITE);
                                btn_erase_crash.setTextColor(Color.WHITE);
                            }
                        } else {
                            addLog("❌ ECU No Connect 2");
                            btnIdenECU.setTextColor(Color.GRAY);
                            btn_lock.setTextColor(Color.GRAY);
                            btn_unlock.setTextColor(Color.GRAY);
                            btnReadDTC.setTextColor(Color.GRAY);
                            btnClearDTC.setTextColor(Color.GRAY);
                            btn_erase_crash.setTextColor(Color.GRAY);
                            btnIdenECU.setCompoundDrawableTintList(ColorStateList.valueOf(Color.GRAY));
                            return;
                        }

                    }

                    if (command.startsWith("ATDP\r")) {
                        if (responseStr.startsWith("AU")) {
                            String[] wordss = responseStr.split(" ");
                            StringBuilder sVim = new StringBuilder(Arrays.toString(wordss));
                            for (int i = 1; i < wordss.length; i++) {
                                if (!wordss[i].startsWith("772") &&
                                        !wordss[i].startsWith("21") &&
                                        !wordss[i].startsWith("22") &&
                                        !wordss[i].startsWith("55")) {
                                    // Удаляем ВСЕ вхождения "772" из строки
                                    String cleanedString = wordss[i].replace("772", "");
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