package com.aurora.home;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Checks the project's public release feed for a newer APK and falls back to
 * the APK stored on the main branch when no release asset exists.
 *
 * The user-facing strings stay neutral; repository details never appear in
 * the Settings UI.
 */
public final class UpdateChecker {

    private static final String RELEASE_API =
            "https://api.github.com/repos/learnerarchitecture-debug/Fire-tv-/releases/latest";
    private static final String RAW_APK =
            "https://raw.githubusercontent.com/learnerarchitecture-debug/Fire-tv-/main/OBAAHome.apk";
    private static final String UPDATE_FILE = "OBAAHome-update.apk";

    public static final class Update {
        public final boolean available;
        public final String version;
        public final File file;

        Update(boolean available, String version, File file) {
            this.available = available;
            this.version = version;
            this.file = file;
        }
    }

    public interface Callback {
        void onResult(Update update);
        void onError(String message);
    }

    private UpdateChecker() {}

    public static void check(final Context context, final Callback callback) {
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Update update = checkNow(app);
                    if (callback != null) callback.onResult(update);
                } catch (Throwable t) {
                    if (callback != null) callback.onError(friendly(t));
                }
            }
        }, "update-check").start();
    }

    private static Update checkNow(Context context) throws Exception {
        String tag = "";
        String apkUrl = "";

        String json = releaseJson();
        if (json != null) {
            try {
                JSONObject release = new JSONObject(json);
                tag = release.optString("tag_name", "").trim();
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset == null) continue;
                        String name = asset.optString("name", "");
                        if (name.toLowerCase(Locale.US).endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url", "");
                            if (apkUrl.length() > 0) break;
                        }
                    }
                }
            } catch (Throwable ignored) {
                // Malformed or rate-limited response: use the raw fallback.
            }
        }

        boolean fromRelease = apkUrl.length() > 0;
        if (!fromRelease) {
            apkUrl = RAW_APK;
            tag = "latest";
        }

        File apk = download(context, apkUrl);
        String remoteHash = sha256(apk);
        String installedHash = installedApkHash(context);
        boolean available = installedHash == null || !installedHash.equals(remoteHash);

        String version = displayVersion(tag);
        if (!available) {
            apk.delete();
            return new Update(false, version, null);
        }
        return new Update(true, version, apk);
    }

    private static String releaseJson() {
        HttpURLConnection conn = null;
        try {
            conn = open(RELEASE_API);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            if (conn.getResponseCode() != 200) return null;
            return readText(conn.getInputStream());
        } catch (Throwable t) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static File download(Context context, String url) throws Exception {
        File dir = null;
        try {
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } catch (Throwable ignored) {
        }
        if (dir == null || (!dir.exists() && !dir.mkdirs()) || !dir.canWrite()) {
            dir = context.getCacheDir();
        }
        File out = new File(dir, UPDATE_FILE);
        if (out.exists() && !out.delete()) {
            out = new File(context.getCacheDir(), UPDATE_FILE);
        }
        if (out.exists()) out.delete();

        HttpURLConnection conn = null;
        InputStream in = null;
        FileOutputStream fos = null;
        try {
            conn = open(url);
            int code = conn.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            in = new BufferedInputStream(conn.getInputStream());
            fos = new FileOutputStream(out);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
        } finally {
            if (fos != null) try { fos.close(); } catch (Throwable ignored) {}
            if (in != null) try { in.close(); } catch (Throwable ignored) {}
            if (conn != null) conn.disconnect();
        }
        if (!out.exists() || out.length() < 1024) {
            out.delete();
            throw new IOException("Empty download");
        }
        return out;
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "OBAA-Home/2.0");
        return conn;
    }

    private static String readText(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
            return out.toString("UTF-8");
        } finally {
            try { in.close(); } catch (Throwable ignored) {}
        }
    }

    private static String installedApkHash(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), 0);
            if (info == null || info.applicationInfo == null
                    || info.applicationInfo.sourceDir == null) {
                return null;
            }
            return sha256(new File(info.applicationInfo.sourceDir));
        } catch (Throwable t) {
            return null;
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream in = new FileInputStream(file);
        try {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        } finally {
            try { in.close(); } catch (Throwable ignored) {}
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String displayVersion(String tag) {
        if (tag == null || tag.length() == 0 || "latest".equalsIgnoreCase(tag)) {
            return "latest build";
        }
        if (tag.startsWith("v") || tag.startsWith("V")) return tag.substring(1);
        return tag;
    }

    private static String friendly(Throwable t) {
        String message = t == null ? "" : String.valueOf(t.getMessage());
        String lower = message.toLowerCase(Locale.US);
        if (lower.contains("timed out") || lower.contains("timeout")) {
            return "The update server took too long to respond. Try again.";
        }
        if (lower.contains("http 403") || lower.contains("rate")) {
            return "The update service is busy right now. Try again in a few minutes.";
        }
        return "Couldn't reach the update server. Check the connection and try again.";
    }
}
