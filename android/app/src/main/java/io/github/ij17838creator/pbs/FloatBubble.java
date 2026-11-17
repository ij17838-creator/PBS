package io.github.ij17838creator.pbs;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * 떠 있는 ＋1 버튼: 다른 앱(수업 자료) 위에 작은 동그라미를 띄워, 누르면 앱 밖 측정과 같은 기록(CounterReceiver.handle)을 한다.
 * - 켜는 조건: 사용자가 켜 둠(bubOn) + '다른 앱 위에 표시' 권한 + 앱 밖 측정 대상이나 열린 회기가 있음.
 * - 끌어서 옮기고, 화면 왼쪽·오른쪽 밖으로 밀면 가장자리에 반쯤만 남는다(누르면 다시 나옴). 위치는 기억한다.
 * - 빈도 ＋1, 지속시간 시작/끝, 지연시간 지시/반응. 강도·간격 기록은 누르면 앱을 연다.
 * 안드로이드가 백그라운드에서 끄지 않도록 작은 알림(포그라운드 서비스)을 같이 띄운다.
 */
public class FloatBubble extends Service {
    static final int NOTI_ID = 1706;
    private static volatile FloatBubble inst;
    private final Handler h = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private TextView v;
    private WindowManager.LayoutParams lp;
    private boolean tucked;

