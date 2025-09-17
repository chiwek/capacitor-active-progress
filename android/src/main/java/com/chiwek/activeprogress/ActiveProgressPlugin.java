package com.chiwek.activeprogress;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

// IMPORTANT: import the plugin’s own R (matches the namespace!)
import com.chiwek.activeprogress.R;

@CapacitorPlugin(name = "ActiveProgress")
public class ActiveProgressPlugin extends Plugin {

  private static final String DEFAULT_CHANNEL = "active_progress";
  private NotificationManager nm;

  @Override
  public void load() {
    Context c = getContext();
    nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= 26) {
      NotificationChannel ch = new NotificationChannel(
          DEFAULT_CHANNEL, "Active Progress", NotificationManager.IMPORTANCE_LOW);
      nm.createNotificationChannel(ch);
    }
  }

  @PluginMethod
  public void show(PluginCall call) {
    String title = call.getString("title", "Working…");
    String text  = call.getString("text",  "Please wait");
    int progress = call.getInt("progress", 0);

    Context appCtx = getContext();
    String appPkg = appCtx.getPackageName();

    // 1) Try to resolve a host-app override layout first
    int layoutId = safeIdentifier(appCtx, appPkg, "notification_active_progress", "layout");

    RemoteViews content;
    String contentPkg;

    if (layoutId != 0) {
      // host app provided a layout with our expected IDs
      content = new RemoteViews(appPkg, layoutId);
      contentPkg = appPkg;
    } else {
      // fallback to plugin’s default layout
      content = new RemoteViews(BuildConfig.LIBRARY_PACKAGE_NAME,
          R.layout.notification_active_progress);
      contentPkg = BuildConfig.LIBRARY_PACKAGE_NAME;
    }

    // Resolve IDs in whichever package owns the layout
    Resources ownerRes = resourcesFor(contentPkg);
    int idTitle    = id(ownerRes, contentPkg, "ap_title");
    int idText     = id(ownerRes, contentPkg, "ap_text");
    int idProgress = id(ownerRes, contentPkg, "ap_progress");

    content.setTextViewText(idTitle, title);
    content.setTextViewText(idText,  text);
    content.setProgressBar(idProgress, 100, progress, false);

    NotificationCompat.Builder nb =
        new NotificationCompat.Builder(appCtx, DEFAULT_CHANNEL)
            .setSmallIcon(resolveSmallIcon()) // host override or plugin default
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(content);

    nm.notify(10123, nb.build());
    call.resolve();
  }

  private int resolveSmallIcon() {
    // Try host app drawable "ic_stat_notify" first
    Context c = getContext();
    int host = safeIdentifier(c, c.getPackageName(), "ic_stat_notify", "drawable");
    if (host != 0) return host;
    // Fallback to plugin drawable
    return R.drawable.ic_stat_notify;
  }

  private int safeIdentifier(Context ctx, String pkg, String name, String type) {
    try {
      Resources res = ctx.getPackageManager().getResourcesForApplication(pkg);
      return res.getIdentifier(name, type, pkg);
    } catch (PackageManager.NameNotFoundException e) {
      return 0;
    }
  }

  private Resources resourcesFor(String pkg) {
    try {
      return getContext().getPackageManager().getResourcesForApplication(pkg);
    } catch (PackageManager.NameNotFoundException e) {
      return getContext().getResources();
    }
  }

  private int id(Resources res, String pkg, String name) {
    int out = res.getIdentifier(name, "id", pkg);
    if (out == 0) throw new IllegalStateException("Missing id @" + name + " in " + pkg);
    return out;
  }
}
