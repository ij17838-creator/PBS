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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

/**
 * 떠 있는 ＋1 (S펜 에어 커맨드 느낌): 다른 앱(수업 자료) 위에 작고 반투명한 동그라미 아이콘이 옆 가장자리에 반쯤 붙어 있다.
 * - 켜는 조건: 사용자가 켜 둠(bubOn) + '다른 앱 위에 표시' 권한 + 회기가 열려 있거나 앱 밖 측정 대상이 있음.
 * - 대기(열린 회기 없음, 2026-11-24): 아이콘 ○, 판은 그리기 판이고 동그라미를 그렸을 때만 회기 시작(다른 모양은 무시).
 * - 아이콘(44dp): 지금 횟수 표시, 평소 반투명. 끌면 따라오고 놓으면 가까운 옆 가장자리에 반쯤 걸쳐 붙는다. 위치 기억.
 * - 아이콘을 누르면 옆으로 작은 판이 열린다: 누르기(bubTrig=tap)면 큰 ＋1 버튼, 긋기(draw)면 그리기 판(아무 획 = ＋1). −1·닫기.
 *   판 밖을 누르거나 8초 동안 안 쓰면 닫힌다. 판·아이콘 밖의 터치는 아래 앱으로 그대로 간다.
 * - 빈도 ＋1, 지속시간 시작/끝, 지연시간 지시/반응. 강도·간격 기록은 앱을 연다. 기록은 CounterReceiver.handle(위젯·알림창과 같음).
 * 안드로이드가 백그라운드에서 끄지 않도록 작은 알림(포그라운드 서비스)을 같이 띄운다.
 */
