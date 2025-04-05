package com.yc.ycbtn;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class FloatingButtonService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private boolean isExpanded = false;
    private WindowManager.LayoutParams params;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!canDrawOverlays()) {
            requestOverlayPermission();
        }
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        toggleLayout();
    }

    private boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void toggleLayout() {
        try {
            // 安全移除旧视图
            if (floatingView != null && floatingView.isAttachedToWindow()) {
                try {
                    windowManager.removeView(floatingView);
                } catch (IllegalArgumentException e) {
                    Log.e("FloatingButton", "视图移除异常: " + e.getMessage());
                }
            }

            // 安全加载新布局
            // 修改布局加载方式
            int layoutRes = isExpanded ? R.layout.floating_main : R.layout.floating_mini;
            LayoutInflater inflater = LayoutInflater.from(this).cloneInContext(this);
            inflater.setFactory(new LayoutInflater.Factory() {
                @Override
                public View onCreateView(String name, Context context, AttributeSet attrs) {
                    try {
                        // 修复Xposed环境下的类加载问题
                        if (name.equals("ImageButton") || name.equals("ImageView")) {
                            return new ImageView(context, attrs);
                        }
                        return null;
                    } catch (Exception e) {
                        Log.e("InflateFix", "创建视图失败: " + name, e);
                        return null;
                    }
                }
            });
            floatingView = inflater.inflate(layoutRes, null);
            
            // 初始化参数和监听
            setupLayoutParams();
            try {
                try {
                    windowManager.addView(floatingView, params);
                } catch (WindowManager.BadTokenException e) {
                    Log.e("Window", "请先授予悬浮窗权限");
                    requestOverlayPermission();
                } catch (SecurityException e) {
                    Log.e("Window", "权限不足: " + e.getMessage());
                    requestOverlayPermission();
                }
                setupListeners();
            } catch (WindowManager.BadTokenException | IllegalStateException e) {
                Log.e("FloatingButton", "添加视图失败: " + e.getMessage());
                return;
            }
        } catch (Exception e) {
            Log.e("FloatingButton", "布局切换异常: ", e); // 打印完整堆栈
            stopSelf();
        }
    }

    private void setupLayoutParams() {
        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        
        // 修改重力参数为右侧中间
        params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        
        // 设置初始位置偏移量(可选)
        params.x = 0;  // 右侧不需要水平偏移
        params.y = 0;  // 垂直居中不需要偏移
        
        if (!isExpanded) {
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        }
    }

    private void setupListeners() {
        if (isExpanded) {
            View minimizeBtn = floatingView.findViewById(R.id.btn_minimize);
            minimizeBtn.setOnClickListener(v -> {
                isExpanded = false;
                toggleLayout();
            });
            
            // 确保展开视图中的按钮可点击
            minimizeBtn.setClickable(true);
            minimizeBtn.setFocusable(true);
            
            // 添加所有功能按钮监听
            View pageUpBtn = floatingView.findViewById(R.id.btn_page_up);
            View pageDownBtn = floatingView.findViewById(R.id.btn_page_down);
            View backBtn = floatingView.findViewById(R.id.back_button);
            View homeBtn = floatingView.findViewById(R.id.home_button);
            View leftSwipeBtn = floatingView.findViewById(R.id.left_button);
            View rightSwipeBtn = floatingView.findViewById(R.id.right_button);
            View upDownSwipeBtn = floatingView.findViewById(R.id.ok_button);  // 使用OK按钮作为上下滑动
            View upLeftSwipeBtn = floatingView.findViewById(R.id.up_button);  // 使用↑按钮作为上左滑动
            View rightLeftSwipeBtn = floatingView.findViewById(R.id.down_button); // 使用↓按钮作为右左滑动
            
            // 设置各按钮点击监听
            setButtonClickListener(pageUpBtn, KeyEvent.KEYCODE_PAGE_UP);
            setButtonClickListener(pageDownBtn, KeyEvent.KEYCODE_PAGE_DOWN);
            setButtonClickListener(backBtn, KeyEvent.KEYCODE_BACK);
            setButtonClickListener(homeBtn, KeyEvent.KEYCODE_HOME);
            setButtonClickListener(leftSwipeBtn, KeyEvent.KEYCODE_DPAD_LEFT);
            setButtonClickListener(rightSwipeBtn, KeyEvent.KEYCODE_DPAD_RIGHT);
            setButtonClickListener(upDownSwipeBtn, KeyEvent.KEYCODE_ENTER);
            setButtonClickListener(upLeftSwipeBtn, KeyEvent.KEYCODE_DPAD_UP);
            setButtonClickListener(rightLeftSwipeBtn, KeyEvent.KEYCODE_DPAD_DOWN);
        } else {
            floatingView.findViewById(R.id.btn_expand).setOnClickListener(v -> {
                isExpanded = true;
                toggleLayout();
            });
            floatingView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_UP:
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            // 修改移动逻辑适应右侧位置
                            params.x = initialX - (int)(event.getRawX() - initialTouchX); // 注意这里是减号
                            params.y = initialY + (int)(event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatingView, params);
                            return true;
                    }
                    return false;
                }
            });
        }
    }

    // 新增通用按钮点击监听设置方法
    private void setButtonClickListener(View button, int keyCode) {
        if (button != null) {
            button.setOnClickListener(v -> {
                sendActionBroadcast(keyCode);
                // 添加点击反馈
                v.setPressed(true);
                new Handler().postDelayed(() -> v.setPressed(false), 100);
            });
            button.setClickable(true);
            button.setFocusable(true);
        }
    }

    // 新增广播发送方法
    // 添加这个方法到您的服务类中
    private boolean isAccessibilityServiceEnabled() {
        String serviceName = getPackageName() + "/" + FloatingAccessibilityService.class.getName();
        try {
            int accessibilityEnabled = Settings.Secure.getInt(
                    getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            if (accessibilityEnabled == 1) {
                String enabledServices = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                return enabledServices != null && enabledServices.contains(serviceName);
            }
        } catch (Exception e) {
            Log.e("Accessibility", "检查无障碍服务失败", e);
        }
        return false;
    }

    // 修改sendActionBroadcast方法
    private void sendActionBroadcast(int actionCode) {
        if (!isAccessibilityServiceEnabled()) {
            // 引导用户开启无障碍服务
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show();
            return;
        }
        
        Intent intent = new Intent("com.yc.ycbtn.ACTION_PERFORM_GLOBAL_ACTION");
        intent.putExtra("action", actionCode);
        sendBroadcast(intent);
        Log.d("FloatingButton", "发送广播 action=" + actionCode);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
    }
}
