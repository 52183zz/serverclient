package com.example.client;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private String SERVER_IP;
    private int SERVER_PORT;
    private Button btnConnect, btnClick;
    private TextView textView;
    private EditText etMessage, etServerIp, etServerPort, etUserName;
    private ExecutorService executor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Socket socket;
    private BufferedReader serverReader;
    private boolean isConnected = false;
    private PrintWriter writer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        executor = Executors.newSingleThreadExecutor();

        // 初始化 UI 组件
        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(this);
        btnClick = findViewById(R.id.btnClick);
        btnClick.setOnClickListener(this);

        etMessage = findViewById(R.id.etMessage);
        etServerIp = findViewById(R.id.etServerIp);
        etServerPort = findViewById(R.id.etServerPort);
        etUserName = findViewById(R.id.etUserName);

        textView = findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());

        updateConnectionStatus();
    }

    private void updateConnectionStatus() {
        if (isConnected) {
            btnConnect.setText("断开");
            btnClick.setEnabled(true);
        } else {
            btnConnect.setText("连接");
            btnClick.setEnabled(false);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnConnect) {
            toggleConnection();
        } else if (view.getId() == R.id.btnClick && isConnected) {
            String message = etMessage.getText().toString();
            String userName = etUserName.getText().toString();
            if (!message.isEmpty()) {
                if(!userName.isEmpty()){
                    sendToServer(userName + ":" + message);
                    etMessage.setText("");
                }else{
                    handler.post(()-> textView.append("\n请填写用户名"));
                }
            }else{
                handler.post(()-> textView.append("\n请输入信息内容"));
            }
        }
    }

    private void toggleConnection() {
        if (isConnected) {
            disconnectFromServer();
        } else {
            connectToServer();
        }
    }

    private void connectToServer() {
        executor.submit(() -> {
            try {
                SERVER_IP = etServerIp.getText().toString();
                String serverPortStr = etServerPort.getText().toString();

                if (SERVER_IP.isEmpty() || serverPortStr.isEmpty()) {
                    handler.post(() -> textView.append("请填写完整的服务器信息"));
                    return;
                }

                SERVER_PORT = Integer.parseInt(serverPortStr);
                if (SERVER_PORT < 0 || SERVER_PORT > 65535) {
                    throw new NumberFormatException("端口号必须在0到65535之间");
                }

                socket = new Socket(SERVER_IP, SERVER_PORT);
                serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

                // 开始监听服务器消息
                listenForMessages();

                isConnected = true;
                handler.post(this::updateConnectionStatus);
                handler.post(() -> textView.append("\n已连接到服务器"));

            } catch (Exception e) {
                Log.e(TAG, "连接服务器失败", e);
                handler.post(() -> textView.append("\n连接服务器失败，请检查配置并重试。"));
            }
        });
    }

    private void listenForMessages() {
        new Thread(() -> {
            String message;
            try {
                while ((message = serverReader.readLine()) != null) {
                    String finalMessage = message;
                    handler.post(() -> {
                        textView.append("\n" + finalMessage);
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "读取服务器消息失败", e);
            }
        }).start();
    }

    private void sendToServer(String message) {
        executor.submit(()->{
            if (writer != null && isConnected) {
                try {
                    writer.println(message);
                } catch (Exception e) {
                    Log.e(TAG, "发送消息到服务器失败", e);
                    handler.post(() -> textView.append("\n发送消息到服务器失败，请重试。"));
                }
            }
        });
    }

    private void disconnectFromServer() {
        executor.submit(() -> {
            if (socket != null) {
                try {
                    socket.close();
                    isConnected = false;
                    handler.post(this::updateConnectionStatus);

                    if (writer != null) {
                        writer.close();
                        writer = null;
                    }

                    handler.post(() -> textView.append("\n已断开连接"));
                } catch (IOException e) {
                    Log.e(TAG, "关闭Socket时出错", e);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromServer();
        executor.shutdown();
    }
}