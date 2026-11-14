package io.github.ij17838creator.pbs;

import android.Manifest;
import android.app.StatusBarManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.graphics.drawable.Icon;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import androidx.core.app.NotificationManagerCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.json.JSONArray;

/**
 * 웹앱(index.html)이 window.Capacitor.Plugins.PbsCounter 로 부르는 기능.
 * start/stop/status/setCount/drain: 알림창 ＋1 버튼, setTarget/clearTarget/pinWidget/addTile: 앱 밖 측정(위젯·타일), saveFile: 백업 파일을 '다운로드/PBS'에 저장.
 */
@CapacitorPlugin(
    name = "PbsCounter",
    permissions = { @Permission(alias = "notifications", strings = { Manifest.permission.POST_NOTIFICATIONS }) }
)
public class PbsCounterPlugin extends Plugin {
    private static volatile PbsCounterPlugin live;

    @Override
    public void load() {
        live = this;
        // 앱이 꺼졌다 다시 켜져도 측정 중이면 알림을 다시 띄운다
        CounterNotifier.refreshAll(getContext());
    }

    @Override
    protected void handleOnDestroy() {
        if (live == this) live = null;
    }

    static void fire() {
        PbsCounterPlugin p = live;
        if (p == null) return;
        new Handler(Looper.getMainLooper()).post(() -> p.notifyListeners("tap", new JSObject()));
    }

    private JSObject state() {
        Context c = getContext();
        JSObject o = new JSObject();
        o.put("active", CounterStore.active(c));
        o.put("key", CounterStore.key(c));
        o.put("count", CounterStore.count(c));
        o.put("enabled", NotificationManagerCompat.from(c).areNotificationsEnabled());
        o.put("mode", CounterStore.mode(c));
        o.put("running", CounterStore.running(c));
        JSObject t = new JSObject();
        t.put("on", CounterStore.hasTarget(c));
        t.put("pid", CounterStore.tPid(c));
        t.put("ph", CounterStore.tPh(c));
        t.put("mode", CounterStore.tMode(c));
        t.put("title", CounterStore.tTitle(c));
        o.put("target", t);
        // 2 = 끝 확인·늦은 시작 보정이 있는 앱 (웹 화면이 먼저 업데이트돼도 옛 앱에선 설정 칸을 숨기려고)
        o.put("ver", 2);
        JSObject k = new JSObject();
        k.put("sec", CounterStore.chkSec(c));
        k.put("mode", CounterStore.chkMode(c));
        k.put("miss", CounterStore.chkMiss(c));
        k.put("adj", CounterStore.adjOn(c));
        o.put("chk", k);
        return o;
    }

