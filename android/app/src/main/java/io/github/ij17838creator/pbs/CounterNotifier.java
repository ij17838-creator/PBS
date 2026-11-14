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

/** 알림창에 늘 떠 있는 '＋1 · −1 · 끝' 알림을 그린다 */
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

    private static PendingIntent action(Context c, String act, int req) {
        Intent i = new Intent(c, CounterReceiver.class).setAction(act);
        return PendingIntent.getBroadcast(c, req, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static boolean show(Context c) {
        if (!CounterStore.active(c)) { cancel(c); return false; }
        ensureChannel(c);
        NotificationManagerCompat nm = NotificationManagerCompat.from(c);
        if (!nm.areNotificationsEnabled()) return false;
        int count = CounterStore.count(c);
        long last = CounterStore.last(c);
        String text = "지금 " + count + "회" + (last > 0 ? " · 마지막 " + new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date(last)) : "");
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(c, 10, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pbs)
            .setContentTitle(CounterStore.title(c))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openPi)
            .setDeleteIntent(action(c, CounterReceiver.DISMISS, 4))
            .addAction(0, "＋1", action(c, CounterReceiver.INC, 1))
            .addAction(0, "−1", action(c, CounterReceiver.DEC, 2))
            .addAction(0, "끝", action(c, CounterReceiver.END, 3));
        try { nm.notify(ID, b.build()); return true; }
        catch (SecurityException e) { return false; }
    }

    static void cancel(Context c) {
        NotificationManagerCompat.from(c).cancel(ID);
    }
}
