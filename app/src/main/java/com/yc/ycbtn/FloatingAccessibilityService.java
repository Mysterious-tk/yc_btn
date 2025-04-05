package com.yc.ycbtn;


import android.hardware.input.InputManager;
import android.os.Handler;  // 添加这行
import android.os.Looper;    // 添加这行
import android.os.SystemClock;
import android.util.Log;
import android.accessibilityservice.AccessibilityService;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;  // 添加这行
import android.os.Build;
import android.view.InputEvent;
import android.view.InputDevice;
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
    // 指针属性生成工具方法
    private static MotionEvent.PointerProperties createPointerProp(int id) {
        MotionEvent.PointerProperties prop = new MotionEvent.PointerProperties();
        prop.id = id;
        return prop;
    }

    // 坐标数据生成工具方法
    private static MotionEvent.PointerCoords createPointerCoord(float x, float y) {
        MotionEvent.PointerCoords coord = new MotionEvent.PointerCoords();
        coord.x = x;
        coord.y = y;
        coord.pressure = 1.0f;
        coord.size = 1.0f;
        return coord;
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
                performSwipeGesture(800, 510, 600, 500); // 从右向左滑动
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                Log.d("Accessibility", "执行向右滑动手势");
                performSwipeGesture(600, 510, 800, 500); // 从左向右滑动
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
            default:
                Log.w("Accessibility", "收到未知键码 keyCode=" + keyCode);
        }
    }

    // 新增通用滑动手势方法
    private void performSwipeGesture(int startX, int startY, int endX, int endY) {
        final long downTime = SystemClock.uptimeMillis();
        
        // 创建更详细的按下事件
        MotionEvent downEvent = MotionEvent.obtain(
                downTime, // downTime (从日志的downTime=257307)
                downTime, // eventTime (从日志的eventTime=257422)
                MotionEvent.ACTION_DOWN,
                1, // pointerCount (单指触摸)
                new MotionEvent.PointerProperties[] { createPointerProp(0) }, // 指针属性
                new MotionEvent.PointerCoords[] { createPointerCoord(startX, startY) }, // 坐标数据
                0, // metaState
                0, // buttonState
                1.0f, // xPrecision
                1.0f, // yPrecision
                1, // deviceId
                0, // edgeFlags
                InputDevice.SOURCE_TOUCHSCREEN | InputDevice.SOURCE_STYLUS,// 组合来源 (日志source=0x5002),
                0 // deviceId (来自日志deviceId=1)
                
        // edgeFlags
        );
        
        
        // 创建更详细的抬起事件
        MotionEvent upEvent = MotionEvent.obtain(
                downTime,        // downTime 
                downTime + 100,  // eventTime 
                MotionEvent.ACTION_UP,
                1, // pointerCount (单指触摸)
                new MotionEvent.PointerProperties[] { createPointerProp(0) }, // 指针属性
                new MotionEvent.PointerCoords[] { createPointerCoord(endX, endY) }, // 坐标数据
                0, // metaState
                0, // buttonState
                1.0f, // xPrecision
                1.0f, // yPrecision
                1, // deviceId
                0, // edgeFlags
                InputDevice.SOURCE_TOUCHSCREEN | InputDevice.SOURCE_STYLUS, // 组合来源
                0// flag (来自日志deviceId=1)
        );
        
        // 计算移动步数和间隔
        int steps = 5; // 移动事件数量
        long moveInterval = 20; // 移动事件间隔(ms)
        float stepX = (endX - startX) / (float)steps;
        float stepY = (endY - startY) / (float)steps;

        // 创建移动事件数组
        MotionEvent[] moveEvents = new MotionEvent[steps];
        for (int i = 1; i <= steps; i++) {
            float currentX = startX + stepX * i;
            float currentY = startY + stepY * i;
            
            moveEvents[i-1] = MotionEvent.obtain(
                downTime, 
                downTime + moveInterval * i, // 时间递增
                MotionEvent.ACTION_MOVE,
                1,
                new MotionEvent.PointerProperties[] { createPointerProp(0) },
                new MotionEvent.PointerCoords[] { createPointerCoord(currentX, currentY) },
                0, 0, 1.0f, 1.0f, 1, 0,
                InputDevice.SOURCE_TOUCHSCREEN | InputDevice.SOURCE_STYLUS,
                0
            );
        }
        try {
            Class<?> inputManagerClass = Class.forName("android.hardware.input.InputManager");
            Method getInstanceMethod = inputManagerClass.getDeclaredMethod("getInstance");
            InputManager im = (InputManager) getInstanceMethod.invoke(null);
            
            Method injectMethod = inputManagerClass.getMethod(
                    "injectInputEvent", InputEvent.class, int.class);
            
            injectMethod.invoke(im, downEvent, 0);
            // 注入所有移动事件
            for (MotionEvent moveEvent : moveEvents) {
                injectMethod.invoke(im, moveEvent, 0);
            }
            injectMethod.invoke(im, upEvent, 0);
        } catch (Exception e) {
            Log.e("Accessibility", "反射调用失败", e);
        } finally {
            downEvent.recycle();
            // 回收所有移动事件
            for (MotionEvent moveEvent : moveEvents) {
                if (moveEvent != null) {
                    moveEvent.recycle();
                }
            }
            upEvent.recycle();
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