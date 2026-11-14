package io.github.ij17838creator.pbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** 알림창 버튼을 누르면 앱을 열지 않고 여기서 바로 기록한다 */
public class CounterReceiver extends BroadcastReceiver {
    static final String INC = "io.github.ij17838creator.pbs.COUNTER_INC";
    static final String DEC = "io.github.ij17838creator.pbs.COUNTER_DEC";
    static final String END = "io.github.ij17838creator.pbs.COUNTER_END";
    static final String DISMISS = "io.github.ij17838creator.pbs.COUNTER_DISMISS";

    @Override
    public void onReceive(Context c, Intent intent) {
        String a = intent.getAction();
        if (a == null) return;
        switch (a) {
            case INC:
                CounterStore.add(c, 1);
                buzz(c, 60);
                break;
            case DEC:
                CounterStore.add(c, -1);
                buzz(c, 30);
                break;
            case END:
                CounterStore.stop(c);
                break;
            case DISMISS:
                // 밀어서 지워도 '끝'을 누르기 전까지는 다시 띄운다
                break;
            default:
                return;
        }
        CounterNotifier.show(c);
        PbsCounterPlugin.fire();
    }

    @SuppressWarnings("deprecation")
    private static void buzz(Context c, long ms) {
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            else v.vibrate(ms);
        } catch (Exception ignored) {}
    }
}