public class FloatBubble extends Service {
    static final int NOTI_ID = 1706;
    private static volatile FloatBubble inst;
    private final Handler h = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private TextView v;
    private WindowManager.LayoutParams lp;
    /** 오른쪽 가장자리에 붙어 있는지 */
    private boolean right = true;
    /** 펼친 판(없으면 null)과 그 안의 것들 */
    private LinearLayout panel;
    private WindowManager.LayoutParams plp;
    private Pad pad;
    private TextView hit, head;
    /** 평소엔 반투명 */
    private final Runnable dim = () -> { if (v != null && panel == null) v.setAlpha(0.45f); };

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
        boolean want = on(a) && perm(a) && CounterStore.anything(a); // 회기가 열려 있거나 앱 밖 측정 대상이 있을 때(대기 중엔 동그라미로 회기 시작)
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
            .setContentText(CounterStore.active(this) ? "앱의 앱 밖 측정에서 끌 수 있어요" : "대기 중 · 아이콘 판에 ○를 그리면 회기 시작")
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

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
    }

    /* ---------- 아이콘: 작고 반투명, 놓으면 가까운 옆 가장자리에 반쯤 붙는다(S펜 에어 커맨드처럼) ---------- */

    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("deprecation")
    private void show() {
        try {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            v = new TextView(this);
            v.setGravity(Gravity.CENTER);
            v.setTextColor(Color.WHITE);
            v.setIncludeFontPadding(false);
            v.setTypeface(v.getTypeface(), android.graphics.Typeface.BOLD);
            int size = dp(44);
            lp = new WindowManager.LayoutParams(size, size, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
            lp.gravity = Gravity.TOP | Gravity.START;
            SharedPreferences p = CounterStore.prefs(this);
            DisplayMetrics m = getResources().getDisplayMetrics();
            right = p.getBoolean("bubR", true);
            lp.y = p.getInt("bubY", m.heightPixels / 3);
            v.setOnTouchListener(new View.OnTouchListener() {
                float sx, sy; int ox, oy; boolean moved;
                @Override public boolean onTouch(View view, MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            sx = e.getRawX(); sy = e.getRawY(); ox = lp.x; oy = lp.y; moved = false;
                            h.removeCallbacks(dim); v.setAlpha(1f);
                            return true;
                        case MotionEvent.ACTION_MOVE: {
                            float dx = e.getRawX() - sx, dy = e.getRawY() - sy;
                            if (!moved && Math.hypot(dx, dy) < dp(8)) return true;
                            if (!moved) { moved = true; closePanel(); }
                            lp.x = (int) (ox + dx); lp.y = (int) (oy + dy);
                            try { wm.updateViewLayout(v, lp); } catch (Exception ignored) {}
                            return true;
                        }
                        case MotionEvent.ACTION_UP:
                            if (moved) {
                                right = lp.x + lp.width / 2 > getResources().getDisplayMetrics().widthPixels / 2;
                                snap();
                            } else if (panel != null) closePanel();
                            else openPanel();
                            return true;
                        case MotionEvent.ACTION_CANCEL:
                            snap();
                            return true;
                    }
                    return false;
                }
            });
            wm.addView(v, lp);
            snap();
            refresh();
        } catch (Exception e) {
            v = null;
            stopSelf();
        }
    }

    /** 가까운 옆 가장자리에 반쯤 걸쳐 붙이고 흐리게 */
    private void snap() {
        if (v == null) return;
        DisplayMetrics m = getResources().getDisplayMetrics();
        int s = lp.width;
        lp.x = right ? m.widthPixels - s / 2 : -s / 2;
        lp.y = Math.max(dp(24), Math.min(lp.y, m.heightPixels - s - dp(48)));
        try { wm.updateViewLayout(v, lp); } catch (Exception ignored) {}
        h.removeCallbacks(dim);
        h.postDelayed(dim, 800);
        CounterStore.prefs(this).edit().putBoolean("bubR", right).putInt("bubY", lp.y).apply();
    }

    /* ---------- 펼친 판: 아이콘을 누르면 옆으로 열린다. 밖을 누르거나 8초 동안 안 쓰면 닫힌다 ---------- */

    @SuppressLint("ClickableViewAccessibility")
    @SuppressWarnings("deprecation")
    private void openPanel() {
        if (panel != null || v == null) return;
        try {
            boolean wait = !CounterStore.active(this); // 대기: 동그라미를 그리면 회기 시작
            boolean draw = wait || "draw".equals(trig(this));
            String mode = CounterStore.uiMode(this);
            boolean freq = !wait && CounterStore.FREQ.equals(mode);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(10), dp(8), dp(10), dp(10));
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(16));
            bg.setColor(Color.argb(240, 250, 252, 251));
            bg.setStroke(dp(1), Color.parseColor("#9DB5B1"));
            box.setBackground(bg);
            box.setElevation(dp(8));

            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            head = new TextView(this);
            head.setTextColor(Color.parseColor("#152426"));
            head.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            head.setTypeface(head.getTypeface(), android.graphics.Typeface.BOLD);
            top.addView(head, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (freq) top.addView(small("−1", () -> { CounterReceiver.handle(getApplicationContext(), CounterReceiver.DEC); poke(); }));
            top.addView(small("✕", this::closePanel));
            box.addView(top);

            View main;
            boolean lvl = !wait && CounterStore.LVL.equals(mode), iv = !wait && CounterStore.INT.equals(mode);
            if (lvl) top.addView(small("↶", () -> { CounterReceiver.handle(getApplicationContext(), CounterReceiver.LV_UNDO); poke(); }), 1);
            if (lvl || iv) { // 강도 1·2·3 / 간격 신호 있음·없음: 판에 버튼을 늘어놓는다(긋기 대신)
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                if (lvl) {
                    row.addView(pick("1", CounterReceiver.LV1));
                    row.addView(pick("2", CounterReceiver.LV2));
                    row.addView(pick("3", CounterReceiver.LV3));
                } else {
                    row.addView(pick("있음", CounterReceiver.IV_YES));
                    row.addView(pick("없음", CounterReceiver.IV_NO));
                }
                main = row;
                LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                rl.topMargin = dp(6);
                box.addView(row, rl);
            } else if (draw) {
                pad = new Pad(this);
                main = pad;
                box.addView(pad, new LinearLayout.LayoutParams(dp(190), dp(140)));
            } else {
                hit = new TextView(this);
                hit.setGravity(Gravity.CENTER);
                hit.setTextColor(Color.WHITE);
                hit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
                hit.setTypeface(hit.getTypeface(), android.graphics.Typeface.BOLD);
                hit.setOnClickListener(x -> { act(); poke(); });
                main = hit;
                LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(dp(150), dp(96));
                hl.topMargin = dp(6);
                box.addView(hit, hl);
            }
            if (main != null) main.setTag("main");

            plp = new WindowManager.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
            plp.gravity = Gravity.TOP | (right ? Gravity.END : Gravity.START);
            plp.x = lp.width / 2 + dp(6); // 아이콘 옆
            plp.y = Math.max(dp(16), lp.y - dp(70));
            box.setOnTouchListener((x, e) -> { if (e.getActionMasked() == MotionEvent.ACTION_OUTSIDE) closePanel(); return false; });
            panel = box;
            wm.addView(panel, plp);
            v.setAlpha(1f);
            h.removeCallbacks(dim);
            refresh();
            poke();
        } catch (Exception e) {
            panel = null; pad = null; hit = null; head = null;
        }
    }

    /** 강도·간격 기록 판의 큰 버튼 */
    private TextView pick(String t, String action) {
        TextView b = new TextView(this);
        b.setText(t);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(Color.WHITE);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, t.length() > 1 ? 18 : 24);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(Color.parseColor("#1B6862"));
        b.setBackground(g);
        b.setOnClickListener(x -> {
            b.animate().scaleX(1.08f).scaleY(1.08f).setDuration(80).withEndAction(() -> b.animate().scaleX(1f).scaleY(1f).setDuration(110)).start();
            CounterReceiver.handle(getApplicationContext(), action);
            poke();
        });
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(dp(t.length() > 1 ? 76 : 56), dp(80));
        l.leftMargin = dp(4); l.rightMargin = dp(4);
        b.setLayoutParams(l);
        return b;
    }

    private TextView small(String t, Runnable r) {
        TextView b = new TextView(this);
        b.setText(t);
        b.setGravity(Gravity.CENTER);
        b.setTextColor(Color.parseColor("#152426"));
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(10));
        g.setColor(Color.WHITE);
        g.setStroke(dp(1), Color.parseColor("#9DB5B1"));
        b.setBackground(g);
        b.setOnClickListener(x -> r.run());
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(dp(44), dp(36));
        l.leftMargin = dp(6);
        b.setLayoutParams(l);
        return b;
    }

    /** 쓰는 중이면 닫힘 시간을 미룬다 */
    private void poke() { h.removeCallbacks(autoClose); h.postDelayed(autoClose, 8000); }
    private final Runnable autoClose = this::closePanel;

    void closePanel() {
        h.removeCallbacks(autoClose);
        h.removeCallbacks(judge);
        if (panel != null && wm != null) { try { wm.removeView(panel); } catch (Exception ignored) {} }
        panel = null; pad = null; hit = null; head = null;
        h.removeCallbacks(dim);
        h.postDelayed(dim, 800);
    }

    /** ＋1(빈도) / 시작·끝(지속시간) / 지시·반응(지연시간). 강도·간격 기록은 판의 버튼으로. 그 밖은 앱을 연다 */
    private void act() {
        String mode = CounterStore.uiMode(this);
        String a;
        if (CounterStore.FREQ.equals(mode)) a = CounterReceiver.INC;
        else if (CounterStore.DUR.equals(mode)) a = CounterReceiver.DUR;
        else if (CounterStore.LAT.equals(mode)) a = CounterReceiver.LAT;
        else {
            closePanel();
            Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            try { startActivity(open); } catch (Exception ignored) {}
            return;
        }
        View t = hit != null ? hit : pad;
        if (t != null) t.animate().scaleX(1.06f).scaleY(1.06f).setDuration(80).withEndAction(() -> t.animate().scaleX(1f).scaleY(1f).setDuration(110)).start();
        CounterReceiver.handle(getApplicationContext(), a); // 기록·진동·알림/위젯 갱신·refresh까지
    }

    void refresh() {
        if (v == null) return;
        String mode = CounterStore.uiMode(this);
        boolean run = CounterStore.running(this);
        boolean freq = CounterStore.FREQ.equals(mode);
        int n = CounterStore.count(this);
        String big;
        if (freq) big = "＋1";
        else if (CounterStore.DUR.equals(mode)) big = run ? "행동 끝" : "행동 시작";
        else if (CounterStore.LAT.equals(mode)) big = run ? "반응" : "지시";
        else if (CounterStore.LVL.equals(mode)) big = "강도 " + n + "회";
        else if (CounterStore.INT.equals(mode)) big = CounterStore.ivPend(this) > 0 ? "지금 신호" : "간격 " + CounterStore.ivYes(this) + "/" + CounterStore.ivTot(this);
        else big = "앱 열기";
        boolean wait = !CounterStore.active(this);
        if (wait) big = "대기 · ○=시작";
        // 아이콘: 지금 횟수(빈도) 또는 ＋, 대기 중엔 ○
        v.setText(wait ? "○" : freq ? String.valueOf(n) : "＋");
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, freq && n >= 100 ? 12 : 15);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(run ? Color.parseColor("#B3402A") : Color.parseColor("#2B3A3C"));
        bg.setStroke(dp(2), Color.argb(200, 255, 255, 255));
        v.setBackground(bg);
        if (head != null) head.setText(wait ? big : freq ? n + "회" : big);
        if (hit != null) {
            hit.setText(big);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(14));
            g.setColor(run ? Color.parseColor("#B3402A") : Color.parseColor("#1B6862"));
            hit.setBackground(g);
        }
        if (pad != null) { pad.run = run; pad.invalidate(); }
    }

    /** 트리거가 바뀌면 열린 판을 닫는다(다음에 열 때 새 모양) */
    void rebuild() { closePanel(); refresh(); }

    /** 판에 획을 다 그은 뒤(0.7초 쉬면) ＋1. 너무 짧은 것(톡 닿음·손바닥)은 세지 않는다 */
    private final Runnable judge = () -> {
        if (pad == null) return;
        List<float[]> drawn = pad.take();
        float L = 0;
        for (float[] st : drawn) for (int i = 2; i + 1 < st.length; i += 2) L += (float) Math.hypot(st[i] - st[i - 2], st[i + 1] - st[i - 1]);
        if (L < dp(14)) { pad.flash(0); return; }
        if (!CounterStore.active(this)) { // 대기: 동그라미일 때만 회기 시작(다른 모양은 무시)
            if (!circle(drawn)) { pad.flash(2); return; }
            pad.flash(1);
            CounterReceiver.handle(getApplicationContext(), CounterReceiver.BEGIN);
            h.postDelayed(this::rebuild, 600); // 다음에 열면 회기 중 판(＋1 등)
            return;
        }
        pad.flash(1);
        act();
    };

    /** 한 붓(또는 이어 그은 획)이 동그라미인지: 크기가 있고, 끝이 처음 가까이 돌아오고, 중심에서 거리가 고르고, 한 바퀴쯤 돈다 */
    static boolean circle(List<float[]> drawn) {
        int n = 0;
        for (float[] st : drawn) n += st.length / 2;
        if (n < 8) return false;
        float[] x = new float[n], y = new float[n];
        int k = 0;
        for (float[] st : drawn) for (int i = 0; i + 1 < st.length; i += 2) { x[k] = st[i]; y[k] = st[i + 1]; k++; }
        float minX = x[0], maxX = x[0], minY = y[0], maxY = y[0], cx = 0, cy = 0;
        for (int i = 0; i < n; i++) { minX = Math.min(minX, x[i]); maxX = Math.max(maxX, x[i]); minY = Math.min(minY, y[i]); maxY = Math.max(maxY, y[i]); cx += x[i]; cy += y[i]; }
        cx /= n; cy /= n;
        float w = maxX - minX, hgt = maxY - minY;
        if (Math.min(w, hgt) < 30 || Math.max(w, hgt) > 3.0f * Math.min(w, hgt)) return false;
        float diag = (float) Math.hypot(w, hgt);
        if (Math.hypot(x[n - 1] - x[0], y[n - 1] - y[0]) > 0.4f * diag) return false; // 닫혀야 함
        double mean = 0, sq = 0;
        for (int i = 0; i < n; i++) { double r = Math.hypot(x[i] - cx, y[i] - cy); mean += r; sq += r * r; }
        mean /= n;
        double sd = Math.sqrt(Math.max(0, sq / n - mean * mean));
        if (mean <= 0 || sd / mean > 0.35) return false; // 중심에서 거리가 고름
        double turn = 0, prev = Math.atan2(y[0] - cy, x[0] - cx);
        for (int i = 1; i < n; i++) {
            double a = Math.atan2(y[i] - cy, x[i] - cx), d = a - prev;
            while (d > Math.PI) d -= 2 * Math.PI;
            while (d < -Math.PI) d += 2 * Math.PI;
            turn += d; prev = a;
        }
        return Math.abs(turn) > 1.6 * Math.PI; // 0.8바퀴 넘게 돎
    }

    /** 긋기 판: 아무 획이나 그으면 ＋1 */
    private class Pad extends View {
        final Paint ink = new Paint(Paint.ANTI_ALIAS_FLAG), box = new Paint(Paint.ANTI_ALIAS_FLAG), txt = new Paint(Paint.ANTI_ALIAS_FLAG);
        final List<float[]> strokes = new ArrayList<>();
        final List<Float> cur = new ArrayList<>();
        final Path path = new Path();
        boolean run;
        int fl;
        @SuppressLint("ClickableViewAccessibility")
        Pad(Context c) {
            super(c);
            ink.setStyle(Paint.Style.STROKE); ink.setStrokeWidth(dp(4)); ink.setStrokeCap(Paint.Cap.ROUND); ink.setStrokeJoin(Paint.Join.ROUND); ink.setColor(Color.parseColor("#1B6862"));
            txt.setColor(Color.parseColor("#7A8C8E")); txt.setTextSize(dp(13)); txt.setTextAlign(Paint.Align.CENTER);
            setOnTouchListener((x, e) -> {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN: h.removeCallbacks(judge); fl = 0; cur.clear(); cur.add(e.getX()); cur.add(e.getY()); path.moveTo(e.getX(), e.getY()); poke(); invalidate(); return true;
                    case MotionEvent.ACTION_MOVE:
                        for (int i = 0; i < e.getHistorySize(); i++) { cur.add(e.getHistoricalX(i)); cur.add(e.getHistoricalY(i)); path.lineTo(e.getHistoricalX(i), e.getHistoricalY(i)); }
                        cur.add(e.getX()); cur.add(e.getY()); path.lineTo(e.getX(), e.getY()); invalidate(); return true;
                    case MotionEvent.ACTION_UP: {
                        float[] f = new float[cur.size()]; for (int i = 0; i < f.length; i++) f[i] = cur.get(i);
                        if (f.length >= 2) strokes.add(f);
                        cur.clear(); h.removeCallbacks(judge); h.postDelayed(judge, 700); return true;
                    }
                    case MotionEvent.ACTION_CANCEL: cur.clear(); strokes.clear(); path.reset(); invalidate(); return true;
                }
                return false;
            });
        }
        List<float[]> take() { List<float[]> r = new ArrayList<>(strokes); strokes.clear(); return r; }
        void flash(int f) { fl = f; path.reset(); invalidate(); h.postDelayed(() -> { fl = 0; invalidate(); }, 500); }
        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), hh = getHeight(), r = dp(12);
            box.setStyle(Paint.Style.FILL);
            box.setColor(fl == 1 ? Color.parseColor("#D2E9E5") : Color.WHITE);
            c.drawRoundRect(0, 0, w, hh, r, r, box);
            box.setStyle(Paint.Style.STROKE); box.setStrokeWidth(dp(2));
            box.setColor(run ? Color.parseColor("#B3402A") : Color.parseColor("#1B6862"));
            c.drawRoundRect(dp(1), dp(1), w - dp(1), hh - dp(1), r, r, box);
            boolean wait = !CounterStore.active(getContext());
            if (path.isEmpty() && fl == 0) c.drawText(wait ? "○ 그리면 회기 시작" : "그으면 ＋1", w / 2, hh / 2 + dp(5), txt);
            if (fl == 2) c.drawText("동그라미를 그려 주세요", w / 2, hh / 2 + dp(5), txt);
            c.drawPath(path, ink);
        }
    }

    @Override
    public void onDestroy() {
        if (inst == this) inst = null;
        closePanel();
        h.removeCallbacksAndMessages(null);
        if (v != null && wm != null) { try { wm.removeView(v); } catch (Exception ignored) {} }
        v = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
