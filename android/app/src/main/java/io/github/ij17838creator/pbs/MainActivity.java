package io.github.ij17838creator.pbs;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(PbsCounterPlugin.class);
        registerPlugin(PbsAuthPlugin.class);
        // 인터넷에서 받아 둔 새 화면이 있으면 이번 실행부터 쓴다 (WebUpdater 참고)
        WebUpdater.boot(this);
        super.onCreate(savedInstanceState);
    }
}
