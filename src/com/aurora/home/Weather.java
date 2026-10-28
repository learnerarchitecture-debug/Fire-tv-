package com.aurora.home;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Weather via Open-Meteo (free, no API key). First run auto-detects an
 * approximate location via ip-api.com; the user can set an exact city
 * in Settings. Results are cached in SharedPreferences so the home
 * screen renders instantly on launch.
 */
public final class Weather {

    public static class Data {
        public float temp = Float.NaN;
        public int code = -1;
        public boolean day = true;
        public float tmax = Float.NaN;
        public float tmin = Float.NaN;
        public String place = "";
        public String unit = "c";
        public long fetched;

        public boolean valid() {
            return !Float.isNaN(temp);
        }
    }

    public interface Callback {
        void onWeather(Data d);
    }

    private Weather() {}

    public static Data loadCache(Context c) {
        try {
            String s = Prefs.get(c, Prefs.K_WX_CACHE, null);
            if (s == null) return null;
            JSONObject o = new JSONObject(s);
            Data d = new Data();
            d.temp = (float) o.optDouble("t", Double.NaN);
            d.code = o.optInt("c", -1);
            d.day = o.optBoolean("d", true);
            d.tmax = (float) o.optDouble("tx", Double.NaN);
            d.tmin = (float) o.optDouble("tn", Double.NaN);
            d.place = o.optString("p", "");
            d.unit = o.optString("u", "c");
            d.fetched = o.optLong("f", 0);
            return d;
        } catch (Throwable t) {
            return null;
        }
    }

