package com.chiwek.activeprogress;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.Map;
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

    // --- Public API ----------------------------------------------------------

    @PluginMethod
    public void start(PluginCall call) {
        String orderId = call.getString("orderId");
        if (orderId == null || orderId.isEmpty()) {
            call.reject("orderId required");
            return;
        }

        String title = getOrDefault(call.getString("title"), "Driver on the way");
        String text = getOrDefault(call.getString("text"), "");
        String channelId = getOrDefault(call.getString("channelId"), DEFAULT_CHANNEL);
        Boolean ongoing = call.getBoolean("ongoing", true);
        Boolean indeterminate = call.getBoolean("indeterminate", false);
        String smallIconName = getOrDefault(call.getString("smallIcon"), "ic_launcher");
        String accent = call.getString("accentColor");

        NotificationManager nm = getNotificationManager();
        int notiId = idMap.computeIfAbsent(orderId, k -> nextId++);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(getIconResId(smallIconName))
                .setOngoing(ongoing != null ? ongoing : true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (accent != null && !accent.isEmpty()) {
            try { builder.setColor(Color.parseColor(accent)); } catch (Exception ignored) {}
        }

        if (indeterminate != null && indeterminate) {
            builder.setProgress(0, 0, true);
        } else {
            Integer p = call.getInt("progress");
            int prog = clamp(p != null ? p : 0, 0, 100);
            progressMap.put(orderId, prog);
            builder.setProgress(100, prog, false);
        }

        nm.notify(notiId, builder.build());
        call.resolve();
    }

    @PluginMethod
    public void update(PluginCall call) {
        String orderId = call.getString("orderId");
        if (orderId == null || orderId.isEmpty()) {
            call.reject("orderId required");
            return;
        }
        Integer id = idMap.get(orderId);
        if (id == null) { call.resolve(); return; }

        NotificationManager nm = getNotificationManager();
        Notification existing = findExistingNotification(id);
        // Build a new one reusing important fields
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getContext(),
                (existing != null && Build.VERSION.SDK_INT >= 26 && existing.getChannelId() != null)
                        ? existing.getChannelId()
                        : DEFAULT_CHANNEL
        );

        int smallIcon = (existing != null && existing.getSmallIcon() != null)
                ? existing.getSmallIcon().getResId()
                : getIconResId("ic_launcher");

        builder.setSmallIcon(smallIcon)
               .setOnlyAlertOnce(true)
               .setPriority(NotificationCompat.PRIORITY_HIGH);

        boolean wasOngoing = existing != null && (existing.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        builder.setOngoing(wasOngoing);

        String newTitle = call.getString("title");
        String newText = call.getString("text");

        String prevTitle = existing != null ? existing.extras.getString(Notification.EXTRA_TITLE) : null;
        String prevText  = existing != null ? existing.extras.getString(Notification.EXTRA_TEXT) : null;

        builder.setContentTitle(newTitle != null ? newTitle : prevTitle);
        builder.setContentText(newText != null ? newText : prevText);

        Integer p = call.getInt("progress");
        if (p != null) {
            int prog = clamp(p, 0, 100);
            progressMap.put(orderId, prog);
            builder.setProgress(100, prog, false);
        } else {
            Integer prev = progressMap.get(orderId);
            if (prev != null) builder.setProgress(100, clamp(prev, 0, 100), false);
        }

        nm.notify(id, builder.build());
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        String orderId = call.getString("orderId");
        if (orderId == null || orderId.isEmpty()) {
            call.reject("orderId required");
            return;
        }
        Integer id = idMap.remove(orderId);
        progressMap.remove(orderId);
        if (id != null) getNotificationManager().cancel(id);
        call.resolve();
    }

    // --- Helpers -------------------------------------------------------------

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void createChannel(String id, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH);
            getNotificationManager().createNotificationChannel(channel);
        }
    }

    private Notification findExistingNotification(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (android.service.notification.StatusBarNotification sbn : getNotificationManager().getActiveNotifications()) {
                if (sbn.getId() == id) return sbn.getNotification();
            }
        }
        return null;
    }

    private int getIconResId(String name) {
        Context ctx = getContext();
        int resId = ctx.getResources().getIdentifier(name, "mipmap", ctx.getPackageName());
        if (resId == 0) resId = ctx.getResources().getIdentifier(name, "drawable", ctx.getPackageName());
        if (resId == 0) resId = android.R.drawable.stat_sys_download;
        return resId;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String getOrDefault(String v, String def) {
        return (v == null || v.isEmpty()) ? def : v;
        }
}
