package io.github.ij17838creator.pbs;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.os.Build;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 앱 화면(index.html)을 인터넷(GitHub Pages)에서 새로 받아 쓰게 한다. 그래서 화면만 바뀐 업데이트는 앱을 다시 설치하지 않아도 된다.
 *
 * - 받은 파일은 앱 안 폴더(files/web/버전)에 두고, Capacitor의 '화면 폴더 바꾸기'(CapWebViewSettings.serverBasePath)로 쓴다.
 *   주소(https://localhost)는 그대로라서 앱에 저장된 자료(localStorage 등)는 그대로 남는다.
 * - 인터넷이 없으면 마지막으로 받은 화면, 그것도 없으면 설치 파일에 든 화면을 쓴다.
 * - 새 설치 파일(.apk)을 깔면 Capacitor가 받은 화면을 지우고 설치 파일의 화면으로 돌아간다.
 * - 안전장치: 받은 화면이 열린 뒤 '준비됨'(webReady) 신호가 오지 않은 채로 앱이 다시 켜지면 설치 파일 화면으로 돌아가고 그 버전은 다시 받지 않는다.
 */
final class WebUpdater {
    static final String PAGES_URL = "https://ij17838-creator.github.io/PBS/index.html";
    private static final String PREFS = "pbs_web";
    private static final String CAP_PREFS = "CapWebViewSettings";
    private static final String CAP_PATH = "serverBasePath";
    private static final long MIN_GAP_MS = 5 * 60 * 1000L;

    /** 지금 화면이 쓰는 폴더 ("" = 설치 파일에 든 화면) */
    static volatile String active = "";
    private static volatile boolean busy = false;

    private WebUpdater() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static SharedPreferences cap(Context c) {
        return c.getApplicationContext().getSharedPreferences(CAP_PREFS, Activity.MODE_PRIVATE);
    }

    /** MainActivity.onCreate에서 super.onCreate 전에 부른다(그래야 이번 실행에 반영된다) */
    static void boot(Context c) {
        SharedPreferences cp = cap(c);
        String path = cp.getString(CAP_PATH, "");
        if (path == null || path.isEmpty() || !new File(path, "index.html").exists() || newBinary(c, cp)) {
            if (path != null && !path.isEmpty() && !new File(path, "index.html").exists()) cp.edit().putString(CAP_PATH, "").commit();
            active = "";
            prefs(c).edit().putInt("trial", 0).commit();
            return;
        }
        SharedPreferences p = prefs(c);
        if (p.getInt("trial", 0) >= 1) {
            // 지난번에 받은 화면이 제대로 열리지 않았다 → 설치 파일 화면으로 돌아간다
            cp.edit().putString(CAP_PATH, "").commit();
            p.edit().putString("bad", p.getString("sha", "")).putInt("trial", 0).commit();
            active = "";
            return;
        }
        p.edit().putInt("trial", 1).commit();
        active = path;
    }

    /** 웹 화면이 다 열렸다고 알려 올 때 */
    static void ready(Context c) {
        prefs(c).edit().putInt("trial", 0).apply();
    }

    /** Capacitor와 같은 방법으로 '새로 설치했는지' 본다(새로 깔았으면 Capacitor가 받은 화면 설정을 지운다) */
    @SuppressWarnings("deprecation")
    private static boolean newBinary(Context c, SharedPreferences cp) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
            String vc = Integer.toString((int) code);
            String vn = pi.versionName != null ? pi.versionName : "";
            return !vc.equals(cp.getString("lastBinaryVersionCode", null)) || !vn.equals(cp.getString("lastBinaryVersionName", null));
        } catch (Exception e) {
            return false;
        }
    }

    /** 받아 둔 새 화면 폴더 (지금 화면과 같으면 "") */
    static String pending(Context c) {
        String path = cap(c).getString(CAP_PATH, "");
        if (path == null || path.isEmpty() || path.equals(active) || !new File(path, "index.html").exists()) return "";
        return path;
    }

    static String version(Context c) {
        String s = active.isEmpty() ? "" : prefs(c).getString("sha", "");
        return s.isEmpty() ? "설치본" : s.substring(0, Math.min(7, s.length()));
    }

    /** 새 화면이 있는지 확인하고 받아 둔다(다음에 앱을 열 때 적용). 오래 걸리므로 백그라운드에서 부른다. 반환값: 받아 둔 새 화면 폴더 또는 "" */
    static String check(Context c, boolean force) {
        if (busy) return pending(c);
        busy = true;
        try {
            SharedPreferences p = prefs(c);
            long now = System.currentTimeMillis();
            if (!force && now - p.getLong("checked", 0L) < MIN_GAP_MS) return pending(c);
            p.edit().putLong("checked", now).apply();

            byte[] html = download(PAGES_URL + "?t=" + now);
            if (html == null || !looksRight(html)) return pending(c);
            String sha = sha(html);
            if (sha.equals(p.getString("bad", ""))) return pending(c);
            String current = cap(c).getString(CAP_PATH, "");
            boolean usingDownloaded = current != null && !current.isEmpty() && new File(current, "index.html").exists();
            String currentSha = usingDownloaded ? p.getString("sha", "") : bundledSha(c);
            if (sha.equals(currentSha)) return pending(c);

            File root = new File(c.getFilesDir(), "web");
            File dir = new File(root, sha.substring(0, 16));
            if (!dir.exists() && !dir.mkdirs()) return pending(c);
            copyAssets(c.getAssets(), "public", dir);
            File tmp = new File(dir, "index.html.tmp");
            try (OutputStream os = new FileOutputStream(tmp)) { os.write(html); }
            File idx = new File(dir, "index.html");
            if (idx.exists() && !idx.delete()) return pending(c);
            if (!tmp.renameTo(idx)) return pending(c);

            cap(c).edit().putString(CAP_PATH, dir.getAbsolutePath()).commit();
            p.edit().putString("sha", sha).putInt("trial", 0).commit();
            // 지금 쓰는 폴더와 새 폴더 말고는 지운다
            File[] olds = root.listFiles();
            if (olds != null) for (File f : olds) {
                String ap = f.getAbsolutePath();
                if (!ap.equals(dir.getAbsolutePath()) && !ap.equals(active)) deleteTree(f);
            }
            return pending(c);
        } catch (Exception e) {
            return pending(c);
        } finally {
            busy = false;
        }
    }

    /** 받은 새 화면을 지금 바로 쓴다 (화면을 다시 불러온다) */
    static boolean applyNow(Context c, com.getcapacitor.Bridge bridge) {
        String path = pending(c);
        if (path.isEmpty() || bridge == null) return false;
        prefs(c).edit().putInt("trial", 1).commit();
        active = path;
        bridge.setServerBasePath(path);
        return true;
    }

    /* ---------- 도우미 ---------- */

    private static boolean looksRight(byte[] html) {
        if (html.length < 50000) return false;
        String s = new String(html, StandardCharsets.UTF_8);
        return s.contains("pbs-observe:v1") && s.contains("PbsCounter") && s.trim().endsWith("</html>");
    }

    private static byte[] download(String u) {
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) new URL(u).openConnection();
            con.setConnectTimeout(8000);
            con.setReadTimeout(15000);
            con.setUseCaches(false);
            con.setRequestProperty("Cache-Control", "no-cache");
            if (con.getResponseCode() != 200) return null;
            try (InputStream in = con.getInputStream()) { return readAll(in); }
        } catch (Exception e) {
            return null;
        } finally {
            if (con != null) con.disconnect();
        }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        return bo.toByteArray();
    }

    private static String sha(byte[] b) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(b);
        StringBuilder sb = new StringBuilder();
        for (byte x : d) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    private static String bundledSha(Context c) {
        try (InputStream in = c.getAssets().open("public/index.html")) { return sha(readAll(in)); }
        catch (Exception e) { return ""; }
    }

    /** 설치 파일에 든 화면 폴더(assets/public)의 다른 파일도 같이 둔다(index.html은 새로 받은 것으로 덮어씀) */
    private static void copyAssets(AssetManager am, String from, File to) throws Exception {
        String[] list = am.list(from);
        if (list == null) return;
        for (String name : list) {
            String src = from + "/" + name;
            String[] sub = am.list(src);
            if (sub != null && sub.length > 0) {
                File d = new File(to, name);
                if (!d.exists() && !d.mkdirs()) continue;
                copyAssets(am, src, d);
            } else {
                if (name.equals("index.html") && from.equals("public")) continue;
                try (InputStream in = am.open(src); OutputStream os = new FileOutputStream(new File(to, name))) {
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
            }
        }
    }

    private static void deleteTree(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File k : kids) deleteTree(k);
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
