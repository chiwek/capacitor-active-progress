package com.chiwek.activeprogress;

import com.chiwek.activeprogress.R;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.widget.RemoteViews;

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

    /* ----------------------------- PUBLIC API ----------------------------- */

    @PluginMethod
    public void start(PluginCall call) {
        String orderId = call.getString("orderId");
        if (isEmpty(orderId)) { call.reject("orderId required"); return; }

        String title      = valOr(call.getString("title"), "Driver on the way");
        String text       = valOr(call.getString("text"),  "");
        String channelId  = valOr(call.getString("channelId"), DEFAULT_CHANNEL);
        boolean ongoing   = call.getBoolean("ongoing", true);
        boolean indet     = call.getBoolean("indeterminate", false);
        String smallIcon  = valOr(call.getString("smallIcon"), "ic_launcher");
        String largeIcon  = call.getString("largeIcon"); // e.g. "ic_logo_round"
        String accent     = call.getString("accentColor"); // e.g. "#FF2D55"
        String subText    = call.getString("subText"); // small brand line
        String style      = valOr(call.getString("style"), "stock"); // "stock" | "custom"
        boolean silent    = call.getBoolean("silent", true);

        // ensure channel exists even if it's custom
        createChannel(channelId, "Active Progress");

        int notiId = idMap.computeIfAbsent(orderId, k -> nextId++);

        Integer p = null;
        if (!indet) {
            int prog = clamp(call.getInt("progress", 0), 0, 100);
            progressMap.put(orderId, prog);
            p = prog;
        }

        Notification n = buildNotification(
                channelId, title, text, smallIcon, largeIcon, accent, subText,
                ongoing, silent, indet, p, style
        );

        nm().notify(notiId, n);
        call.resolve();
    }

    @PluginMethod
    public void update(PluginCall call) {
        String orderId = call.getString("orderId");
        if (isEmpty(orderId)) { call.reject("orderId required"); return; }
        Integer id = idMap.get(orderId);
        if (id == null) { call.resolve(); return; }

        Notification existing = find(id);
        String channelId = (existing != null && Build.VERSION.SDK_INT >= 26 && existing.getChannelId() != null)
                ? existing.getChannelId() : DEFAULT_CHANNEL;

        String smallIcon = (existing != null && existing.getSmallIcon() != null)
                ? existing.getSmallIcon().getResId() == 0 ? "ic_launcher" : resNameFromId(existing.getSmallIcon().getResId())
                : "ic_launcher";

        String title   = valOr(call.getString("title"),  existing != null ? existing.extras.getString(Notification.EXTRA_TITLE) : null);
        String text    = valOr(call.getString("text"),   existing != null ? existing.extras.getString(Notification.EXTRA_TEXT)  : null);
        String subText = call.getString("subText");
        String largeIcon = call.getString("largeIcon");
        String accent    = call.getString("accentColor");
        String style     = valOr(call.getString("style"), "stock");
        boolean silent   = call.getBoolean("silent", true);

        boolean ongoing = existing != null && (existing.flags & Notification.FLAG_ONGOING_EVENT) != 0;

        boolean indet;
        Integer p = null;
        if (call.getData().has("indeterminate")) {
            indet = call.getBoolean("indeterminate", false);
        } else {
            // keep previous indeterminate if we can't infer it
            indet = false;
        }

        if (call.getData().has("progress")) {
            int prog = clamp(call.getInt("progress", 0), 0, 100);
            progressMap.put(orderId, prog);
            p = prog;
        } else {
            Integer prev = progressMap.get(orderId);
            if (prev != null) p = clamp(prev, 0, 100);
        }

        Notification n = buildNotification(
                channelId, title, text, smallIcon, largeIcon, accent, subText,
                ongoing, silent, indet, p, style
        );

        nm().notify(id, n);
        call.resolve();
    }

    @PluginMethod
    public void stop(PluginCall call) {
        String orderId = call.getString("orderId");
        if (isEmpty(orderId)) { call.reject("orderId required"); return; }
        Integer id = idMap.remove(orderId);
        progressMap.remove(orderId);
        if (id != null) nm().cancel(id);
        call.resolve();
    }

    /* ----------------------------- BUILDERS ------------------------------ */

    private Notification buildNotification(
            String channelId,
            String title,
            String text,
            String smallIconName,
            String largeIconName,
            String accent,
            String subText,
            boolean ongoing,
            boolean silent,
            boolean indeterminate,
            Integer progress, // null means “no progress update”, keep existing
            String style // "stock" | "custom"
    ) {
        int smallIcon = iconRes(smallIconName);

        NotificationCompat.Builder b = new NotificationCompat.Builder(getContext(), channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setOngoing(ongoing)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSilent(silent);

        if (!isEmpty(subText)) {
            b.setSubText(subText);
        }

        if (!isEmpty(accent)) {
            try { b.setColor(Color.parseColor(accent)).setColorized(true); } catch (Exception ignored) {}
        }

        Bitmap li = largeIcon(largeIconName);
        if (li != null) b.setLargeIcon(li);

        if ("custom".equalsIgnoreCase(style)) {
            applyCustomRemoteViews(b, title, text, subText, indeterminate, progress, accent, largeIconName, smallIcon);
        } else {
            // Stock: BigText makes it more readable
            if (!isEmpty(text)) {
                b.setStyle(new NotificationCompat.BigTextStyle().bigText(text));
            }
            if (indeterminate) {
                b.setProgress(0, 0, true);
            } else if (progress != null) {
                b.setProgress(100, progress, false);
            }
        }

        return b.build();
    }

    /* ----------------------------- CUSTOM VIEWS ----------------------------- */

    private void applyCustomRemoteViews(
            NotificationCompat.Builder b,
            String title,
            String text,
            String subText,
            boolean indeterminate,
            Integer progress,
            String accent,
            String largeIconName,
            int smallIconRes
    ) {
        Context c = getContext();


        Context c = getContext();
        String appPkg = c.getPackageName();
        int hostLayout = c.getResources().getIdentifier(
                "notification_active_progress", "layout", appPkg);

        RemoteViews content;
        if (hostLayout != 0) {
          content = new RemoteViews(appPkg, hostLayout);
        } else {
          String pluginPkg = ActiveProgressPlugin.class.getPackage().getName();
          content = new RemoteViews(pluginPkg, R.layout.notification_active_progress);
        }

        content.setTextViewText(R.id.ap_title, title);
        content.setTextViewText(R.id.ap_text,  text == null ? "" : text);
        content.setImageViewResource(R.id.ap_small_icon, smallIconRes);

        if (!isEmpty(subText)) {
            content.setTextViewText(R.id.ap_subtext, subText);
            content.setViewVisibility(R.id.ap_subtext, android.view.View.VISIBLE);
        } else {
            content.setViewVisibility(R.id.ap_subtext, android.view.View.GONE);
        }

        // brand color on progress drawable (uses tint on API 23+)
        if (!isEmpty(accent)) {
            try {
                int color = Color.parseColor(accent);
                content.setInt(R.id.ap_progress, "setIndeterminateTintList", android.content.res.ColorStateList.valueOf(color));
                content.setInt(R.id.ap_progress, "setProgressTintList", android.content.res.ColorStateList.valueOf(color));
                content.setInt(R.id.ap_progress, "setProgressBackgroundTintList", android.content.res.ColorStateList.valueOf(adjustAlpha(color, 0.25f)));
            } catch (Exception ignored) {}
        }

        if (indeterminate) {
            content.setProgressBar(R.id.ap_progress, 0, 0, true);
        } else if (progress != null) {
            content.setProgressBar(R.id.ap_progress, 100, progress, false);
            content.setTextViewText(R.id.ap_percent, progress + "%");
        }

        Bitmap li = largeIcon(largeIconName);
        if (li != null) {
            content.setImageViewBitmap(R.id.ap_large_icon, li);
            content.setViewVisibility(R.id.ap_large_icon, android.view.View.VISIBLE);
        } else {
            content.setViewVisibility(R.id.ap_large_icon, android.view.View.GONE);
        }

        b.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
        b.setCustomContentView(content);
        b.setCustomBigContentView(content);
    }

    /* ----------------------------- HELPERS ----------------------------- */

    private NotificationManager nm() {
        return (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void createChannel(String id, String name) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW);
            ch.enableVibration(false);
            ch.enableLights(false);
            ch.setShowBadge(false);
            nm().createNotificationChannel(ch);
        }
    }

    private Notification find(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (StatusBarNotification sbn : nm().getActiveNotifications()) {
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

    private Bitmap largeIcon(String name) {
        if (isEmpty(name)) return null;
        Context c = getContext();
        int id = c.getResources().getIdentifier(name, "mipmap", c.getPackageName());
        if (id == 0) id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
        if (id == 0) return null;
        return BitmapFactory.decodeResource(c.getResources(), id);
    }

    private static int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private static String valOr(String v, String d) { return (v == null || v.isEmpty()) ? d : v; }
    private static boolean isEmpty(String s) { return s == null || s.isEmpty(); }

    private static int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private String resNameFromId(int resId) {
        try { return getContext().getResources().getResourceEntryName(resId); }
        catch (Exception ignored) { return "ic_launcher"; }
    }
}
