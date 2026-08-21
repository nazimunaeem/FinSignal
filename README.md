# CardBill

CardBill is an Android app for tracking card bills, reminders, and payment activity from SMS data.

## Features
- Parse bank and utility SMS messages
- Track bills and due dates
- Manage credit cards and activity history
- Show reminders and notification-based follow-ups
- Store records locally on the device

## Tech Stack
- Kotlin
- Jetpack Compose
- Room database
- Hilt dependency injection
- WorkManager
- Android Gradle plugin

## Project Structure
- `app/` – Android application code
- `Analysis/` – local analysis and sample SMS data
- `gradle/` – Gradle wrapper and configuration

## Local Setup
1. Open the project in Android Studio.
2. Make sure the Android SDK is installed.
3. Run:

```bash
./gradlew assembleDebug
```

## Security and GitHub Notes
This project should not be published with:
- local machine paths from `local.properties`
- SMS exports containing OTPs, account numbers, or passwords
- API keys, tokens, or secret credentials
- generated build directories

The repository currently ignores the folders that contain local analysis and build output, including `Analysis/`, `app/build/`, and `.gradle/`.

## Important
Do not commit or push personal SMS data, bank details, OTPs, credentials, or anything that could identify a real user.
