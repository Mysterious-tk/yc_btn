package com.yc.ycbtn;

import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class AdbCommandExecutor {
    private static final String TAG = "AdbCommandExecutor";
    private static final String ADB_HOST = "127.0.0.1"; // 本地ADB服务
    private static final int ADB_PORT = 5555; // ADB默认端口

    public static void sendKeyEvent(int keyCode) {
        new Thread(() -> {
            try {
                Socket socket = new Socket(ADB_HOST, ADB_PORT);
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                String command = "input keyevent " + keyCode + "\n";
                dos.writeBytes(command);
                dos.flush();
                socket.close();
                Log.d(TAG, "ADB命令执行成功: " + command);
            } catch (IOException e) {
                Log.e(TAG, "ADB命令执行失败", e);
            }
        }).start();
    }
}