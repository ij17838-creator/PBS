package io.github.ij17838creator.pbs;

import android.os.Bundle;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(PbsCounterPlugin.class);
        registerPlugin(PbsAuthPlugin.class);
        // 인터넷에서 받아 둔 새 화면이 있으면 이번 실행부터 쓴다 (WebUpdater 참고)
        WebUpdater.boot(this);
        super.onCreate(savedInstanceState);
        // 뒤로 가기: 앱을 바로 끄지 않고 화면(index.html)의 pbsBack()에 먼저 묻는다.
        // 열린 창 닫기 → 앞 화면으로 → 처음 화면에서는 한 번 더 누르면 앱을 뒤로 보냄(회기·기록은 그대로).
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                WebView wv = getBridge() != null ? getBridge().getWebView() : null;
                if (wv == null) { moveTaskToBack(true); return; }
                wv.evaluateJavascript(
                    "(function(){try{return !!(window.pbsBack&&window.pbsBack());}catch(e){return false;}})()",
                    v -> { if (!"true".equals(v)) moveTaskToBack(true); });
            }
        });
    }
}
