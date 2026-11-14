package io.github.ij17838creator.pbs;

import android.Manifest;
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
 * start/stop/status/setCount/drain: 알림창 ＋1 버튼, saveFile: 백업 파일을 '다운로드/PBS'에 저장.
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
        if (CounterStore.active(getContext())) CounterNotifier.show(getContext());
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
            call.reject("알림을 띄울 수 없습니다. 설정에서 이 앱의 알림을 켜 주세요", "blocked");
            return;
        }
        call.resolve(state());
    }

    @PluginMethod
    public void stop(PluginCall call) {
        CounterStore.stop(getContext());
        CounterNotifier.cancel(getContext());
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
