package io.github.ij17838creator.pbs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

/**
 * 홈 화면 위젯. 버튼은 CounterReceiver로 바로 가서 기록되므로 앱이 꺼져 있어도 된다.
 * 빈도: ＋1(크게) · −1 · 회기 끝 / 지속시간: 행동 시작·끝(크게, 같은 버튼) · 회기 끝, 진행 중엔 −10초·−30초·−1분(늦은 시작 보정). 열린 회기가 없으면 '회기 시작'.
 * 지연시간: 지시·반응 시작(크게, 같은 버튼) · 기다리는 동안 중단 / 강도: 1·2·3단계(큰 버튼 3개) · 되돌리기 · 회기 끝
 * 간격 기록: 관찰 시작 → 신호 때 있음·없음(큰 버튼 2개) · 회기 끝.
 */
public class CounterWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        if (ids == null || ids.length == 0) return;
        m.updateAppWidget(ids, views(c));
    }

    static void updateAll(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            if (m == null) return;
            int[] ids = m.getAppWidgetIds(new ComponentName(c, CounterWidget.class));
            if (ids == null || ids.length == 0) return;
            m.updateAppWidget(ids, views(c));
        } catch (Exception ignored) {}
    }

    private static PendingIntent openApp(Context c) {
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 20, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void button(Context c, RemoteViews v, int id, String label, String act, int req) {
        v.setViewVisibility(id, View.VISIBLE);
        v.setTextViewText(id, label);
        v.setOnClickPendingIntent(id, CounterNotifier.action(c, act, req));
    }

    /** ＋1 둘레에 남은 측정 시간 고리: 시계 방향으로 줄어들고, 끝나면 빨간 한 바퀴. 측정 시간(sesMin)이 있을 때만 */
    private static void ring(Context c, RemoteViews v) {
        long end = CounterStore.sesEnd(c);
        int min = CounterStore.sesMin(c);
        if (end <= 0 || min <= 0) return;
        long total = min * 60000L;
        float left = Math.max(0f, Math.min(1f, (end - System.currentTimeMillis()) / (float) total));
        v.setImageViewBitmap(R.id.w_ring, ringBitmap(left));
        v.setViewVisibility(R.id.w_ring, View.VISIBLE);
        v.setOnClickPendingIntent(R.id.w_ring, CounterNotifier.action(c, CounterReceiver.INC, 1));
    }

    static Bitmap ringBitmap(float left) {
        int n = 160;
        float sw = 12f;
        Bitmap b = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        RectF r = new RectF(sw / 2 + 1, sw / 2 + 1, n - sw / 2 - 1, n - sw / 2 - 1);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(sw);
        p.setStrokeCap(Paint.Cap.BUTT);
        if (left <= 0f) {
            p.setColor(0xFFFF8A7A); // 끝: 빨간 한 바퀴
            cv.drawOval(r, p);
            return b;
        }
        p.setColor(0x55FFFFFF); // 지나간 시간
        cv.drawOval(r, p);
        p.setColor(0xFFFFFFFF); // 남은 시간: 12시에서 지나간 만큼 시계 방향으로 밀려 줄어듦
        float gone = 360f * (1f - left);
        cv.drawArc(r, -90f + gone, 360f * left, false, p);
        return b;
    }

    static boolean has(Context c) {
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m == null ? null : m.getAppWidgetIds(new ComponentName(c, CounterWidget.class));
            return ids != null && ids.length > 0;
        } catch (Exception e) { return false; }
    }

    static RemoteViews views(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_counter);
        v.setOnClickPendingIntent(R.id.w_head, openApp(c));
        v.setViewVisibility(R.id.w_chrono, View.GONE);
        v.setViewVisibility(R.id.w_sub1, View.GONE);
        v.setViewVisibility(R.id.w_sub2, View.GONE);
        v.setViewVisibility(R.id.w_adj, View.GONE);
        v.setViewVisibility(R.id.w_lvl, View.GONE);
        v.setViewVisibility(R.id.w_mainbox, View.VISIBLE);
        v.setViewVisibility(R.id.w_ring, View.GONE);
        if (!CounterStore.anything(c)) {
            v.setTextViewText(R.id.w_title, "PBS 기록");
            v.setTextViewText(R.id.w_status, "앱에서 측정 대상을 고르세요");
            v.setTextViewText(R.id.w_main, "앱 열기");
            v.setOnClickPendingIntent(R.id.w_main, openApp(c));
            v.setViewVisibility(R.id.w_row, View.GONE);
            return v;
        }
        v.setViewVisibility(R.id.w_row, View.VISIBLE);
        v.setTextViewText(R.id.w_title, CounterNotifier.titleText(c));
        v.setTextViewText(R.id.w_status, CounterNotifier.statusText(c));
        boolean on = CounterStore.active(c);
        String um = CounterStore.uiMode(c);
        boolean dur = CounterStore.DUR.equals(um);
        if (CounterStore.LAT.equals(um)) {
            boolean run = on && CounterStore.running(c);
            button(c, v, R.id.w_main, run ? "반응 시작" : "지시", CounterReceiver.LAT, 15);
            if (run) {
                long base = SystemClock.elapsedRealtime() - Math.max(0L, System.currentTimeMillis() - CounterStore.runStart(c));
                v.setChronometer(R.id.w_chrono, base, null, true);
                v.setViewVisibility(R.id.w_chrono, View.VISIBLE);
                button(c, v, R.id.w_sub1, "중단", CounterReceiver.LAT_X, 16);
                button(c, v, R.id.w_sub2, "회기 끝", CounterReceiver.END, 3);
            } else if (on) {
                button(c, v, R.id.w_sub1, "회기 끝", CounterReceiver.END, 3);
            } else {
                button(c, v, R.id.w_sub1, "회기 시작", CounterReceiver.BEGIN, 6);
            }
        } else if (CounterStore.LVL.equals(um)) {
            v.setViewVisibility(R.id.w_mainbox, View.GONE);
            v.setViewVisibility(R.id.w_lvl, View.VISIBLE);
            button(c, v, R.id.w_l1, "1 약", CounterReceiver.LV1, 17);
            button(c, v, R.id.w_l2, "2 중", CounterReceiver.LV2, 18);
            button(c, v, R.id.w_l3, "3 강", CounterReceiver.LV3, 19);
            if (on) {
                button(c, v, R.id.w_sub1, "되돌리기", CounterReceiver.LV_UNDO, 20);
                button(c, v, R.id.w_sub2, "회기 끝", CounterReceiver.END, 3);
            } else {
                button(c, v, R.id.w_sub1, "회기 시작", CounterReceiver.BEGIN, 6);
            }
        } else if (CounterStore.INT.equals(um)) {
            if (on) {
                v.setViewVisibility(R.id.w_mainbox, View.GONE);
                v.setViewVisibility(R.id.w_lvl, View.VISIBLE);
                button(c, v, R.id.w_l1, "있음", CounterReceiver.IV_YES, 21);
                button(c, v, R.id.w_l2, "없음", CounterReceiver.IV_NO, 22);
                v.setViewVisibility(R.id.w_l3, View.GONE);
                button(c, v, R.id.w_sub1, "회기 끝", CounterReceiver.END, 3);
            } else {
                button(c, v, R.id.w_main, "관찰 시작", CounterReceiver.BEGIN, 6);
                v.setViewVisibility(R.id.w_row, View.GONE);
            }
        } else if (dur) {
            boolean run = on && CounterStore.running(c);
            button(c, v, R.id.w_main, run ? "행동 끝" : "행동 시작", CounterReceiver.DUR, 5);
            if (run) {
                long base = SystemClock.elapsedRealtime() - Math.max(0L, System.currentTimeMillis() - CounterStore.runStart(c));
                v.setChronometer(R.id.w_chrono, base, null, true);
                v.setViewVisibility(R.id.w_chrono, View.VISIBLE);
                if (CounterStore.adjOn(c)) {
                    v.setViewVisibility(R.id.w_adj, View.VISIBLE);
                    button(c, v, R.id.w_adj10, "−10초", CounterReceiver.ADJ10, 7);
                    button(c, v, R.id.w_adj30, "−30초", CounterReceiver.ADJ30, 8);
                    button(c, v, R.id.w_adj60, "−1분", CounterReceiver.ADJ60, 12);
                }
            }
            if (on) button(c, v, R.id.w_sub1, "회기 끝", CounterReceiver.END, 3);
            else button(c, v, R.id.w_sub1, "회기 시작", CounterReceiver.BEGIN, 6);
        } else {
            button(c, v, R.id.w_main, "＋1", CounterReceiver.INC, 1);
            if (on) ring(c, v);
            if (on) {
                button(c, v, R.id.w_sub1, "−1", CounterReceiver.DEC, 2);
                button(c, v, R.id.w_sub2, "회기 끝", CounterReceiver.END, 3);
            } else {
                button(c, v, R.id.w_sub1, "회기 시작", CounterReceiver.BEGIN, 6);
            }
        }
        return v;
    }
}
