package io.github.ij17838creator.pbs;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;

/**
 * 'PBS 측정 시작' 바로가기 (앱 서랍에 따로 보이는 아이콘).
 * 갤럭시 설정 → 유용한 기능 → 측면 버튼 → 두 번 누르기 → 앱 열기에 이걸 고르면,
 * 화면 없이 바로 '버튼 3번'과 같은 일(회기 없으면 시작, 있으면 방법별 기본 버튼)을 하고 진동으로 알린 뒤 닫힌다.
 * 누름 감지는 갤럭시 시스템이 하므로 앱이 백그라운드여도 된다. 잠금 화면에서 바로 열리는지는 기기 설정에 따라 다르다[추정].
 */
public class QuickStartActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true);
        try {
            int fb = PressWatch.act(this);
            PressWatch.log(this, "측면 바로가기 → " + (fb == PressWatch.FB_START ? "시작" : fb == PressWatch.FB_TICK ? "＋1"
                : fb == PressWatch.FB_END ? "끝" : "기록 안 됨" + (CounterStore.anything(this) ? "(이 방법은 시작만)" : "(측정 대상 없음)")));
            PressWatch.feedback(this, fb);
            CounterNotifier.refreshAll(this);
            PbsCounterPlugin.fire();
        } catch (Exception ignored) {}
        finish();
    }
}
