package dev.max.alteweserwind;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class WindWidgetProvider extends AppWidgetProvider {

  public static final String ACTION_REFRESH = "dev.max.alteweserwind.REFRESH";
  private static final String WORK_NAME = "WindUpdatePeriodic";

  @Override
  public void onEnabled(Context context) {
    // Schedule periodic updates every 30 minutes
    PeriodicWorkRequest req = new PeriodicWorkRequest.Builder(WindUpdateWorker.class, 30, TimeUnit.MINUTES)
            .setConstraints(new Constraints.Builder().build())
            .build();
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, req);
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    // Trigger immediate refresh
    WindUpdateWorker.enqueueNow(context);

    // Set click to open app
    for (int appWidgetId : appWidgetIds) {
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_wind);
      Intent i = new Intent(context, MainActivity.class);
      PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
      views.setOnClickPendingIntent(R.id.txtSpeed, pi);
      views.setOnClickPendingIntent(R.id.txtDetail, pi);
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  static void updateAll(Context context, RemoteViews views){
    AppWidgetManager mgr = AppWidgetManager.getInstance(context);
    ComponentName cn = new ComponentName(context, WindWidgetProvider.class);
    mgr.updateAppWidget(cn, views);
  }
}