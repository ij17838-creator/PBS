package io.github.ij17838creator.pbs;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 알림창에 늘 떠 있는 측정 알림을 그린다 (빈도: ＋1 · −1 · 회기 끝 / 지속시간: 행동 시작·끝 · 회기 끝) */
final class CounterNotifier {
    static final String CHANNEL = "pbs_counter";
    static final int ID = 1701;

    private CounterNotifier() {}

    static void ensureChannel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL) != null) return;
        NotificationChannel ch = new NotificationChannel(CHANNEL, "측정 버튼", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("알림창에서 바로 횟수를 기록하는 버튼");
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setShowBadge(false);
        ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
    }

    static PendingIntent action(Context c, String act, int req) {
        Intent i = new Intent(c, CounterReceiver.class).setAction(act);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** 알림 한 줄 설명 (위젯·타일도 같은 글을 쓴다) */
    static String statusText(Context c) {
        if (!CounterStore.active(c)) {
            return CounterStore.DUR.equals(CounterStore.tMode(c)) ? "대기 · 행동 시작을 누르면 새 회기" : "대기 · ＋1을 누르면 새 회기";
        }
        if (CounterStore.DUR.equals(CounterStore.mode(c))) {
            long min = Math.round(CounterStore.durMs(c) / 6000.0);
            String sum = CounterStore.count(c) + "회 · 총 " + (min / 10) + "." + (min % 10) + "분";
            return CounterStore.running(c) ? "행동 진행 중 · 앞서 " + sum : "지금 " + sum;
        }
        int count = CounterStore.count(c);
        long last = CounterStore.last(c);
        return "지금 " + count + "회" + (last > 0 ? " · 마지막 " + new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date(last)) : "");
    }

    static String titleText(Context c) {
        return CounterStore.active(c) ? CounterStore.title(c) : CounterStore.tTitle(c);
    }

    static boolean show(Context c) {
        if (!CounterStore.anything(c)) { cancel(c); return false; }
        ensureChannel(c);
        NotificationManagerCompat nm = NotificationManagerCompat.from(c);
        if (!nm.areNotificationsEnabled()) return false;
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(c, 10, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        boolean on = CounterStore.active(c);
        boolean dur = CounterStore.DUR.equals(CounterStore.uiMode(c));
        boolean run = on && CounterStore.running(c);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pbs)
            .setContentTitle(titleText(c))
            .setContentText(statusText(c))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(run)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openPi)
            .setDeleteIntent(action(c, CounterReceiver.DISMISS, 4));
        if (run) b.setWhen(CounterStore.runStart(c)).setUsesChronometer(true);
        if (dur) {
            b.addAction(0, run ? "행동 끝" : "행동 시작", action(c, CounterReceiver.DUR, 5));
            if (on) b.addAction(0, "회기 끝", action(c, CounterReceiver.END, 3));
            else b.addAction(0, "회기 시작", action(c, CounterReceiver.BEGIN, 6));
        } else if (on) {
            b.addAction(0, "＋1", action(c, CounterReceiver.INC, 1))
                .addAction(0, "−1", action(c, CounterReceiver.DEC, 2))
                .addAction(0, "회기 끝", action(c, CounterReceiver.END, 3));
        } else {
            b.addAction(0, "＋1", action(c, CounterReceiver.INC, 1))
                .addAction(0, "회기 시작", action(c, CounterReceiver.BEGIN, 6));
        }
        try { nm.notify(ID, b.build()); return true; }
        catch (SecurityException e) { return false; }
    }

    /** 알림·위젯·빠른 설정 타일을 한꺼번에 새로 그린다 */
    static void refreshAll(Context c) {
        show(c);
        CounterWidget.updateAll(c);
        CounterTile.refresh(c);
    }

    static void cancel(Context c) {
        NotificationManagerCompat.from(c).cancel(ID);
    }
}
