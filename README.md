# AI App Factory

## Run locally

**Prerequisites:** Node.js

1. Install dependencies: `npm install`
2. To use Gemini features, put `API_KEY=your-key` in `.env.local`.
3. Start the app: `npm run dev`

## Build an Android APK

**Prerequisites:** Node.js 22 or later, Android Studio with the Android SDK, and Java 17 or later.

1. Install dependencies: `npm install`
2. Build the web app and sync it into Android: `npm run android:sync`
3. Open the Android project in Android Studio: `npm run android:open`. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Alternatively, build a debug APK from the repository root:

```sh
cd android
./gradlew assembleDebug
```

The APK is created at `android/app/build/outputs/apk/debug/app-debug.apk`. Install it on a connected device with `adb install -r android/app/build/outputs/apk/debug/app-debug.apk`.

Run `npm run android:sync` again whenever the web app changes.

The app uses online services for styling, fonts, and Gemini AI features, so it needs an internet connection.

### Gemini API key

The app reads `API_KEY` at build time. Any key included in an APK can be extracted, so do not distribute an APK containing a private Gemini key. Use a backend proxy for a publicly distributed app.
