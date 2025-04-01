package com.yc.ycbtn;

import android.app.Service;
import android.content.Intent;
// 新增导入声明
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
// 删除这行
// import android.app.Instrumentation;
import android.provider.Settings;  // 添加这行
import android.app.AlertDialog;

import com.yc.ycbtn.R;
import android.util.Log;

public class FloatingButtonService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    // 添加以下4个成员变量
    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        // 获取 WindowManager 服务
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 加载悬浮窗布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_buttons, null);

        // 设置悬浮窗参数
        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        // 修改悬浮窗参数设置部分
        // 设置悬浮窗参数
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |  // 添加这行
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,   // 添加这行
                PixelFormat.TRANSLUCENT
        );
        
        // 设置位置为右侧垂直居中偏下1/2
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.x = 0;
        params.y = metrics.heightPixels * 3 / 4;  // 向下偏移3/4屏幕高度

        
        // 删除下面这行重复的gravity设置
        // params.gravity = Gravity.TOP | Gravity.START;

        // 将悬浮窗添加到 WindowManager
        windowManager.addView(floatingView, params);

        // 初始化返回按钮
        Button backButton = floatingView.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("FloatingButton", "返回按钮被点击");
                sendKeyEvent(KeyEvent.KEYCODE_BACK);
            }
        });

        // 初始化 Home 按钮
        Button homeButton = floatingView.findViewById(R.id.home_button);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("FloatingButton", "主页按钮被点击");
                sendKeyEvent(KeyEvent.KEYCODE_HOME);
            }
        });

        // 新增的按钮初始化代码
        Button okButton = floatingView.findViewById(R.id.ok_button);
        okButton.setOnClickListener(v -> {
            Log.d("FloatingButton", "确认按钮被点击");
            sendKeyEvent(KeyEvent.KEYCODE_ENTER);
        });

        Button upButton = floatingView.findViewById(R.id.up_button);
        upButton.setOnClickListener(v -> {
            Log.d("FloatingButton", "上方向按钮被点击");
            sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP);
        });

        Button downButton = floatingView.findViewById(R.id.down_button);
        downButton.setOnClickListener(v -> {
            Log.d("FloatingButton", "下方向按钮被点击");
            sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN);
        });

        Button leftButton = floatingView.findViewById(R.id.left_button);
        leftButton.setOnClickListener(v -> {
            Log.d("FloatingButton", "左方向按钮被点击");
            sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT);  // 修正为正确方向键码
        });

        Button rightButton = floatingView.findViewById(R.id.right_button);
        rightButton.setOnClickListener(v -> {
            Log.d("FloatingButton", "右方向按钮被点击");
            sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT);  // 修正为正确方向键码
        });

        Button pageUpButton = floatingView.findViewById(R.id.page_up_button);
        pageUpButton.setOnClickListener(v -> {
            Log.d("FloatingButton", "PageUp按钮被点击");
            sendKeyEvent(KeyEvent.KEYCODE_PAGE_UP); // 键码 92
        });

        Button pageDownButton = floatingView.findViewById(R.id.page_down_button);
        pageDownButton.setOnClickListener(v -> {
            Log.d("FloatingButton", "PageDown按钮被点击");
            sendKeyEvent(KeyEvent.KEYCODE_PAGE_DOWN); // 键码 93
        });

        // 处理悬浮窗的拖动事件
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int)(event.getRawX() - initialTouchX);
                        params.y = initialY + (int)(event.getRawY() - initialTouchY);
                        // 确保按钮不会移出屏幕
                        params.x = Math.max(0, Math.min(params.x, metrics.widthPixels - v.getWidth()));
                        params.y = Math.max(0, Math.min(params.y, metrics.heightPixels - v.getHeight()));
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
        registerReceiver(keyReceiver, new IntentFilter("com.yc.ycbtn.ACTION_HANDLE_KEY"));
    }

    // 删除或注释掉AndroidManifest.xml中的以下行
    // <uses-permission android:name="android.permission.INJECT_EVENTS" />
    
    private void sendKeyEvent(int keyCode) {
        Log.d("FloatingButton", "尝试发送按键事件 keyCode=" + keyCode);
        if (isAccessibilityServiceEnabled()) {
            try {
                Log.d("FloatingButton", "准备启动无障碍服务 keyCode=" + keyCode);
                Intent intent = new Intent(this, FloatingAccessibilityService.class);
                intent.putExtra("action_key", keyCode);
                startService(intent);
            } catch (Exception e) {
                Log.e("FloatingButton", "发送按键事件失败 keyCode=" + keyCode, e);
            }
        } else {
            Log.w("FloatingButton", "无障碍服务未启用，无法发送按键事件");
            showAccessibilityServiceAlert();
        }
    }

    private BroadcastReceiver keyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.yc.ycbtn.ACTION_HANDLE_KEY".equals(intent.getAction())) {
                boolean hasMarker = intent.hasExtra("from_broadcast");
                int keyCode = intent.getIntExtra("key_code", -1);
                Log.d("FloatingButton", "收到广播事件 keyCode=" + keyCode 
                    + " hasMarker=" + hasMarker);
                
                if (!hasMarker) {
                    Log.d("FloatingButton", "处理广播事件 keyCode=" + keyCode);
                    sendKeyEvent(keyCode);
                } else {
                    Log.d("FloatingButton", "忽略已标记的广播事件");
                }
            }
        }
    };
    
    // 简化状态检查方法
    private boolean isKeyEventProcessed(int keyCode) {
        // 由于移除了备用方案，始终返回true
        return true; 
    }

    // 新增引导弹窗方法
    private void showAccessibilityServiceAlert() {
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("需要无障碍权限")
            .setMessage("请前往设置开启悬浮按钮的无障碍服务")
            .setPositiveButton("去开启", (d, which) -> {  // 修改参数名dialog为d
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            })
            .setNegativeButton("取消", null)
            .create();
        
        // Add window type parameters for service context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        
        dialog.show();
    }

    // 新增服务状态检查方法
    private boolean isAccessibilityServiceEnabled() {
        String serviceId = getPackageName() + "/" + FloatingAccessibilityService.class.getName();
        try {
            int enabled = Settings.Secure.getInt(
                getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED
            );
            if (enabled != 1) return false;
    
            String services = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            // Android 5需要更宽松的匹配方式
            return services != null && (services.contains(serviceId) || 
                   services.contains(getPackageName() + "/.FloatingAccessibilityService"));
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(keyReceiver); 
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
