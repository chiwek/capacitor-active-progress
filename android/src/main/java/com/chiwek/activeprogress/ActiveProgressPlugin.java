package com.chiwek.activeprogress;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.concurrent.ConcurrentHashMap;

@CapacitorPlugin(name = "ActiveProgress")
public class ActiveProgressPlugin extends Plugin {

    private static final String DEFAULT_CHANNEL = "active_progress";
    private final ConcurrentHashMap<String, Integer> idMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> progressMap = new ConcurrentHashMap<>();
    private int nextId = 1001;

    @Override
    public void load() {
        createChannel(DEFAULT_CHANNEL, "Active Progress");
    }

    @PluginMethod
    public void start(PluginCall call) {
        String orderId = call.getString("orderId");
        if (orderId == null || orderId.isEmpty()) { call.reject("orderId required"); return; }

        String title = valOr(call.getString("title"), "Driver on the way");
        String text = valOr(call.getString("text"), "");
        String channelId = valOr(call.getString("channelId"), DEFAULT_CHANNEL);
        boolean ongoing = call.getBoolean("ongoing", true);
        boolean indeterminate = call.getBoolean("indeterminate", false);
        String smallIconName = valOr(call.getString("smallIcon"), "ic_launcher");
        String accent = call.getString("accentColor");

        NotificationManager nm = nm();
        int notiId = idMap.computeIfAbsent(orderId, k -> nextId++);

        NotificationCompat.Builder b = new NotificationCompat.Builder(getContext(), channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(iconRes(smallIconName))
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (accent != null && !accent.isEmpty()) {
            try { b.setColor(Color.parseColor(accent)); } catch (Exception ignored) {}
        }

        if (indeterminate) {
            b.setProgress(0, 0, true);
        } else {
            int p = clamp(call.getInt("progress", 0), 0, 100);
            progressMap.put(orderId, p);
            b.setProgress(100, p, false);
        }

        nm.notify(notiId, b.build());
        call.resolve();
    }

    @PluginMethod
    public void update(PluginCall call) {
        String orderId = call.getString("orderId");
        if (orderId == null || orderId.isEmpty()) { call.reject("orderId required"); return; }
        Integer id = idMap.get(orderId);
        if (id == null) { call.resolve(); return; }

        Notification existing = find(id);
        String ch = (existing != null && Build.VERSION.SDK_INT >= 26 && existing.getChannelId() != null)
                  ? existing.getChannelId() : DEFAULT_CHANNEL;

        int smallIcon = (existing != null && existing.getSmallIcon() != null)
                      ? existing.getSmallIcon().getResId()
                      : iconRes("ic_launcher");

        boolean wasOngoing = existing != null && (existing.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        String prevTitle = existing != null ? existing.extras.getString(Notification.EXTRA_TITLE) : null;
        String prevText  = existing != null ? existing.extras.getString(Notification.EXTRA_TEXT) : null;

        String title = valOr(call.getString("title"), prevTitle);
        String text  = valOr(call.getString("text"),  prevText);

        NotificationCompat.Builder b = new NotificationCompat.Builder(getContext(), ch)
            .setSmallIcon(smallIcon)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(wasOngoing)
            .setContentTitle(title)
            .setContentText(text);

        if (call.getData().has("progress")) {
            int p = clamp(call.getInt("progress", 0), 0, 100);
            progressMap.put(orderId, p);
            b.setProgress(100, p, false);
        } else {
            Integer prev = progressMap.get(orderId);
            if (prev != null) b.setProgress(100, clamp(prev, 0, 100), false);
        }

        nm().notify(id, b.build());
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        String orderId = call.getString("orderId");
        if (orderId == null || orderId.isEmpty()) { call.reject("orderId required"); return; }
        Integer id = idMap.remove(orderId);
        progressMap.remove(orderId);
        if (id != null) nm().cancel(id);
        call.resolve();
    }

    // Helpers
    private NotificationManager nm() {
        return (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }
    private void createChannel(String id, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm().createNotificationChannel(new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH));
        }
    }
    private Notification find(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (android.service.notification.StatusBarNotification sbn : nm().getActiveNotifications()) {
                if (sbn.getId() == id) return sbn.getNotification();
            }
        }
        return null;
    }
    private int iconRes(String name) {
        Context c = getContext();
        int r = c.getResources().getIdentifier(name, "mipmap", c.getPackageName());
        if (r == 0) r = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
        if (r == 0) r = android.R.drawable.stat_sys_download;
        return r;
    }
    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static String valOr(String v, String d) { return (v == null || v.isEmpty()) ? d : v; }
}
