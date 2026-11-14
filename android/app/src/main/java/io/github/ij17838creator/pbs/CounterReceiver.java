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
                if (CounterStore.add(c, 1) > 0) buzz(c, 60);
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
            case END:
                CounterStore.stop(c);
                buzz(c, 200);
                break;
            case DISMISS:
                // 밀어서 지워도 측정 중이면 다시 띄운다
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
