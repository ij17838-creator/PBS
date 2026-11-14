package io.github.ij17838creator.pbs;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(PbsCounterPlugin.class);
        registerPlugin(PbsAuthPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
