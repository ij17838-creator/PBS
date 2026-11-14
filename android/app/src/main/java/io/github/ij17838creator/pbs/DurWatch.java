package io.github.ij17838creator.pbs;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.content.ContextCompat;

/**
 * 끝 확인(끝 깜빡 방지) 시계. 지속시간 행동이 진행 중인 동안만 돈다.
 * 앱이 꺼져 있어도 시간을 재도록 '포그라운드 서비스'로 돌리고(알림창의 측정 알림을 그대로 같이 씀),
 * 서비스를 못 띄우는 기기·상황에 대비해 알람(정확하지 않을 수 있음)도 같이 걸어 둔다.
 * 두 쪽이 같은 때를 두 번 알려도 CounterStore.checkTick이 시각을 보고 한 번만 처리한다.
 * 하드웨어 버튼(전원·음량·이어폰)은 쓰지 않는다.
 */
public class DurWatch extends Service {
    private static volatile boolean alive;
    private static volatile DurWatch inst;
    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::onTick;

    /** 상태가 바뀔 때마다 부른다: 필요하면 시계를 켜고, 아니면 끈다 */
    static void sync(Context c) {
        Context a = c.getApplicationContext();
        if (CounterStore.watching(a)) {
            alarm(a, true);
            DurWatch w = inst;
            if (alive && w != null) {
                w.h.post(w::schedule); // 간격이 바뀌었을 수 있으니 다시 잰다
            } else {
                try { ContextCompat.startForegroundService(a, new Intent(a, DurWatch.class)); }
                catch (Exception ignored) { /* 서비스를 못 띄우면 알람만으로 */ }
            }
        } else {
            alarm(a, false);
            CounterNotifier.cancelCheck(a);
            if (alive) {
                try { a.stopService(new Intent(a, DurWatch.class)); } catch (Exception ignored) {}
            }
        }
    }

    private static PendingIntent alarmPi(Context c) {
        Intent i = new Intent(c, CounterReceiver.class).setAction(CounterReceiver.CHECK);
        return PendingIntent.getBroadcast(c, 40, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void alarm(Context c, boolean on) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            PendingIntent pi = alarmPi(c);
            if (!on) { am.cancel(pi); return; }
            long t = CounterStore.nextCheck(c);
            if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            else am.set(AlarmManager.RTC_WAKEUP, t, pi);
        } catch (Exception ignored) {}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(CounterNotifier.ID, CounterNotifier.build(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(CounterNotifier.ID, CounterNotifier.build(this));
            }
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        alive = true;
        inst = this;
        schedule();
        return START_STICKY;
    }

    private void schedule() {
        h.removeCallbacks(tick);
        Context c = getApplicationContext();
        if (!CounterStore.watching(c)) { stopSelf(); return; }
        long wait = CounterStore.nextCheck(c) - System.currentTimeMillis();
        h.postDelayed(tick, Math.max(500L, wait));
    }

    private void onTick() {
        CounterReceiver.handle(getApplicationContext(), CounterReceiver.CHECK);
        schedule();
    }

    @Override
    public void onDestroy() {
        alive = false;
        if (inst == this) inst = null;
        h.removeCallbacks(tick);
        try {
            if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_DETACH);
            else stopForeground(false);
        } catch (Exception ignored) {}
        // 측정 알림은 남겨 둔다(회기가 열려 있으면 계속 보여야 함)
        CounterNotifier.show(getApplicationContext());
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
