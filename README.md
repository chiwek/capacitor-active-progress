# @chiwek/capacitor-active-progress

Persistent “cab on the way” **active progress** for Capacitor apps.

- **Android (Java):** progress-bar notification you can start/update/stop from JS (and from FCM data handler).
- **iOS:** (planned/companion) Live Activity via ActivityKit (start locally, update via APNs).

> This package currently includes the Android Java plugin. iOS Live Activity will be provided in the iOS target.

## Install

```bash
npm i @chiwek/capacitor-active-progress
npx cap sync
```

Android 13+ permission in your app manifest:
```bash
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

Request at runtime before first use.

API
```bash
interface StartOptions {
  orderId: string;
  title?: string;
  text?: string;
  progress?: number;        // 0..100
  etaSeconds?: number;
  accentColor?: string;     // "#RRGGBB"
  smallIcon?: string;       // Android: mipmap/drawable name (no prefix)
  channelId?: string;       // default "active_progress"
  ongoing?: boolean;        // default true
  indeterminate?: boolean;  // default false
  payload?: Record<string, any>;
}

interface UpdateOptions {
  orderId: string;
  progress?: number;
  etaSeconds?: number;
  title?: string;
  text?: string;
  payload?: Record<string, any>;
}

interface StopOptions {
  orderId: string;
  reason?: 'arrived' | 'canceled' | 'timeout' | 'other';
  text?: string;
}
```
Usage
```bash
import { ActiveProgress } from '@chiwek/capacitor-active-progress';

await ActiveProgress.start({
  orderId: 'abc123',
  title: 'Driver accepted',
  text: 'ETA ~5 min',
  progress: 10,
  accentColor: '#f7a1ab',
  smallIcon: 'ic_launcher'
});

// later
await ActiveProgress.update({ orderId: 'abc123', progress: 65, text: 'ETA ~2 min' });

// on arrival
await ActiveProgress.stop({ orderId: 'abc123', reason: 'arrived', text: 'Driver has arrived' });
```

FCM (optional)

Handle your existing data messages and route to the plugin:

```
// data example:
// { type: "cab_progress", op: "update", orderId: "abc123", progress: 70, text: "ETA ~1 min" }
```

Notes

Keep styling & copy in JS 

The plugin doesn’t bundle Firebase; you can use Capacitor Push or your own handler.

smallIcon must be an existing resource name in your app (mipmap/drawable)