    @PluginMethod
    public void start(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notifications") != PermissionState.GRANTED) {
            requestPermissionForAlias("notifications", call, "notifPerm");
            return;
        }
        doStart(call);
    }

    @PermissionCallback
    private void notifPerm(PluginCall call) {
        if (getPermissionState("notifications") != PermissionState.GRANTED) {
            call.reject("알림 권한이 꺼져 있습니다", "denied");
            return;
        }
        doStart(call);
    }

    private void doStart(PluginCall call) {
        String key = call.getString("key", "");
        if (key == null || key.isEmpty()) { call.reject("key가 없습니다"); return; }
        Context c = getContext();
        CounterStore.start(c, key, call.getString("title", "PBS 기록"), call.getInt("count", 0));
        boolean shown = CounterNotifier.show(c);
        if (!shown) {
            CounterStore.stop(c);
            CounterNotifier.refreshAll(c);
            call.reject("알림을 띄울 수 없습니다. 설정에서 이 앱의 알림을 켜 주세요", "blocked");
            return;
        }
        CounterNotifier.refreshAll(c);
        call.resolve(state());
    }

    @PluginMethod
    public void stop(PluginCall call) {
        CounterStore.stop(getContext());
        CounterNotifier.refreshAll(getContext());
        call.resolve(state());
    }

    @PluginMethod
    public void status(PluginCall call) {
        call.resolve(state());
    }

    @PluginMethod
    public void setCount(PluginCall call) {
        Context c = getContext();
        CounterStore.setCount(c, call.getString("key", ""), call.getInt("count", 0));
        if (CounterStore.active(c)) CounterNotifier.show(c);
        call.resolve(state());
    }

    /** 앱 밖 측정 대상 정하기: {pid, ph:'bl'|'iv', title, mode:'freq'|'dur'}. 알림 권한이 없어도 위젯·타일은 된다 */
    @PluginMethod
    public void setTarget(PluginCall call) {
        if (Build.VERSION.SDK_INT >= 33 && getPermissionState("notifications") != PermissionState.GRANTED) {
            requestPermissionForAlias("notifications", call, "targetPerm");
            return;
        }
        doTarget(call);
    }

    @PermissionCallback
    private void targetPerm(PluginCall call) {
        doTarget(call);
    }

    private void doTarget(PluginCall call) {
        String pid = call.getString("pid", "");
        String ph = call.getString("ph", "");
        if (pid == null || pid.isEmpty() || ph == null || ph.isEmpty()) { call.reject("측정 대상이 없습니다"); return; }
        Context c = getContext();
        CounterStore.setTarget(c, pid, ph, call.getString("title", "PBS 기록"), call.getString("mode", CounterStore.FREQ));
        CounterNotifier.refreshAll(c);
        call.resolve(state());
    }

    /** 끝 확인 설정: {sec:간격(초), mode:'vib'|'sound'|'both'|'quiet'|'off', miss:응답 없이 몇 번이면 자동 끝(0=안 끝냄), adj:늦은 시작 보정 버튼} */
    @PluginMethod
    public void setCheck(PluginCall call) {
        Context c = getContext();
        CounterStore.setCheck(c, call.getInt("sec", 30), call.getString("mode", CounterStore.CHK_VIB),
            call.getInt("miss", 3), Boolean.TRUE.equals(call.getBoolean("adj", true)));
        CounterNotifier.refreshAll(c);
        call.resolve(state());
    }

    @PluginMethod
    public void clearTarget(PluginCall call) {
        CounterStore.clearTarget(getContext());
        CounterNotifier.refreshAll(getContext());
        call.resolve(state());
    }

    /** 홈 화면에 위젯 놓기 창 띄우기 (런처가 지원할 때만) */
    @PluginMethod
    public void pinWidget(PluginCall call) {
        JSObject o = new JSObject();
        boolean ok = false;
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                AppWidgetManager m = getContext().getSystemService(AppWidgetManager.class);
                if (m != null && m.isRequestPinAppWidgetSupported()) {
                    ok = m.requestPinAppWidget(new ComponentName(getContext(), CounterWidget.class), null, null);
                }
            }
        } catch (Exception ignored) {}
        o.put("ok", ok);
        call.resolve(o);
    }

    /** 빠른 설정에 타일 추가 창 띄우기 (안드로이드 13 이상) */
    @PluginMethod
    public void addTile(PluginCall call) {
        JSObject o = new JSObject();
        if (Build.VERSION.SDK_INT < 33) { o.put("ok", false); call.resolve(o); return; }
        try {
            StatusBarManager sb = getContext().getSystemService(StatusBarManager.class);
            if (sb == null) { o.put("ok", false); call.resolve(o); return; }
            sb.requestAddTileService(
                new ComponentName(getContext(), CounterTile.class),
                getContext().getString(R.string.tile_label),
                Icon.createWithResource(getContext(), R.drawable.ic_stat_pbs),
                getContext().getMainExecutor(),
                r -> {
                    JSObject res = new JSObject();
                    res.put("ok", r == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED || r == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED);
                    res.put("already", r == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED);
                    call.resolve(res);
                });
        } catch (Exception e) {
            o.put("ok", false);
            call.resolve(o);
        }
    }

    /* ---------- 화면 자동 업데이트 (WebUpdater) ---------- */

    /** 인터넷에서 새 화면이 있는지 보고 받아 둔다: {force} → {pending, version} */
    @PluginMethod
    public void webCheck(PluginCall call) {
        boolean force = Boolean.TRUE.equals(call.getBoolean("force", false));
        Context c = getContext();
        new Thread(() -> {
            String p = WebUpdater.check(c, force);
            JSObject o = new JSObject();
            o.put("pending", !p.isEmpty());
            o.put("version", WebUpdater.version(c));
            call.resolve(o);
        }).start();
    }

    /** 받아 둔 새 화면을 지금 바로 쓴다(화면을 다시 불러옴) */
    @PluginMethod
    public void webApply(PluginCall call) {
        JSObject o = new JSObject();
        o.put("ok", WebUpdater.applyNow(getContext(), getBridge()));
        call.resolve(o);
    }

    /** 웹 화면이 다 열렸다는 신호 (받은 화면이 고장 나면 설치본으로 돌아가는 안전장치용) */
    @PluginMethod
    public void webReady(PluginCall call) {
        WebUpdater.ready(getContext());
        JSObject o = new JSObject();
        o.put("version", WebUpdater.version(getContext()));
        o.put("pending", !WebUpdater.pending(getContext()).isEmpty());
        call.resolve(o);
    }

    @PluginMethod
    public void drain(PluginCall call) {
        JSONArray ev = CounterStore.drain(getContext());
        JSObject o = state();
        try { o.put("events", new JSArray(ev.toString())); }
        catch (Exception e) { o.put("events", new JSArray()); }
        call.resolve(o);
    }

    @PluginMethod
    public void saveFile(PluginCall call) {
        String name = call.getString("filename", "pbs-backup.json");
        String mime = call.getString("mime", "application/octet-stream");
        String b64 = call.getString("data", "");
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            String where;
            if (Build.VERSION.SDK_INT >= 29) {
                ContentResolver cr = getContext().getContentResolver();
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PBS");
                Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("저장 위치를 만들지 못했습니다");
                try (OutputStream os = cr.openOutputStream(uri)) {
                    if (os == null) throw new Exception("파일을 열지 못했습니다");
                    os.write(bytes);
                }
                where = "다운로드/PBS/" + name;
            } else {
                File dir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "PBS");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("폴더를 만들지 못했습니다");
                File f = new File(dir, name);
                try (FileOutputStream os = new FileOutputStream(f)) { os.write(bytes); }
                where = f.getAbsolutePath();
            }
            JSObject o = new JSObject();
            o.put("path", where);
            call.resolve(o);
        } catch (Exception e) {
            call.reject(e.getMessage() == null ? "파일을 저장하지 못했습니다" : e.getMessage());
        }
    }
}
