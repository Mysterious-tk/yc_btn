package com.yc.ycbtn;


import android.graphics.PixelFormat;
import android.hardware.input.InputManager;
import android.os.Handler;  // 添加这行
import android.os.Looper;    // 添加这行
import android.os.SystemClock;
import android.util.Log;
import android.accessibilityservice.AccessibilityService;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.Gravity;
import android.util.DisplayMetrics;

public class FloatingAccessibilityService extends AccessibilityService {
    private Handler autoMoveHandler = new Handler();
    private boolean isMoving = false;
    private int currentDirectionX = 0;
    private int currentDirectionY = 0;
    private final long MOVE_INTERVAL = 100; // 移动间隔(ms)
    private static final long HIDE_TIMEOUT = 5000; // 5秒无操作隐藏
    private Runnable hideRunnable = this::hideDot;
    private boolean isActive = true;

    // 在现有成员变量后添加
    private void hideDot() {
        if (isActive) {
            dotView.setVisibility(View.INVISIBLE);
            isActive = false;
        }
    }

    private void showDot() {
        if (!isActive) {
            dotView.setVisibility(View.VISIBLE);
        }
        isActive = true;
        autoMoveHandler.removeCallbacks(hideRunnable);
        autoMoveHandler.postDelayed(hideRunnable, HIDE_TIMEOUT);
    }
    // 创建持续移动的Runnable
    private Runnable autoMoveRunnable = new Runnable() {
        @Override
        public void run() {
            if (isMoving) {
                moveDot(currentDirectionX * MOVE_STEP, currentDirectionY * MOVE_STEP);
                autoMoveHandler.postDelayed(this, MOVE_INTERVAL);
            }
        }
    };
    

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

    // 在类顶部成员变量区添加
    private WindowManager.LayoutParams dotParams;
    private WindowManager.LayoutParams controlParams;
    private WindowManager windowManager;
    private View floatingView;
    private ImageView dotView;
    private int dotX = 500; // 圆点初始X位置
    private int dotY = 500; // 圆点初始Y位置
    private static final int DOT_SIZE = 30; // 圆点大小
    private static final int BUTTON_SIZE = 80; // 按钮大小
    private static final int MOVE_STEP = 20; // 每次移动步长
    
    // 新增圆点视图初始化方法
    // 在圆点初始化方法中明确设置尺寸
    private void initDotView() {
        dotParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // 新增触摸事件穿透标志
            PixelFormat.TRANSLUCENT
        );
        dotParams.gravity = Gravity.TOP | Gravity.LEFT;
        dotParams.width = DOT_SIZE;  // 明确设置固定宽度
        dotParams.height = DOT_SIZE; // 明确设置固定高度
        
        dotView = new ImageView(this);
        dotView.setImageResource(R.drawable.dot_circle);
        dotView.setLayoutParams(new ViewGroup.LayoutParams(
            getResources().getDimensionPixelSize(R.dimen.dot_size),
            getResources().getDimensionPixelSize(R.dimen.dot_size)
        ));
        
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        // 修复圆点初始位置计算（考虑圆点自身尺寸）
        dotX = (metrics.widthPixels - DOT_SIZE) / 2;
        dotY = (metrics.heightPixels - DOT_SIZE) / 2;
        
