package io.github.ij17838creator.pbs;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

/**
 * 홈 화면 위젯. 버튼은 CounterReceiver로 바로 가서 기록되므로 앱이 꺼져 있어도 된다.
 * 빈도: ＋1(크게) · −1 · 회기 끝 / 지속시간: 행동 시작·끝(크게) · 회기 끝. 열린 회기가 없으면 '회기 시작'.
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

    static RemoteViews views(Context c) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_counter);
        v.setOnClickPendingIntent(R.id.w_head, openApp(c));
        v.setViewVisibility(R.id.w_chrono, View.GONE);
        v.setViewVisibility(R.id.w_sub1, View.GONE);
        v.setViewVisibility(R.id.w_sub2, View.GONE);
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
        boolean dur = CounterStore.DUR.equals(CounterStore.uiMode(c));
        if (dur) {
            boolean run = on && CounterStore.running(c);
            button(c, v, R.id.w_main, run ? "행동 끝" : "행동 시작", CounterReceiver.DUR, 5);
            if (run) {
                long base = SystemClock.elapsedRealtime() - Math.max(0L, System.currentTimeMillis() - CounterStore.runStart(c));
                v.setChronometer(R.id.w_chrono, base, null, true);
                v.setViewVisibility(R.id.w_chrono, View.VISIBLE);
            }
            if (on) button(c, v, R.id.w_sub1, "회기 끝", CounterReceiver.END, 3);
            else button(c, v, R.id.w_sub1, "회기 시작", CounterReceiver.BEGIN, 6);
        } else {
            button(c, v, R.id.w_main, "＋1", CounterReceiver.INC, 1);
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
