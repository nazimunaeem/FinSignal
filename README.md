# 📊 FinSignal

**FinSignal** is a privacy-focused, local-first Android application designed to automate the tracking of credit card bills and financial statements by intelligently parsing bank SMS messages.

Designed with a high-density, modern UI, FinSignal helps you stay on top of your credit card payments without ever letting your sensitive financial data leave your device.

---

## ✨ Key Features

- 📱 **Smart SMS Parsing**: Automatically detects and extracts bill details (Total Due, Minimum Due, Due Date) from supported bank messages.
- 📅 **Year-wise History**: Organizes your financial history by year with a high-density view of all past statements.
- 💳 **Card Management**: Track multiple cards with detailed statistics, including "Total Paid" summaries for each card.
- 🔔 **Intelligent Reminders**: Customizable notification rules (Daily, 3 days before, Hourly on due date) to ensure you never miss a payment.
- 🌓 **Dark Mode**: Full support for System, Light, and Dark themes.
- 🔒 **Privacy First**: No cloud sync, no accounts. All data is stored locally in an encrypted Room database and Jetpack DataStore.
- 📤 **Data Export**: Export your entire bill history to CSV for personal accounting or backup.

---

## 🏦 Supported Banks

FinSignal currently provides automated parsing for major banks, including:
- ✅ **Prime Bank**
- ✅ **Pubali Bank**
- ✅ **City Bank (Amex)**
- ✅ **BRAC Bank**
- ✅ **Eastern Bank (EBL)**

> [!TIP]
> **Want your bank supported?** Open an issue or comment with your bank's bill SMS format. Please **exclude/anonymize** sensitive data like exact balances or customer IDs by replacing them with `*`.

---

## 📲 How to Install (APK)

If you are installing the pre-built APK, please follow these steps to ensure the app works correctly with Android's security features:

1. **Disable Play Protect**: Open the **Play Store**, tap your profile icon, select **Play Protect** -> **Settings**, and temporarily toggle off "Scan apps with Play Protect".
2. **Install the APK**: Open your downloaded `.apk` file and tap **Install**.
3. **Allow Restricted Settings**:
   - Go to your phone's **Settings** -> **Apps** -> **See all apps**.
   - Find and tap on **FinSignal**.
   - Tap the **three dots (⋮)** in the top right corner.
   - Select **"Allow restricted settings"** (This is required for SMS access on newer Android versions).
4. **Grant Permissions**:
   - Inside the app's Info page, go to **Permissions**.
   - Manually allow **SMS** and **Notifications**.
5. **Enjoy!**: Launch the app and start tracking your bills.

---

## 🛠 Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt
- **Local Database**: Room (with migrations support)
- **Background Tasks**: WorkManager (for periodic SMS scanning)
- **Data Persistence**: Jetpack DataStore (Preferences)
- **Build System**: Kotlin DSL (`.gradle.kts`)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 34+
- A device/emulator with SMS capability (for testing parsing)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/nazimunaeem/FinSignal.git
   ```
2. Open the project in Android Studio.
3. Build and run the app:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🤝 Contributing: Adding a New Bank Parser

We welcome contributions for new bank parsers! All parsing logic is centralized in:
`app/src/main/java/com/finsignal/data/sms/BankSmsParser.kt`

### Quick Steps to Add a Bank:
1. **Identify the Pattern**: Collect an anonymized sample SMS from the bank.
2. **Add Regex**: Define a new `Regex` pattern for the SMS body in `BankSmsParser.kt`.
3. **Register Sender**: Add the bank's sender ID (e.g., `CITYBANK`) to the `senderPatterns` map.
4. **Implement Parser**: Write a `parse[BankName]` function using the provided normalization helpers:
   - `parseAmount()`: Handles commas and currency symbols.
   - `normalizeDate()`: Standardizes dates to `dd/MM/yyyy`.
   - `normalizeBillPeriod()`: Canonicalizes periods to `Month Year`.
5. **Verify with Tests**: Add a test case in `BankSmsParserTest.kt` and run:
   ```bash
   ./gradlew testDebugUnitTest
   ```

---

## 🛡 Security & Privacy

FinSignal is built with security as a core principle:
- **Zero Cloud**: Your SMS data is never uploaded to any server.
- **Permission Scoping**: Only requires `READ_SMS` and `RECEIVE_SMS` to function.
- **No Personal Data in Repo**: We strictly forbid committing real SMS data or `local.properties`.

---

## 📜 License
Developed by **Nazim** • Executed with AI.
This project is for personal financial management and educational purposes.

---
> [!IMPORTANT]
> Never commit or push personal SMS data, bank details, OTPs, or real credentials to this repository.
