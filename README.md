# FinSignal

FinSignal is an Android app for tracking card bills, reminders, and payment activity from SMS data.

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

## Contributing: Adding a New Bank Parser

Only a few banks are supported so far. Adding support for another bank is welcome and only touches a few files.

All parsing lives in one file:

```
app/src/main/java/com/finsignal/data/sms/BankSmsParser.kt
```

Each bank is identified by its SMS sender ID (e.g. `PRIMEBANK`) or by keywords inside the message body, then parsed into a `ParsedBill`:

```kotlin
data class ParsedBill(
    val bankName: String,
    val cardLast4: String,
    val billPeriod: String,
    val totalDue: Double,
    val minDue: Double,
    val dueDate: String,
    val clientId: String? = null,
    val currency: String = "BDT"
)
```

### Steps to add a bank

1. **Collect real sample SMS** from the target bank (a card bill/statement SMS). Never commit real personal data — anonymize card numbers, names, and amounts in tests.
2. **Add a `BankType` enum value** in `BankSmsParser.kt`:
   ```kotlin
   enum class BankType { PRIME_BANK, PUBALI_BANK, CITY_BANK, BRAC_BANK, EBL, MY_NEW_BANK, UNKNOWN }
   ```
3. **Add a private regex** for the SMS format near the other bank patterns:
   ```kotlin
   private val myNewBankPattern = Regex(
       """...your pattern here...""",
       RegexOption.IGNORE_CASE
   )
   ```
4. **Register sender IDs** in the `senderPatterns` map:
   ```kotlin
   "MYNEWBANK" to BankType.MY_NEW_BANK,
   ```
5. **(Optional)** If the bank sends from a normal phone number instead of a sender ID, add statement-link keywords to `linkBankKeywords` or body-keyword heuristics in `identifyBank()` so the bank can still be detected.
6. **Add a dispatch branch** in the `when` block of `parse()`:
   ```kotlin
   BankType.MY_NEW_BANK -> parseMyNewBank(smsBody)
   ```
7. **Write the parse function**, following the existing pattern:
   ```kotlin
   private fun parseMyNewBank(smsBody: String): ParsedBill? {
       val match = myNewBankPattern.find(smsBody) ?: return null
       return ParsedBill(
           bankName = "My New Bank",
           cardLast4 = match.groupValues[1],
           billPeriod = normalizeBillPeriod(match.groupValues[2]),
           currency = normalizeCurrency(match.groupValues[3]),
           totalDue = parseAmount(match.groupValues[4]),
           minDue = parseAmount(match.groupValues[5]),
           dueDate = normalizeDate(match.groupValues[6])
       )
   }
   ```
8. **Add the bank name to `FILTER_KEYWORDS`** in `app/src/main/java/com/finsignal/data/sms/SmsReader.kt` — otherwise the inbox scanner may skip that bank's SMS before it ever reaches the parser.
9. **Add unit tests** in `app/src/test/java/com/finsignal/data/sms/BankSmsParserTest.kt` using your anonymized sample SMS:
   ```kotlin
   @Test
   fun parseMyNewBankSms() {
       val sms = "...sample sms body..."
       val parsed = BankSmsParser.parse(sms, "MYNEWBANK")
       assertNotNull(parsed)
       assertEquals("My New Bank", parsed?.bankName)
       assertEquals("1234", parsed?.cardLast4)
       // ...assert totalDue, minDue, dueDate, billPeriod
   }
   ```

### Conventions to follow

- Use `[\s\u00A0]*` between tokens in regexes so whitespace, line breaks, and non-breaking spaces are tolerated; always use `RegexOption.IGNORE_CASE`.
- Always pass captured values through the shared helpers instead of parsing manually:
  - `parseAmount(...)` – strips commas/currency symbols → `Double`
  - `normalizeCurrency(...)` – maps `Tk`, `৳`, `$`, etc. → canonical currency code
  - `normalizeDate(...)` – converts any date format → `dd/MM/yyyy`
  - `normalizeBillPeriod(...)` – converts any period format → e.g. `August 2026`

  These canonical forms are required because bills are deduplicated on `(card, billPeriod, currency)`.
- Return `null` when the body doesn't match — never throw.
- If a bank has multiple SMS formats, try each pattern in order and fall back to another shared pattern if needed (see `parsePubaliBank`).
- Run the tests before opening a PR:

```bash
./gradlew testDebugUnitTest
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