    private static void saveCache(Context c, Data d) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", (double) d.temp);
            o.put("c", d.code);
            o.put("d", d.day);
            if (!Float.isNaN(d.tmax)) o.put("tx", (double) d.tmax);
            if (!Float.isNaN(d.tmin)) o.put("tn", (double) d.tmin);
            o.put("p", d.place);
            o.put("u", d.unit);
            o.put("f", d.fetched);
            Prefs.put(c, Prefs.K_WX_CACHE, o.toString());
        } catch (Throwable ignored) {
        }
    }

    public static void refresh(final Context ctx, final Callback cb) {
        final Handler main = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                Data d = null;
                try {
                    ensureLocation(ctx);
                    double lat = Double.parseDouble(Prefs.get(ctx, Prefs.K_LAT, ""));
                    double lon = Double.parseDouble(Prefs.get(ctx, Prefs.K_LON, ""));
                    String unit = Prefs.get(ctx, Prefs.K_TEMP_UNIT, "c");
                    String u = "f".equals(unit) ? "fahrenheit" : "celsius";
                    String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                            + "&longitude=" + lon
                            + "&current=temperature_2m,weather_code,is_day"
                            + "&daily=temperature_2m_max,temperature_2m_min"
                            + "&timezone=auto&temperature_unit=" + u;
                    JSONObject o = httpJson(url);
                    Data nd = new Data();
                    JSONObject cur = o.getJSONObject("current");
                    nd.temp = (float) cur.getDouble("temperature_2m");
                    nd.code = cur.optInt("weather_code", 0);
                    nd.day = cur.optInt("is_day", 1) == 1;
                    JSONObject dl = o.optJSONObject("daily");
                    if (dl != null) {
                        JSONArray mx = dl.optJSONArray("temperature_2m_max");
                        JSONArray mn = dl.optJSONArray("temperature_2m_min");
                        if (mx != null && mx.length() > 0) nd.tmax = (float) mx.getDouble(0);
                        if (mn != null && mn.length() > 0) nd.tmin = (float) mn.getDouble(0);
                    }
                    nd.place = Prefs.get(ctx, Prefs.K_CITY, "");
                    nd.unit = unit;
                    nd.fetched = System.currentTimeMillis();
                    saveCache(ctx, nd);
                    d = nd;
                } catch (Throwable t) {
                    d = null;
                }
                if (d == null) d = loadCache(ctx);
                final Data fd = d;
                main.post(new Runnable() {
                    @Override
                    public void run() {
                        cb.onWeather(fd);
                    }
                });
            }
        }, "weather").start();
    }

    private static void ensureLocation(Context ctx) throws Exception {
        if (Prefs.get(ctx, Prefs.K_LAT, "").length() > 0) return;
        try {
            JSONObject ip = httpJson("http://ip-api.com/json/?fields=status,lat,lon,city");
            if ("success".equals(ip.optString("status"))) {
                double lat = ip.getDouble("lat");
                double lon = ip.getDouble("lon");
                String city = ip.optString("city", "");
                if (city == null || city.length() == 0) city = "Home";
                Prefs.put(ctx, Prefs.K_LAT, String.valueOf(lat));
                Prefs.put(ctx, Prefs.K_LON, String.valueOf(lon));
                Prefs.put(ctx, Prefs.K_CITY, city);
                return;
            }
        } catch (Throwable ignored) {
        }
        Prefs.put(ctx, Prefs.K_LAT, "40.7128");
        Prefs.put(ctx, Prefs.K_LON, "-74.0060");
        Prefs.put(ctx, Prefs.K_CITY, "New York");
    }

    public static void geocodeCity(Context ctx, String city) throws Exception {
        JSONObject o = httpJson("https://geocoding-api.open-meteo.com/v1/search?name="
                + URLEncoder.encode(city, "UTF-8") + "&count=1");
        JSONArray arr = o.optJSONArray("results");
        if (arr == null || arr.length() == 0) {
            throw new Exception("City not found");
        }
        JSONObject r = arr.getJSONObject(0);
        Prefs.put(ctx, Prefs.K_LAT, String.valueOf(r.getDouble("latitude")));
        Prefs.put(ctx, Prefs.K_LON, String.valueOf(r.getDouble("longitude")));
        Prefs.put(ctx, Prefs.K_CITY, r.optString("name", city));
    }

    private static JSONObject httpJson(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            c.setRequestProperty("User-Agent", "OBAAHome/2.0");
            int code = c.getResponseCode();
            if (code != 200) throw new IOException("HTTP " + code);
            BufferedReader r = new BufferedReader(
                    new InputStreamReader(c.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return new JSONObject(sb.toString());
        } finally {
            c.disconnect();
        }
    }

    public static String condition(int code) {
        switch (code) {
            case 0: return "Clear sky";
            case 1: return "Mainly clear";
            case 2: return "Partly cloudy";
            case 3: return "Overcast";
            case 45: case 48: return "Foggy";
            case 51: case 53: case 55: return "Drizzle";
            case 56: case 57: return "Freezing drizzle";
            case 61: return "Light rain";
            case 63: return "Rain";
            case 65: return "Heavy rain";
            case 66: case 67: return "Freezing rain";
            case 71: case 73: case 75: return "Snow";
            case 77: return "Snow grains";
            case 80: return "Light showers";
            case 81: return "Showers";
            case 82: return "Heavy showers";
            case 85: case 86: return "Snow showers";
            case 95: return "Thunderstorm";
            case 96: case 99: return "Thunderstorm";
            default: return "Cloudy";
        }
    }

    public static int iconKind(int code, boolean day) {
        if (code == 0) return day ? WeatherIconView.SUN : WeatherIconView.MOON;
        if (code == 1 || code == 2) return day
                ? WeatherIconView.PARTLY_DAY : WeatherIconView.PARTLY_NIGHT;
        if (code == 3) return WeatherIconView.CLOUD;
        if (code == 45 || code == 48) return WeatherIconView.FOG;
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return WeatherIconView.RAIN;
        if ((code >= 71 && code <= 77) || code == 85 || code == 86) return WeatherIconView.SNOW;
        if (code >= 95) return WeatherIconView.STORM;
        return WeatherIconView.CLOUD;
    }
}
