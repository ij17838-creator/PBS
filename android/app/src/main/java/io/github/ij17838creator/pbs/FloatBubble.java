package io.github.ij17838creator.pbs;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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
import java.util.ArrayList;
import java.util.List;

/**
 * 떠 있는 ＋1 버튼: 다른 앱(수업 자료) 위에 작은 동그라미를 띄워, 누르면 앱 밖 측정과 같은 기록(CounterReceiver.handle)을 한다.
 * - 켜는 조건: 사용자가 켜 둠(bubOn) + '다른 앱 위에 표시' 권한 + 회기가 열려 있음(회기 끝나면 사라짐).
 * - 수업 화면을 덜 가리게: 작게(64dp), 평소엔 반투명, 누를 때만 진하게. 버튼 밖 터치는 아래 앱으로 그대로 간다.
 * - 끌어서 옮기고, 화면 왼쪽·오른쪽 밖으로 밀면 가장자리에 반쯤만 남는다(누르면 다시 나옴). 위치는 기억한다.
 * - 빈도 ＋1, 지속시간 시작/끝, 지연시간 지시/반응. 강도·간격 기록은 누르면 앱을 연다.
 * 트리거 '긋기'(bubTrig=draw): 동그라미 대신 작은 판이 뜨고, 판 안에 아무 획이나 그으면 ＋1(여러 획을 0.7초 안에 이어 그리면 한 번).
 *   판 위쪽 띠를 잡아 옮기거나 밀어 숨긴다. 판 밖의 펜·손가락은 아래 앱(칠판 필기)으로 그대로 간다.
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
    private boolean draw;
    private Pad pad;
    /** 평소엔 반투명(숨김이면 더 흐리게) */
    private final Runnable dim = () -> { if (v != null) v.setAlpha(tucked ? 0.35f : 0.55f); };

    static boolean on(Context c) { return CounterStore.prefs(c).getBoolean("bubOn", false); }
    static boolean perm(Context c) { return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c); }
    static boolean alive() { return inst != null; }
    static String trig(Context c) { return "draw".equals(CounterStore.prefs(c).getString("bubTrig", "tap")) ? "draw" : "tap"; }
    /** 트리거 저장: tap(누르기) / draw(긋기) */
    static void setTrig(Context c, String trig) {
        CounterStore.prefs(c).edit().putString("bubTrig", "draw".equals(trig) ? "draw" : "tap").apply();
        FloatBubble b = inst;
        if (b != null) b.h.post(b::rebuild);
    }

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
        boolean want = on(a) && perm(a) && CounterStore.active(a); // 회기가 열려 있을 때만
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
        if (!(on(this) && perm(this) && CounterStore.active(this))) { stopSelf(); return START_NOT_STICKY; }
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
            draw = "draw".equals(trig(this));
            v = draw ? (pad = new Pad(this)) : new TextView(this);
            v.setGravity(Gravity.CENTER);
            v.setTextColor(Color.WHITE);
            v.setIncludeFontPadding(false);
            v.setLineSpacing(0, 1.0f);
            v.setElevation(dp(6));
            int size = draw ? dp(150) : dp(64);
            lp = new WindowManager.LayoutParams(size, size,
                Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            SharedPreferences p = CounterStore.prefs(this);
            DisplayMetrics m = getResources().getDisplayMetrics();
            lp.x = p.getInt("bubX", m.widthPixels - size - dp(8));
            lp.y = p.getInt("bubY", m.heightPixels / 3);
            tucked = p.getBoolean("bubT", false);
            v.setOnTouchListener(new View.OnTouchListener() {
                float sx, sy; int ox, oy; boolean moved, inking;
                @Override public boolean onTouch(View view, MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            sx = e.getRawX(); sy = e.getRawY(); ox = lp.x; oy = lp.y; moved = false;
                            h.removeCallbacks(dim); v.setAlpha(1f);
                            // 그리기 판: 위쪽 띠가 아닌 곳은 그리기 (숨김 상태면 꺼내기)
                            inking = draw && !tucked && e.getY() > dp(30);
                            if (inking) pad.down(e.getX(), e.getY());
                            return true;
                        case MotionEvent.ACTION_MOVE: {
                            if (inking) { for (int i = 0; i < e.getHistorySize(); i++) pad.move(e.getHistoricalX(i), e.getHistoricalY(i)); pad.move(e.getX(), e.getY()); return true; }
                            float dx = e.getRawX() - sx, dy = e.getRawY() - sy;
                            if (!moved && Math.hypot(dx, dy) < dp(10)) return true;
                            moved = true; tucked = false;
                            lp.x = (int) (ox + dx); lp.y = (int) (oy + dy);
                            try { wm.updateViewLayout(v, lp); } catch (Exception ignored) {}
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                            if (inking) { inking = false; pad.up(); h.removeCallbacks(dim); h.postDelayed(dim, 2500); return true; }
                            if (!moved) {
                                if (draw && !tucked) { place(false); return true; } // 띠를 톡 누른 것은 무시
                                if (tucked) { tucked = false; place(true); }
                                else { tap(); h.removeCallbacks(dim); h.postDelayed(dim, 1500); }
                            } else {
                                int w = getResources().getDisplayMetrics().widthPixels;
                                int s = lp.width;
                                tucked = lp.x < -s * 0.3 || lp.x + s > w + s * 0.3;
                                place(false);
                            }
                            return true;
                        case MotionEvent.ACTION_CANCEL:
                            if (inking) { inking = false; pad.cancel(); }
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
        h.removeCallbacks(dim);
        h.postDelayed(dim, 1500);
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
        if (draw) { pad.label = (CounterStore.FREQ.equals(mode) ? CounterStore.count(this) + "회" : big) + " · 그으면 ＋1"; pad.run = run; pad.invalidate(); return; }
        v.setText(big + "\n" + small);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(run ? Color.parseColor("#B3402A") : Color.parseColor("#1B6862"));
        bg.setStroke(dp(2), Color.WHITE);
        v.setBackground(bg);
    }

    /** 트리거가 바뀌면 화면 위 버튼을 새로 만든다 */
    void rebuild() {
        if (v != null && wm != null) { try { wm.removeView(v); } catch (Exception ignored) {} }
        v = null; pad = null;
        if (on(this) && perm(this) && CounterStore.active(this)) show();
    }

    /** 판에 획을 다 그은 뒤(0.7초 쉬면) ＋1. 너무 짧은 것(톡 닿음·손바닥)은 세지 않는다 */
    private final Runnable judge = () -> {
        if (pad == null) return;
        List<float[]> drawn = pad.take();
        float L = 0;
        for (float[] st : drawn) for (int i = 2; i + 1 < st.length; i += 2) L += (float) Math.hypot(st[i] - st[i - 2], st[i + 1] - st[i - 1]);
        if (L < dp(14)) { pad.flash(0); return; }
        pad.flash(1);
        tap();
    };

    /** 작은 그리기 판 */
    private class Pad extends TextView {
        final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG), box = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        final List<float[]> strokes = new ArrayList<>();
        final List<Float> cur = new ArrayList<>();
        final Path path = new Path();
        String label = "";
        boolean run;
        int fl; // 1 맞음 -1 아님
        Pad(Context c) {
            super(c);
            ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(dp(4)); ink.setStrokeCap(Paint.Cap.ROUND); ink.setStrokeJoin(Paint.Join.ROUND); ink.setColor(Color.parseColor("#1B6862"));
            txt.setColor(Color.WHITE); txt.setTextSize(dp(13)); txt.setFakeBoldText(true);
        }
        void down(float x, float y) { h.removeCallbacks(judge); fl = 0; cur.clear(); cur.add(x); cur.add(y); path.moveTo(x, y); invalidate(); }
        void move(float x, float y) { cur.add(x); cur.add(y); path.lineTo(x, y); invalidate(); }
        void up() {
            float[] f = new float[cur.size()]; for (int i = 0; i < f.length; i++) f[i] = cur.get(i);
            if (f.length >= 2) strokes.add(f);
            cur.clear(); h.removeCallbacks(judge); h.postDelayed(judge, 700);
        }
        void cancel() { cur.clear(); strokes.clear(); path.reset(); invalidate(); }
        List<float[]> take() { List<float[]> r = new ArrayList<>(strokes); strokes.clear(); return r; }
        void flash(int f) { fl = f; path.reset(); invalidate(); h.postDelayed(() -> { fl = 0; invalidate(); }, 600); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), hh = getHeight(), r = dp(14);
            box.setStyle(Paint.Style.FILL);
            box.setColor(fl == 1 ? Color.parseColor("#D2E9E5") : fl == -1 ? Color.parseColor("#F4D6CF") : Color.argb(235, 255, 255, 255));
            c.drawRoundRect(0, 0, w, hh, r, r, box);
            box.setColor(run ? Color.parseColor("#B3402A") : Color.parseColor("#1B6862"));
            c.drawRoundRect(0, 0, w, dp(30), r, r, box);
            c.drawRect(0, dp(16), w, dp(30), box);
            box.setStyle(Paint.Style.STROKE); box.setStrokeWidth(dp(2));
            c.drawRoundRect(dp(1), dp(1), w - dp(1), hh - dp(1), r, r, box);
            c.drawText("≡ " + label, dp(10), dp(20), txt);
            c.drawPath(path, ink);
        }
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
