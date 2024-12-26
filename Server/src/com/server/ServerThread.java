package com.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

public class ServerThread implements Runnable {
	private final Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final ServerThread thisRef;

    public ServerThread(Socket socket) {
        this.socket = socket;
        this.thisRef = this;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            String str;
            while ((str = reader.readLine()) != null) {
                System.out.println("服务器接收到：" + str);
                Server.broadcastMessage(str); // 广播消息给所有客户端
            }
        }catch (SocketException e) {
            if ("Connection reset".equals(e.getMessage())) {
                System.out.println("客户端已断开连接");
            } else {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (!socket.isClosed()) {
                    socket.close();
                }
                Server.clients.remove(thisRef);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) throws IOException {
        synchronized (writer) {
            writer.write(message + "\n");
            writer.flush();
        }
    }
}