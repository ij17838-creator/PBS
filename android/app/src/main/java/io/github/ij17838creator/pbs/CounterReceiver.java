package io.github.ij17838creator.pbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * 알림창·홈 화면 위젯 버튼을 누르면 앱을 열지 않고 여기서 바로 기록한다.
 * 앱이 꺼져 있어도(최근 앱에서 지운 뒤 포함) 안드로이드가 이 부분만 깨워서 실행한다.
 */
public class CounterReceiver extends BroadcastReceiver {
    static final String INC = "io.github.ij17838creator.pbs.COUNTER_INC";
    static final String DEC = "io.github.ij17838creator.pbs.COUNTER_DEC";
    static final String END = "io.github.ij17838creator.pbs.COUNTER_END";
    static final String BEGIN = "io.github.ij17838creator.pbs.COUNTER_BEGIN";
    static final String DUR = "io.github.ij17838creator.pbs.COUNTER_DUR";
    static final String DISMISS = "io.github.ij17838creator.pbs.COUNTER_DISMISS";
    /** 끝 확인 알림의 '계속' / '행동 끝'(끝만, 시작은 안 함) / 확인 시각이 됨(시계·알람) */
    static final String CONFIRM = "io.github.ij17838creator.pbs.COUNTER_CONFIRM";
    static final String DUR_END = "io.github.ij17838creator.pbs.COUNTER_DUR_END";
    static final String CHECK = "io.github.ij17838creator.pbs.COUNTER_CHECK";
    /** 늦은 시작 보정: 진행 중인 행동의 시작을 10초·30초·1분 앞당김 */
    static final String ADJ10 = "io.github.ij17838creator.pbs.COUNTER_ADJ10";
    static final String ADJ30 = "io.github.ij17838creator.pbs.COUNTER_ADJ30";
    static final String ADJ60 = "io.github.ij17838creator.pbs.COUNTER_ADJ60";
    /** 지연시간: 지시/반응 시작(같은 버튼) · 중단 */
    static final String LAT = "io.github.ij17838creator.pbs.COUNTER_LAT";
    static final String LAT_X = "io.github.ij17838creator.pbs.COUNTER_LAT_X";
    /** 강도: 1·2·3단계 · 되돌리기 */
    static final String LV1 = "io.github.ij17838creator.pbs.COUNTER_LV1";
    static final String LV2 = "io.github.ij17838creator.pbs.COUNTER_LV2";
    static final String LV3 = "io.github.ij17838creator.pbs.COUNTER_LV3";
    static final String LV_UNDO = "io.github.ij17838creator.pbs.COUNTER_LV_UNDO";
    /** 간격 기록: 신호에 있음/없음 */
    static final String IV_YES = "io.github.ij17838creator.pbs.COUNTER_IV_YES";
    static final String IV_NO = "io.github.ij17838creator.pbs.COUNTER_IV_NO";

    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        handle(c, a);
    }

    /** 버튼 하나 처리 (빠른 설정 타일도 이걸 쓴다) */
    static void handle(Context c, String a) {
        switch (a) {
            case INC:
                // 열린 회기가 없을 때 ＋1은 회기 시작만 한다(시작하면서 1회가 저절로 세지지 않게)
                if (CounterStore.begin(c)) buzz(c, 40);
                else if (CounterStore.add(c, 1) > 0) buzz(c, 60);
                break;
            case DEC:
                CounterStore.add(c, -1);
                buzz(c, 30);
                break;
            case BEGIN:
                if (CounterStore.begin(c)) buzz(c, 40);
                break;
            case DUR:
                buzz(c, CounterStore.durToggle(c) ? 60 : 120);
                break;
            case DUR_END:
                if (CounterStore.durEnd(c)) buzz(c, 120);
                break;
            case CONFIRM:
                if (CounterStore.confirm(c)) buzz(c, 30);
                CounterNotifier.cancelCheck(c);
                break;
            case ADJ10:
            case ADJ30:
            case ADJ60:
                if (CounterStore.adjust(c, ADJ10.equals(a) ? 10 : ADJ30.equals(a) ? 30 : 60) > 0) buzz(c, 30);
                break;
            case LAT:
                buzz(c, CounterStore.latToggle(c) ? 60 : 120);
                break;
            case LAT_X:
                if (CounterStore.latStop(c)) buzz(c, 200);
                break;
            case LV1:
            case LV2:
            case LV3: {
                int lv = LV1.equals(a) ? 1 : LV2.equals(a) ? 2 : 3;
                if (CounterStore.lvlAdd(c, lv)) buzz(c, 40L * lv); // 단계가 높을수록 길게 떨림
                break;
            }
            case LV_UNDO:
                if (CounterStore.lvlUndo(c)) buzz(c, 30);
                break;
            case IV_YES:
            case IV_NO:
                if (CounterStore.ivAnswer(c, IV_YES.equals(a))) buzz(c, IV_YES.equals(a) ? 80 : 40);
                CounterNotifier.cancelIv(c);
                break;
            case CHECK: {
                int r = CounterStore.checkTick(c);
                if (r == 0) { DurWatch.sync(c); return; } // 아직 때가 아님: 알람만 다시 건다
                if (r == 1) { CounterNotifier.showCheck(c); DurWatch.sync(c); return; }
                if (r == 3) { CounterNotifier.showLatMax(c); break; }
                if (r == 4) { CounterNotifier.showIvSignal(c); break; }
                if (r == 5) { CounterNotifier.cancelIv(c); CounterNotifier.showIvAutoEnd(c); break; }
                if (r >= 6 && r <= 8) { // 측정 시간 끝: 회기 닫음 / 새 회기로 갱신 / 시간만 다시
                    if (r != 8) { CounterNotifier.cancelCheck(c); CounterNotifier.cancelIv(c); }
                    CounterNotifier.showTimer(c, r);
                    buzz(c, 400);
                    break;
                }
                CounterNotifier.cancelCheck(c);
                CounterNotifier.showAutoEnd(c, CounterStore.last(c));
                break;
            }
            case END:
                CounterStore.stop(c);
                CounterNotifier.cancelIv(c);
                buzz(c, 200);
                break;
            case DISMISS:
                // 밀어서 지워도 측정 중(회기 열림)이면 다시 띄운다. 대기 알림은 지우면 다시 띄우지 않는다(앱에서 앱 밖 측정을 새로 켜면 다시 보임)
                CounterStore.dismissWait(c);
                break;
            default:
                return;
        }
        CounterNotifier.refreshAll(c);
        PbsCounterPlugin.fire();
    }

    @SuppressWarnings("deprecation")
    static void buzz(Context c, long ms) {
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(ms);
        } catch (Exception ignored) {}
    }
}