    static boolean on(Context c) { return CounterStore.prefs(c).getBoolean("bubOn", false); }
    static boolean perm(Context c) { return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c); }
    static boolean alive() { return inst != null; }

    static void set(Context c, boolean on) {
        CounterStore.prefs(c).edit().putBoolean("bubOn", on).apply();
        sync(c);
    }

    /** '다른 앱 위에 표시' 설정 화면 열기 */
    static void askPerm(Context c) {
        try {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception ignored) {}
    }

    /** 상태가 바뀔 때마다 부른다(CounterNotifier.refreshAll): 필요하면 띄우고, 아니면 닫는다, 떠 있으면 글자만 고친다 */
    static void sync(Context c) {
        Context a = c.getApplicationContext();
        boolean want = on(a) && perm(a) && CounterStore.anything(a);
        FloatBubble b = inst;
        if (want) {
            if (b != null) b.h.post(b::refresh);
            else {
                try { ContextCompat.startForegroundService(a, new Intent(a, FloatBubble.class)); } catch (Exception ignored) {}
            }
        } else if (b != null) {
            try { a.stopService(new Intent(a, FloatBubble.class)); } catch (Exception ignored) {}
        }
    }

    private Notification note() {
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 61, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        CounterNotifier.ensureChannel(this);
        return new NotificationCompat.Builder(this, CounterNotifier.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pbs)
            .setContentTitle("떠 있는 ＋1 켜짐")
            .setContentText("앱의 앱 밖 측정에서 끌 수 있어요")
            .setOngoing(true).setSilent(true).setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTI_ID, note(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(NOTI_ID, note());
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        inst = this;
        if (!(on(this) && perm(this) && CounterStore.anything(this))) { stopSelf(); return START_NOT_STICKY; }
        if (v == null) show();
        else refresh();
        return START_STICKY;
    }

    private int dp(float x) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, x, getResources().getDisplayMetrics()); }

    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("deprecation")
    private void show() {
        try {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            v = new TextView(this);
            v.setGravity(Gravity.CENTER);
            v.setTextColor(Color.WHITE);
            v.setIncludeFontPadding(false);
            v.setLineSpacing(0, 1.0f);
            v.setElevation(dp(6));
            int size = dp(84);
            lp = new WindowManager.LayoutParams(size, size,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            SharedPreferences p = CounterStore.prefs(this);
            DisplayMetrics m = getResources().getDisplayMetrics();
            lp.x = p.getInt("bubX", m.widthPixels - size - dp(16));
            lp.y = p.getInt("bubY", m.heightPixels / 2);
            tucked = p.getBoolean("bubT", false);
            v.setOnTouchListener(new View.OnTouchListener() {
                float sx, sy; int ox, oy; boolean moved;
                @Override public boolean onTouch(View view, MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            sx = e.getRawX(); sy = e.getRawY(); ox = lp.x; oy = lp.y; moved = false;
                            return true;
                        case MotionEvent.ACTION_MOVE: {
                            float dx = e.getRawX() - sx, dy = e.getRawY() - sy;
                            if (!moved && Math.hypot(dx, dy) < dp(10)) return true;
                            moved = true; tucked = false;
                            lp.x = (int) (ox + dx); lp.y = (int) (oy + dy);
                            try { wm.updateViewLayout(v, lp); } catch (Exception ignored) {}
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                            if (!moved) {
                                if (tucked) { tucked = false; place(true); }
                                else tap();
                            } else {
                                int w = getResources().getDisplayMetrics().widthPixels;
                                int s = lp.width;
                                tucked = lp.x < -s * 0.3 || lp.x + s > w + s * 0.3;
                                place(false);
                            }
                            return true;
                        case MotionEvent.ACTION_CANCEL:
                            place(false);
                            return true;
                    }
                    return false;
                }
            });
            wm.addView(v, lp);
            place(false);
            refresh();
        } catch (Exception e) {
            v = null;
            stopSelf();
        }
    }

    /** 화면 안에 맞추고, 숨김이면 가장자리에 반쯤만 남긴다 */
    private void place(boolean pullOut) {
        if (v == null) return;
        DisplayMetrics m = getResources().getDisplayMetrics();
        int s = lp.width;
        boolean left = lp.x + s / 2 < m.widthPixels / 2;
        if (tucked) lp.x = left ? -s * 2 / 3 : m.widthPixels - s / 3;
        else if (pullOut) lp.x = left ? dp(12) : m.widthPixels - s - dp(12);
        else lp.x = Math.max(0, Math.min(lp.x, m.widthPixels - s));
        lp.y = Math.max(0, Math.min(lp.y, m.heightPixels - s - dp(24)));
        v.setAlpha(tucked ? 0.75f : 1f);
        try { wm.updateViewLayout(v, lp); } catch (Exception ignored) {}
        CounterStore.prefs(this).edit().putInt("bubX", lp.x).putInt("bubY", lp.y).putBoolean("bubT", tucked).apply();
    }

    private void tap() {
        String mode = CounterStore.uiMode(this);
        String act;
        if (CounterStore.FREQ.equals(mode)) act = CounterReceiver.INC;
        else if (CounterStore.DUR.equals(mode)) act = CounterReceiver.DUR;
        else if (CounterStore.LAT.equals(mode)) act = CounterReceiver.LAT;
        else {
            Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try { startActivity(open); } catch (Exception ignored) {}
            return;
        }
        v.animate().scaleX(1.15f).scaleY(1.15f).setDuration(90).withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(120)).start();
        CounterReceiver.handle(getApplicationContext(), act); // 기록·진동·알림/위젯 갱신·refresh까지
    }

    void refresh() {
        if (v == null) return;
        String mode = CounterStore.uiMode(this);
        boolean act = CounterStore.active(this);
        boolean run = CounterStore.running(this);
        String big, small;
        if (CounterStore.FREQ.equals(mode)) { big = act ? "＋1" : "시작"; small = act ? CounterStore.count(this) + "회" : "회기"; }
        else if (CounterStore.DUR.equals(mode)) { big = run ? "끝" : "시작"; small = "행동"; }
        else if (CounterStore.LAT.equals(mode)) { big = run ? "반응" : "지시"; small = "지연"; }
        else { big = "열기"; small = "앱"; }
        v.setText(big + "\n" + small);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(run ? Color.parseColor("#B3402A") : Color.parseColor("#1B6862"));
        bg.setStroke(dp(2), Color.WHITE);
        v.setBackground(bg);
    }

    @Override
    public void onDestroy() {
        if (inst == this) inst = null;
        h.removeCallbacksAndMessages(null);
        if (v != null && wm != null) { try { wm.removeView(v); } catch (Exception ignored) {} }
        v = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
