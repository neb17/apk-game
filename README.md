# NOVA AI (Android)

A starter Android AI chat app with a modern dark interface.

## Build the APK with GitHub Actions

1. Create a new GitHub repository.
2. Upload the contents of this project ZIP (not the ZIP file itself) to the repository.
3. Open the **Actions** tab and enable workflows if GitHub asks.
4. Select **Build NOVA AI APK** and press **Run workflow**, or push to `main`.
5. Open the completed workflow run, scroll to **Artifacts**, and download `NOVA-AI-debug-APK`.
6. Extract the downloaded artifact ZIP to get `app-debug.apk`.

## Enable AI replies

The app uses the OpenAI Chat Completions API and the `gpt-4o-mini` model. Open Settings in the app and enter an API key from an eligible API account. API usage may cost money and is separate from a ChatGPT subscription. Ask a parent/guardian to handle API account setup and billing if needed.

The key is saved in the app's private preferences on that device. It is not bundled into the APK. Do not share your key or distribute an APK configured with your personal key. For a public app, replace direct device-to-provider calls with a secure backend so API credentials are not exposed.

## Project details

- Package: `com.novaai.assistant`
- Minimum Android: 6.0 (API 23)
- Target SDK: 35
- Build: Android Gradle Plugin 8.7.3, Gradle 8.9, JDK 17
