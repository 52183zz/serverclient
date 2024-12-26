package com.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 12302;
    private static final int THREAD_POOL_SIZE = 10; // 增加线程池大小以支持更多并发连接
    static CopyOnWriteArrayList<ServerThread> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("服务器启动并监听端口 " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("新客户端已连接: " + socket.getInetAddress());

                ServerThread clientHandler = new ServerThread(socket);
                clients.add(clientHandler);
                executor.submit(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    public static void broadcastMessage(String message) throws IOException {
        for (ServerThread client : clients) {
            client.sendMessage(message);
        }
    }
}