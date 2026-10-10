# IG Notify Forwarder (test)

A minimal Android app that forwards Instagram notifications to a receiver over HTTP.
It is a test of whether Instagram's own post notifications can serve as a new-post signal.

- **Reads:** notifications from `com.instagram.android` only. All others are dropped on the phone.
- **Sends:** title, text, post time and the notification's simple extras, plus a heartbeat every 5 minutes.
- **Permissions:** internet only. Notification access is granted by hand in Settings.
- **Dependencies:** none. The app is the three files in `app/src/main/java/dev/vorgal/ignotify/`.

## Build

GitHub Actions builds the APK on every push. Open the run under **Actions** and download
the `ig-notify-forwarder-apk` artifact. Each CI build uses a fresh debug key, so uninstall
the old version before installing a new build.

## Run the receiver

On a machine on the same Wi-Fi as the phone:

```
TOKEN=pick-a-secret node receiver/server.js
```

Find the machine's LAN IP (macOS: `ipconfig getifaddr en0`).

## Set up the phone

Use a dedicated Android 12+ phone with only Instagram and this app.

1. Install the APK. Allow "install unknown apps" for the browser or file manager.
2. Open the app. Enter `http://<LAN-IP>:8787/ingest` and the token. Tap **Save**, then **Send test event**.
3. Tap **Open notification access** and enable IG Notify Forwarder.
   On Android 13+ a sideloaded app first needs: App info → ⋮ → **Allow restricted settings**.
4. In the same notification-access page, open the app's entry → **See all apps** and switch off
   every app except Instagram. The OS then hides other apps from the listener.
5. Tap **Open battery optimization** and set both this app and Instagram to "Don't optimize".
6. In Instagram, follow the test accounts and turn on the bell for Posts and Reels.

Check `http://<LAN-IP>:8787/status` for counts and heartbeat age.
