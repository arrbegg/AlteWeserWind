package dev.max.alteweserwind;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WindUpdateWorker extends Worker {

  private static final String STATION_UUID = "c6772c3c-a6bb-4728-9250-a408ab3856bd";
  private static final String URL = "https://www.pegelonline.wsv.de/webservices/rest-api/v2/stations/" + STATION_UUID + ".json?includeTimeseries=true&includeCurrentMeasurement=true";

  private final OkHttpClient client = new OkHttpClient();

  public WindUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
  }

  public static void enqueueNow(Context context){
    OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(WindUpdateWorker.class).build();
    WorkManager.getInstance(context).enqueueUniqueWork("WindUpdateNow", ExistingWorkPolicy.REPLACE, req);
  }

  @NonNull
  @Override
  public Result doWork() {
    try {
      String body = fetch(URL);
      if (body == null) return Result.retry();

      JSONObject station = new JSONObject(body);
      JSONArray ts = station.optJSONArray("timeseries");
      if (ts == null) return Result.retry();

      JSONObject windTs = pickWindTs(ts);
      JSONObject dirTs = pickDirTs(ts);

      if (windTs == null) return Result.retry();
      JSONObject cm = windTs.optJSONObject("currentMeasurement");
      if (cm == null) return Result.retry();

      double v = cm.getDouble("value");
      double kmh = v * 3.6;
      double kn = v * 1.943844;

      Beaufort b = Beaufort.fromMs(v);
      int color = colorForBeaufort(b.n);

      int degFrom = -1;
      if (dirTs != null && dirTs.optJSONObject("currentMeasurement") != null) {
        degFrom = (int)Math.round(dirTs.optJSONObject("currentMeasurement").getDouble("value") % 360);
        if (degFrom < 0) degFrom += 360;
      }
      int degTo = (degFrom >= 0) ? (degFrom + 180) % 360 : -1;
      String arrow = arrow8(degTo);

      // Build RemoteViews
      RemoteViews views = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_wind);
      views.setTextViewText(R.id.txtSpeed, String.format(Locale.US, "%.1f m/s", v));
      views.setTextColor(R.id.txtSpeed, color);

      String detail = String.format(Locale.US, "Bft %d (%.1f–%s m/s)", b.n, b.lo, (Double.isInfinite(b.hi)? "∞": String.format(Locale.US, "%.1f", b.hi)));
      detail += String.format(Locale.US, " • %s %s", arrow, (degFrom >= 0 ? (degFrom + "°") : "–"));
      views.setTextViewText(R.id.txtDetail, detail);
      views.setTextColor(R.id.txtDetail, color);

      Calendar c = Calendar.getInstance();
      String time = DateFormat.format("dd.MM.yyyy HH:mm", c).toString();
      views.setTextViewText(R.id.txtUpdated, "Updated: " + time);

      WindWidgetProvider.updateAll(getApplicationContext(), views);
      return Result.success();
    } catch (Exception e) {
      e.printStackTrace();
      return Result.retry();
    }
  }

  private String fetch(String url) throws IOException {
    Request req = new Request.Builder().url(url).header("Accept", "application/json").build();
    try (Response resp = client.newCall(req).execute()) {
      if (!resp.isSuccessful()) return null;
      return resp.body() != null ? resp.body().string() : null;
    }
  }

  private JSONObject pickWindTs(JSONArray arr){
    for (int i=0;i<arr.length();i++){
      JSONObject t = arr.optJSONObject(i);
      String name = ((t.optString("shortname","")) + " " + t.optString("longname","")).toLowerCase(Locale.US);
      String unit = t.optString("unit","").toLowerCase(Locale.US);
      if (name.contains("wind") || name.contains("windgeschwindigkeit") || unit.equals("m/s")){
        return t;
      }
    }
    return null;
  }

  private JSONObject pickDirTs(JSONArray arr){
    for (int i=0;i<arr.length();i++){
      JSONObject t = arr.optJSONObject(i);
      String name = ((t.optString("shortname","")) + " " + t.optString("longname","")).toLowerCase(Locale.US);
      String unit = t.optString("unit","").toLowerCase(Locale.US);
      if (name.contains("wind") && (unit.contains("grad") || unit.contains("°"))) return t;
    }
    return null;
  }

  private static class Beaufort {
    final int n; final double lo; final double hi;
    Beaufort(int n, double lo, double hi){ this.n=n; this.lo=lo; this.hi=hi; }
    static Beaufort fromMs(double v){
      double[][] t = {
        {0,0.0,0.2},{1,0.3,1.5},{2,1.6,3.3},{3,3.4,5.4},{4,5.5,7.9},
        {5,8.0,10.7},{6,10.8,13.8},{7,13.9,17.1},{8,17.2,20.7},{9,20.8,24.4},
        {10,24.5,28.4},{11,28.5,32.6},{12,32.7,9999}
      };
      for (double[] b: t){
        if (v < 0.3 && b[0]==0) return new Beaufort((int)b[0], b[1], b[2]);
        if (v >= b[1] && v <= b[2]) return new Beaufort((int)b[0], b[1], b[2]);
      }
      return new Beaufort(12,32.7,9999);
    }
  }

  private int colorForBeaufort(int n){
    if (n <= 2) return Color.parseColor("#60a5fa");
    if (n <= 4) return Color.parseColor("#22c55e");
    if (n == 5) return Color.parseColor("#eab308");
    if (n == 6) return Color.parseColor("#f97316");
    if (n <= 9) return Color.parseColor("#ef4444");
    return Color.parseColor("#a21caf");
  }

  // Map degrees to 8-way arrow, using "to" direction
  private String arrow8(int deg){
    if (deg < 0) return "↑";
    int idx = (int)Math.round(deg / 45.0) % 8;
    String[] arr = {"↑","↗","→","↘","↓","↙","←","↖"};
    return arr[idx];
  }
}