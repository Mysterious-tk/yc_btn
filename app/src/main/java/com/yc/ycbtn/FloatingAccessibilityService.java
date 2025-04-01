package com.yc.ycbtn;


import android.os.Handler;  // 添加这行
import android.os.Looper;    // 添加这行
import android.os.SystemClock;
import android.util.Log;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;  // 添加这行
import android.os.Build;
import android.view.InputEvent;
import java.lang.reflect.Method;
import android.app.Instrumentation;

public class FloatingAccessibilityService extends AccessibilityService {
    // 移除dispatchKeyEvent的重写方法

    private void injectKeyEvent(int keyCode) {
        new Thread(() -> {
            try {
                // 发送按键按下事件
                long downTime = SystemClock.uptimeMillis();
                KeyEvent downEvent = new KeyEvent(downTime, downTime, 
                    KeyEvent.ACTION_DOWN, keyCode, 0);
                Instrumentation inst = new Instrumentation();
                inst.sendKeySync(downEvent);
                
                // 发送按键抬起事件
                long upTime = SystemClock.uptimeMillis();
                KeyEvent upEvent = new KeyEvent(downTime, upTime,
                    KeyEvent.ACTION_UP, keyCode, 0);
                inst.sendKeySync(upEvent);
                
                Log.d("Accessibility", "成功发送按键事件: " + keyCode);
            } catch (Exception e) {
                Log.e("Accessibility", "发送按键事件失败", e);
            }
        }).start();
    }

    private void handleKeyAction(int keyCode, boolean isFromBroadcast) {
        Log.d("Accessibility", "处理按键 keyCode=" + keyCode + " isFromBroadcast=" + isFromBroadcast);
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                Log.d("Accessibility", "执行BACK操作");
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case KeyEvent.KEYCODE_HOME:
                Log.d("Accessibility", "执行HOME操作");
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case KeyEvent.KEYCODE_PAGE_UP:
                Log.d("Accessibility", "执行向上滚动操作");
                injectKeyEvent(keyCode); 
                break;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                Log.d("Accessibility", "执行向下滚动操作");
                injectKeyEvent(keyCode); 
                break;     
            case KeyEvent.KEYCODE_DPAD_LEFT:
                Log.d("Accessibility", "执行向左滑动手势");
                performGlobalAction(AccessibilityService.GESTURE_SWIPE_LEFT);
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.d("Accessibility", "执行向右滑动手势");
                performGlobalAction(AccessibilityService.GESTURE_SWIPE_RIGHT);
                break;       
            case KeyEvent.KEYCODE_ENTER:
                Log.d("Accessibility", "执行上下滑动手势");
                performGlobalAction(AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN);
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                Log.d("Accessibility", "执行上右滑动手势");
                performGlobalAction(AccessibilityService.GESTURE_SWIPE_UP_AND_LEFT);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                Log.d("Accessibility", "执行右左滑动手势");
                performGlobalAction(AccessibilityService.GESTURE_SWIPE_RIGHT_AND_LEFT);
                break;                

                //injectKeyEvent(keyCode);  // 修改原有广播发送逻辑
                //break;
            default:
                Log.w("Accessibility", "收到未知键码 keyCode=" + keyCode);
        }
    }

    private BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.yc.ycbtn.ACTION_PERFORM_GLOBAL_ACTION".equals(intent.getAction())) {
                int action = intent.getIntExtra("action", -1);
                handleKeyAction(action, true); // 标记来自广播
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        registerReceiver(actionReceiver, new IntentFilter("com.yc.ycbtn.ACTION_PERFORM_GLOBAL_ACTION"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(actionReceiver);
    }
    public void onAccessibilityEvent(AccessibilityEvent event) {}
    
    @Override
    public void onInterrupt() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("action_key")) {
            int keyCode = intent.getIntExtra("action_key", -1);
            Log.d("Accessibility", "收到服务请求 keyCode=" + keyCode);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d("Accessibility", "开始延迟处理 keyCode=" + keyCode);
                handleKeyAction(keyCode, false);
            }, 200);
        }
        return START_STICKY;
    }
}