package io.github.ij17838creator.pbs;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.Notification;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 알림창에 늘 떠 있는 측정 알림을 그린다 (빈도: ＋1 · −1 · 회기 끝 / 지속시간: 행동 시작·끝 · 회기 끝) */
final class CounterNotifier {
    static final String CHANNEL = "pbs_counter";
    static final int ID = 1701;
    /** 끝 확인 '아직 행동 중인가요?' 알림 */
    static final int CHECK_ID = 1702;
    /** '응답이 없어 자동으로 끝냈어요' 알림 */
    static final int AUTO_ID = 1703;
    /** 간격 기록 신호 '지금 행동 있나요?' / 지연시간 최대 대기 넘음 */
    static final int IV_ID = 1704;
    static final int LAT_ID = 1705;

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
        long se = CounterStore.sesEnd(c);
        String t = statusBase(c);
        return se > 0 ? t + " · " + CounterStore.hm(se) + "까지" : t;
    }

    private static String statusBase(Context c) {
        if (!CounterStore.active(c)) {
            switch (CounterStore.tMode(c)) {
                case CounterStore.DUR: return "대기 · 행동 시작을 누르면 새 회기";
                case CounterStore.LAT: return "대기 · 지시를 누르면 새 회기";
                case CounterStore.LVL: return "대기 · 단계를 누르면 새 회기";
                case CounterStore.INT: return "대기 · 관찰 시작을 누르세요";
                default: return CounterStore.isAbc(c) ? "대기 · ＋1을 누르면 관찰 시작" : "대기 · 회기 시작을 누르세요";
            }
        }
        String md = CounterStore.mode(c);
        if (CounterStore.LAT.equals(md)) {
            if (CounterStore.running(c)) return "반응 기다리는 중 · 최대 " + CounterStore.latMax(c) + "초";
            int n = CounterStore.count(c);
            return "지시 " + n + "회" + (n > 0 ? " · 평균 " + Math.round(CounterStore.durMs(c) / 1000.0 / n) + "초" : "");
        }
        if (CounterStore.LVL.equals(md)) {
            int lv = CounterStore.lastLvl(c);
            return "지금 " + CounterStore.count(c) + "회" + (lv > 0 ? " · 마지막 " + lv + "단계" : "");
        }
        if (CounterStore.INT.equals(md)) {
            long nx = CounterStore.ivNext(c);
            return "있음 " + CounterStore.ivYes(c) + "/" + CounterStore.ivTot(c) + "칸"
                + (CounterStore.ivPend(c) > 0 ? " · 답할 신호 있음" : "")
                + (nx > 0 ? " · 다음 " + new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date(nx)) : "");
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
        NotificationManagerCompat nm = NotificationManagerCompat.from(c);
        if (!nm.areNotificationsEnabled()) return false;
        try { nm.notify(ID, build(c)); return true; }
        catch (SecurityException e) { return false; }
    }

    private static PendingIntent openApp(Context c) {
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 10, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** 측정 알림 (끝 확인 시계 서비스도 이 알림을 쓴다) */
    static Notification build(Context c) {
        ensureChannel(c);
        Intent open = new Intent(c, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(c, 10, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        boolean on = CounterStore.active(c);
        String um = CounterStore.uiMode(c);
        boolean dur = CounterStore.DUR.equals(um);
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
        if (CounterStore.LAT.equals(um)) {
            // 지연시간: 지시 → 반응 시작(같은 버튼), 기다리는 동안 '중단'
            b.addAction(0, run ? "반응 시작" : "지시", action(c, CounterReceiver.LAT, 15));
            if (run) b.addAction(0, "중단", action(c, CounterReceiver.LAT_X, 16));
            else if (on) b.addAction(0, "회기 끝", action(c, CounterReceiver.END, 3));
            else b.addAction(0, "회기 시작", action(c, CounterReceiver.BEGIN, 6));
        } else if (CounterStore.LVL.equals(um)) {
            // 강도: 알림 버튼은 3개까지라 단계 버튼만 (되돌리기·회기 끝은 위젯·앱에서)
            b.addAction(0, "1 약", action(c, CounterReceiver.LV1, 17))
                .addAction(0, "2 중", action(c, CounterReceiver.LV2, 18))
                .addAction(0, "3 강", action(c, CounterReceiver.LV3, 19));
        } else if (CounterStore.INT.equals(um)) {
            if (on) {
                b.addAction(0, "있음", action(c, CounterReceiver.IV_YES, 21))
                    .addAction(0, "없음", action(c, CounterReceiver.IV_NO, 22))
                    .addAction(0, "회기 끝", action(c, CounterReceiver.END, 3));
            } else {
                b.addAction(0, "관찰 시작", action(c, CounterReceiver.BEGIN, 6));
            }
        } else if (dur && run && CounterStore.adjOn(c)) {
            // 진행 중: 같은 버튼으로 끝 + 늦게 눌렀을 때 시작 당기기 (알림 버튼은 3개까지라 −1분은 위젯에만)
            b.addAction(0, "행동 끝", action(c, CounterReceiver.DUR, 5))
                .addAction(0, "−10초", action(c, CounterReceiver.ADJ10, 7))
                .addAction(0, "−30초", action(c, CounterReceiver.ADJ30, 8));
        } else if (dur) {
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
        return b.build();
    }

    /* ---------- 끝 확인 ---------- */

    /** 알림 방식마다 채널이 따로 있다(안드로이드는 채널을 만든 뒤 소리·진동을 못 바꾸기 때문) */
    static String checkChannel(Context c, String mode) { return alertChannel(c, mode, false); }

    /** iv=true면 간격 기록 신호용 채널 (끝 확인과 따로 끄고 켤 수 있게) */
    static String alertChannel(Context c, String mode, boolean iv) {
        String id = (iv ? "pbs_iv_" : "pbs_chk_") + mode;
        if (Build.VERSION.SDK_INT < 26) return id;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(id) != null) return id;
        String name;
        switch (mode) {
            case CounterStore.CHK_SOUND: name = "끝 확인 (소리)"; break;
            case CounterStore.CHK_BOTH: name = "끝 확인 (소리+진동)"; break;
            case CounterStore.CHK_QUIET: name = "끝 확인 (무음)"; break;
            default: name = "끝 확인 (진동)";
        }
        if (iv) name = name.replace("끝 확인", "간격 신호");
        NotificationChannel ch = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription(iv ? "간격 기록: 지금 행동이 있는지 묻는 신호" : "지속시간 행동이 아직 진행 중인지 묻는 알림");
        boolean snd = CounterStore.CHK_SOUND.equals(mode) || CounterStore.CHK_BOTH.equals(mode);
        boolean vib = CounterStore.CHK_VIB.equals(mode) || CounterStore.CHK_BOTH.equals(mode);
        if (snd) {
            ch.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        } else {
            ch.setSound(null, null);
        }
        ch.enableVibration(vib);
        if (vib) ch.setVibrationPattern(new long[] { 0, 250, 150, 250 });
        ch.setShowBadge(false);
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);
        return id;
    }

    private static String mmss(long ms) {
        long s = Math.max(0L, ms / 1000L);
        return s >= 60 ? (s / 60) + "분 " + (s % 60) + "초" : s + "초";
    }

    private static NotificationCompat.Builder alertBase(Context c, String mode) { return alertBase(c, mode, false); }

    private static NotificationCompat.Builder alertBase(Context c, String mode, boolean iv) {
        boolean snd = CounterStore.CHK_SOUND.equals(mode) || CounterStore.CHK_BOTH.equals(mode);
        boolean vib = CounterStore.CHK_VIB.equals(mode) || CounterStore.CHK_BOTH.equals(mode);
        NotificationCompat.Builder b = new NotificationCompat.Builder(c, alertChannel(c, mode, iv))
            .setSmallIcon(R.drawable.ic_stat_pbs)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp(c))
            .setAutoCancel(true);
        // 안드로이드 7 이하(채널 없음)용
        int def = (snd ? Notification.DEFAULT_SOUND : 0) | (vib ? Notification.DEFAULT_VIBRATE : 0);
        b.setDefaults(def);
        if (!snd && !vib) b.setSilent(true);
        return b;
    }

    /** '아직 행동 중인가요?' (계속 · 행동 끝). 다음 확인 때 저절로 사라진다 */
    static void showCheck(Context c) {
        String mode = CounterStore.chkMode(c);
        if (CounterStore.CHK_OFF.equals(mode)) return;
        long now = System.currentTimeMillis();
        int lim = CounterStore.chkMiss(c);
        int left = lim - CounterStore.miss(c);
        String txt = mmss(now - CounterStore.runStart(c)) + "째 진행 중"
            + (lim > 0 ? (left <= 0 ? " · 응답 없으면 다음에 자동 끝" : " · 응답 없이 " + (left + 1) + "번이면 자동 끝") : "");
        NotificationCompat.Builder b = alertBase(c, mode)
            .setContentTitle("아직 행동 중인가요?")
            .setContentText(txt)
            .setTimeoutAfter(CounterStore.chkSec(c) * 1000L)
            .addAction(0, "계속", action(c, CounterReceiver.CONFIRM, 9))
            .addAction(0, "행동 끝", action(c, CounterReceiver.DUR_END, 11));
        try { NotificationManagerCompat.from(c).notify(CHECK_ID, b.build()); } catch (SecurityException ignored) {}
    }

    /** 응답이 없어 자동으로 끝냈을 때 한 번 알림 */
    static void showAutoEnd(Context c, long endAt) {
        String mode = CounterStore.chkMode(c);
        NotificationCompat.Builder b = alertBase(c, CounterStore.CHK_OFF.equals(mode) ? CounterStore.CHK_QUIET : mode)
            .setContentTitle("행동을 자동으로 끝냈어요")
            .setContentText("끝 시각 " + new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date(endAt)) + " · 앱에서 확인");
        try { NotificationManagerCompat.from(c).notify(AUTO_ID, b.build()); } catch (SecurityException ignored) {}
    }

    /* ---------- 지연시간 · 간격 기록 ---------- */

    /** 지연시간: 최대 대기가 지나 '반응 없음(최대)'으로 적었을 때 */
    static void showLatMax(Context c) {
        NotificationCompat.Builder b = alertBase(c, CounterStore.CHK_VIB)
            .setContentTitle("반응 없이 " + CounterStore.latMax(c) + "초 지남")
            .setContentText("최대 대기로 적었어요 · 앱에서 확인")
            .setTimeoutAfter(60 * 1000L);
        try { NotificationManagerCompat.from(c).notify(LAT_ID, b.build()); } catch (SecurityException ignored) {}
    }

    /** 간격 기록 신호: 지금 이 순간 행동이 있나요? (있음 · 없음). 다음 신호 때 저절로 사라진다 */
    static void showIvSignal(Context c) {
        long t = CounterStore.ivPend(c);
        if (t <= 0) return;
        NotificationCompat.Builder b = alertBase(c, CounterStore.ivMode(c), true)
            .setContentTitle("지금 보세요 · 행동 있나요?")
            .setContentText(new SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(new Date(t)) + " 신호 · 다음 신호 전까지 답하기")
            .setTimeoutAfter(Math.max(5000L, CounterStore.ivSec(c) * 1000L - 1000L))
            .addAction(0, "있음", action(c, CounterReceiver.IV_YES, 21))
            .addAction(0, "없음", action(c, CounterReceiver.IV_NO, 22));
        try { NotificationManagerCompat.from(c).notify(IV_ID, b.build()); } catch (SecurityException ignored) {}
    }

    /** 간격 기록: 응답 없는 신호가 10분 넘게 이어져 회기를 닫았을 때 */
    static void showIvAutoEnd(Context c) {
        NotificationCompat.Builder b = alertBase(c, CounterStore.CHK_QUIET, true)
            .setContentTitle("간격 기록을 끝냈어요")
            .setContentText("응답 없는 신호가 10분 넘게 이어짐 · 앱에서 확인");
        try { NotificationManagerCompat.from(c).notify(AUTO_ID, b.build()); } catch (SecurityException ignored) {}
    }

    /** 측정 시간이 끝났을 때 (6 = 회기 닫음, 7 = 새 회기, 8 = 같은 회기에서 시간만 다시) */
    static void showTimer(Context c, int r) {
        int m = CounterStore.sesMin(c);
        String text = r == 6 ? "회기를 닫았어요 · 다음 회기는 다시 시작"
            : r == 7 ? "새 회기를 시작했어요 · 횟수 0부터" : "같은 회기에서 시간만 다시 재요";
        NotificationCompat.Builder b = alertBase(c, CounterStore.CHK_QUIET, true)
            .setContentTitle("측정 시간 " + m + "분 끝")
            .setContentText(text);
        try { NotificationManagerCompat.from(c).notify(AUTO_ID, b.build()); } catch (SecurityException ignored) {}
    }

    static void cancelIv(Context c) {
        NotificationManagerCompat.from(c).cancel(IV_ID);
    }

    static void cancelCheck(Context c) {
        NotificationManagerCompat.from(c).cancel(CHECK_ID);
    }

    /** 알림·위젯·빠른 설정 타일을 한꺼번에 새로 그린다 */
    static void refreshAll(Context c) {
        show(c);
        DurWatch.sync(c);
        CounterWidget.updateAll(c);
        CounterTile.refresh(c);
    }

    static void cancel(Context c) {
        NotificationManagerCompat.from(c).cancel(ID);
    }
}
