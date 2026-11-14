package io.github.ij17838creator.pbs;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/**
 * 빠른 설정(화면 위에서 두 번 내리는 버튼 판)의 'PBS 측정' 타일.
 * 빈도면 누를 때마다 ＋1, 지속시간이면 행동 시작/끝. 앱이 꺼져 있어도 된다.
 */
public class CounterTile extends TileService {

    static void refresh(Context c) {
        try { TileService.requestListeningState(c, new ComponentName(c, CounterTile.class)); }
        catch (Exception ignored) {}
    }

    @Override
    public void onStartListening() {
        draw();
    }

    @Override
    public void onClick() {
        Context c = getApplicationContext();
        if (!CounterStore.anything(c)) { openApp(); return; }
        if (CounterStore.DUR.equals(CounterStore.uiMode(c))) CounterReceiver.handle(c, CounterReceiver.DUR);
        else CounterReceiver.handle(c, CounterReceiver.INC);
        draw();
    }

    @SuppressWarnings("deprecation")
    private void openApp() {
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 30, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
            } else {
                startActivityAndCollapse(open);
            }
        } catch (Exception ignored) {}
    }

    private void draw() {
        Tile t = getQsTile();
        if (t == null) return;
        Context c = getApplicationContext();
        boolean any = CounterStore.anything(c);
        boolean on = CounterStore.active(c);
        boolean dur = CounterStore.DUR.equals(CounterStore.uiMode(c));
        String label;
        if (!any) label = "PBS 측정";
        else if (dur) label = on && CounterStore.running(c) ? "PBS 행동 끝" : "PBS 행동 시작";
        else label = on ? "PBS ＋1 (" + CounterStore.count(c) + "회)" : "PBS ＋1";
        t.setLabel(label);
        if (Build.VERSION.SDK_INT >= 29) t.setSubtitle(any ? CounterNotifier.titleText(c) : "앱에서 대상 고르기");
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.updateTile();
    }
}