        windowManager.addView(dotView, dotParams);
        dotView.setX(dotX);
        dotView.setY(dotY);
        dotView.setVisibility(View.VISIBLE);
        dotView.bringToFront();
    }

    // 新增控制面板初始化方法
    private void initControlPanel() {
        controlParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                : WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );
        controlParams.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        controlParams.x = -30; // 确保控制键紧贴屏幕右边
        controlParams.y = 600;
        controlParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        controlParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_controller, null);
        windowManager.addView(floatingView, controlParams);
        
        // 初始化按钮事件监听
        ImageButton upBtn = floatingView.findViewById(R.id.up_btn);
        ImageButton downBtn = floatingView.findViewById(R.id.down_btn);
        ImageButton leftBtn = floatingView.findViewById(R.id.left_btn);
        ImageButton rightBtn = floatingView.findViewById(R.id.right_btn);
        ImageButton confirmBtn = floatingView.findViewById(R.id.confirm_btn);
        
        upBtn.setOnTouchListener((v, event) -> handleTouchEvent(event, 0, -1));
        downBtn.setOnTouchListener((v, event) -> handleTouchEvent(event, 0, 1));
        leftBtn.setOnTouchListener((v, event) -> handleTouchEvent(event, -1, 0));
        rightBtn.setOnTouchListener((v, event) -> handleTouchEvent(event, 1, 0));
        confirmBtn.setOnClickListener(v -> performTapAt(dotX, dotY));
       
        windowManager.updateViewLayout(floatingView, controlParams);
        // 在添加按钮监听后添加面板触摸监听
        floatingView.setOnTouchListener((v, event) -> {
            showDot();
            return false;
        });
    }
    // 新增触摸事件处理方法
    private boolean handleTouchEvent(MotionEvent event, int dirX, int dirY) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isMoving = true;
                currentDirectionX = dirX;
                currentDirectionY = dirY;
                autoMoveHandler.post(autoMoveRunnable); // 立即开始移动
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isMoving = false;
                currentDirectionX = 0;
                currentDirectionY = 0;
                autoMoveHandler.removeCallbacks(autoMoveRunnable); // 停止移动
                return true;
        }
        return false;
    }
    // 在onServiceConnected方法中添加悬浮窗初始化
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        registerReceiver(actionReceiver, new IntentFilter("com.yc.ycbtn.ACTION_PERFORM_GLOBAL_ACTION"));
        
        // 初始化窗口管理器
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        // 分离初始化逻辑
        initControlPanel();  // 控制面板初始化
        initDotView();       // 圆点视图初始化
        
        // 定义 metrics 变量
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        
        // 添加延迟确保视图完成布局
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 添加初始化状态验证
            if (floatingView == null || dotView == null) {
                Log.e("ViewDebug", "视图初始化失败！floatingView:" + floatingView + " dotView:" + dotView);
                return;
            }
            
            // 强制测量布局（添加异常捕获）
            try {
                floatingView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                );
                floatingView.layout(0, 0, floatingView.getMeasuredWidth(), floatingView.getMeasuredHeight());
            } catch (Exception e) {
                Log.e("ViewDebug", "视图测量异常", e);
            }
            
            // 添加圆点视图验证
            if (dotView.getParent() == null) {
                Log.w("ViewDebug", "圆点视图未附加到窗口");
                windowManager.addView(dotView, dotParams); // 需要定义dotParams为类成员变量
            }
            

            // 重新设置圆点位置（修复坐标转换问题）
            dotView.setX(dotX);
            dotView.setY(dotY);
            dotView.bringToFront();
            windowManager.updateViewLayout(dotView, dotParams);
            // 添加调试日志
            Log.d("ViewDebug", "悬浮窗尺寸: " + floatingView.getWidth() + "x" + floatingView.getHeight());
            Log.d("ViewDebug", "圆点坐标: " + dotView.getX() + "," + dotView.getY());
            
            // 修改测量方式为屏幕尺寸
            floatingView.measure(
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(metrics.heightPixels, View.MeasureSpec.EXACTLY)
            );
            
            // 使用绝对坐标系设置位置（替代原相对坐标计算）
            dotView.setX(dotX);
            dotView.setY(dotY);

            // 添加边界验证日志
            Log.d("ViewDebug", "悬浮窗位置 L:" + floatingView.getLeft() 
            + " T:" + floatingView.getTop() 
            + " R:" + floatingView.getRight() 
            + " B:" + floatingView.getBottom());
            
            dotView.layout(dotX, dotY, dotX + DOT_SIZE, dotY + DOT_SIZE);
            windowManager.updateViewLayout(dotView, dotParams);
            windowManager.updateViewLayout(floatingView, controlParams);
            dotView.setVisibility(View.VISIBLE);
        }, 300); // 补全postDelayed的括号
    }  // 修正方法闭合括号位置
    
    // 移动圆点的方法
    // 在moveDot方法中添加同步校验
    private void moveDot(int dx, int dy) {
        // 获取屏幕尺寸
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        showDot(); // 移动时保持可见
        
        // 计算新位置
        int newX = Math.max(0, Math.min(metrics.widthPixels - DOT_SIZE, dotX + dx));
        int newY = Math.max(0, Math.min(metrics.heightPixels - DOT_SIZE, dotY + dy));
        
        dotX = newX;
        dotY = newY;
        
        // 更新圆点位置参数
        // 替换坐标设置方式
        dotParams.x = dotX;
        dotParams.y = dotY;
        
        try {
            // 同时更新两个视图
            windowManager.updateViewLayout(dotView, dotParams);
            windowManager.updateViewLayout(floatingView, controlParams);  // 修正参数名称
            
            // 添加布局更新日志
            Log.d("ViewLayout", "圆点布局参数更新 [X:" + dotParams.x 
                + " Y:" + dotParams.y 
                + " W:" + dotParams.width 
                + " H:" + dotParams.height + "]");
                
            Log.d("ViewLayout", "控制面板参数更新 [X:" + controlParams.x 
                + " Y:" + controlParams.y 
                + " W:" + controlParams.width 
                + " H:" + controlParams.height + "]");

            windowManager.updateViewLayout(dotView, dotParams);
            windowManager.updateViewLayout(floatingView, controlParams);
            
            // 添加调试日志
            Log.d("ViewPosition", "圆点位置 X:" + dotX + " Y:" + dotY);
        } catch (IllegalArgumentException e) {
            Log.e("ViewUpdate", "视图未附加到窗口", e);
        }
        
        // 添加调试日志
        Log.d("ViewPosition", "圆点位置 X:" + dotX + " Y:" + dotY);
    
    // 添加校验日志
    Log.d("PositionSync", "参数坐标 X:" + dotParams.x + " 实际坐标 X:" + dotView.getX());
    Log.d("PositionSync", "参数坐标 Y:" + dotParams.y + " 实际坐标 Y:" + dotView.getY());
    }
    
    // 在指定位置执行点击的方法
    private void performTapAt(int x, int y) {
        final long downTime = SystemClock.uptimeMillis();
        
        // 创建按下事件
        MotionEvent downEvent = MotionEvent.obtain(
            downTime, downTime,
            MotionEvent.ACTION_DOWN,
            1,
            new MotionEvent.PointerProperties[] { createPointerProp(0) },
            new MotionEvent.PointerCoords[] { createPointerCoord(x, y) },
            0, 0, 1.0f, 1.0f, 1, 0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        );
        
        // 创建抬起事件
        MotionEvent upEvent = MotionEvent.obtain(
            downTime, downTime + 100,
            MotionEvent.ACTION_UP,
            1,
            new MotionEvent.PointerProperties[] { createPointerProp(0) },
            new MotionEvent.PointerCoords[] { createPointerCoord(x, y) },
            0, 0, 1.0f, 1.0f, 1, 0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0
        );
        
        try {
            Class<?> inputManagerClass = Class.forName("android.hardware.input.InputManager");
            Method getInstanceMethod = inputManagerClass.getDeclaredMethod("getInstance");
            InputManager im = (InputManager) getInstanceMethod.invoke(null);
            
            Method injectMethod = inputManagerClass.getMethod(
                "injectInputEvent", InputEvent.class, int.class);
            
            injectMethod.invoke(im, downEvent, 0);

            injectMethod.invoke(im, upEvent, 0);
        } catch (Exception e) {
            Log.e("Accessibility", "发送点击事件失败", e);
        } finally {
            downEvent.recycle();
            upEvent.recycle();
        }
    }

    // 在onDestroy中移除悬浮窗
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(actionReceiver);
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
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
