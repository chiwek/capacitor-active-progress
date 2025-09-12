#!/usr/bin/env node
/**
 * npx chiwek-active-progress <command>
 * Commands:
 *   ios:add-extension   -> copies template Live Activity files into ios/App/ActiveProgressWidget/
 *   android:check       -> prints Android permissions/channel checklist
 */

const fs = require('fs');
const path = require('path');

const ROOT = process.cwd();

function log(s){ console.log(s); }

function copyDir(src, dest) {
    if (!fs.existsSync(dest)) fs.mkdirSync(dest, { recursive: true });
    for (const entry of fs.readdirSync(src)) {
        const srcPath = path.join(src, entry);
        const destPath = path.join(dest, entry);
        const stat = fs.statSync(srcPath);
        if (stat.isDirectory()) copyDir(srcPath, destPath);
        else fs.copyFileSync(srcPath, destPath);
    }
}

function iosAddExtension() {
    // Typical Capacitor iOS app location:
    const targetDir = path.join(ROOT, 'ios', 'App', 'ActiveProgressWidget');
    const tplDir = path.join(__dirname, '..', 'templates', 'ios', 'ActiveProgressWidget');

    if (!fs.existsSync(path.join(ROOT, 'ios'))) {
        log('❌ Could not find ios/ folder. Run "npx cap add ios" first.');
        process.exit(1);
    }

    copyDir(tplDir, targetDir);
    log(`✅ Copied Live Activity templates to: ${path.relative(ROOT, targetDir)}`);

    log(`
Next steps in Xcode (once per app):
1) File → New → Target → Widget Extension → check "Include Live Activity"
   - Name it: ActiveProgressWidget
   - Set files to point to ${path.relative(ROOT, targetDir)}
   - Ensure the new target is added to the workspace.
2) App target → Signing & Capabilities:
   - Add: Push Notifications
   - Add: Background Modes → Remote notifications
   - Add: Live Activities
3) App Info.plist:
   - Add key: NSSupportsLiveActivities = YES
4) Build & run on iOS 16.1+ device/simulator.
  `);
}

function androidCheck() {
    log(`
Android checklist:
1) In app AndroidManifest.xml add:
   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
2) Request permission at runtime (Capacitor Push or Local Notifications).
3) You can pass smallIcon per tenant (mipmap/drawable).
4) Channel id defaults to "active_progress"; override via start({ channelId }).
  `);
}

const cmd = process.argv[2];
if (cmd === 'ios:add-extension') iosAddExtension();
else if (cmd === 'android:check') androidCheck();
else {
    log(`Usage:
  npx chiwek-active-progress ios:add-extension
  npx chiwek-active-progress android:check`);
}
