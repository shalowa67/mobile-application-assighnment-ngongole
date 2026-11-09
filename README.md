# Ngongole: a smart credit book for Zambian shop owners

**BSE 3340 Mobile Application Development | Zambia University College of Technology**

> Small shop owners use Ngongole to record what each customer owes, record payments, and send a reminder to customers who have not paid.

## The problem

Many market traders and small shop owners in Zambia let regular customers buy on credit (*ngongole*) and write down who owes what in an exercise book. Pages get lost or wet, amounts are disputed, and owners forget whom to remind. Money that is owed goes unpaid because nobody can see the full picture quickly.

**Users**
- **Shop owners and market traders** (main users) record credit and payments, and send reminders.
- **Shop assistants** look up a customer's balance before giving more credit (same app, same phone).

## How to build and run

1. Install **Android Studio** (any recent stable version). It ships with the JDK the build needs.
2. Clone the repository:
   ```
   git clone <repository-url>
   ```
3. In Android Studio, choose **File > Open** and select the `Ngongole` folder. Wait for the Gradle sync to finish. The first sync downloads dependencies, so you need internet.
4. If Android Studio asks, install **Android SDK Platform 37** (Tools > SDK Manager).
5. Start an emulator (Device Manager > create a device with API 24 or newer) or plug in an Android phone with USB debugging on.
6. Press **Run ▶**.

Command line alternative (Windows): `gradlew.bat assembleDebug`. Unit tests: `gradlew.bat testDebugUnitTest`.

**Minimum Android version:** 7.0 (API 24).

## Screens

| Screen | What it does |
|---|---|
| **Customer List** (home) | Scrollable list of customers with what each owes, and the total owed to the shop. Search by name or phone. Shows a message when there are no customers yet or nothing matches the search. The bell button runs the overdue check right away. |
| **Add Customer** | Form for name, phone, amount and note. Checks that the name is present, the phone is a valid Zambian mobile number (095/096/097/075/076/077, with or without +260) and not already saved, and the amount is above zero with at most 2 decimals. Each error appears under its field. Typed text survives rotation. |
| **Customer Details** | Balance and full credit and payment history for one customer. **Record payment** (cannot exceed the balance), **Add credit**, **Remind by SMS** / **WhatsApp** with a ready-written message, and delete the customer (asks for confirmation first). |

Navigation: List → Add Customer, List → Customer Details. The system Back button and the top-bar arrow both return to the list, and Back on the list exits the app.

## How the technical requirements are met

| Requirement | Where |
|---|---|
| 3+ screens with navigation | `ui/NgongoleNavHost.kt` (Navigation Compose, one Activity) |
| Validated form | `ui/addcustomer/*`, rules in `domain/Validators.kt` |
| Scrollable list from our data | `LazyColumn` in `ui/customers/CustomerListScreen.kt`, data from Room |
| Lifecycle (rotation / leave and return) | ViewModels survive rotation. Form fields, search text and dialog state are stored in `SavedStateHandle`, so they also survive Android killing the app in the background |
| Data survives closing the app | Room database `ngongole.db` (`data/local/`) |
| Clean structure | UI (`ui/`) → ViewModel → `CustomerRepository` → DAO / Room (`data/`). Validation and formatting in `domain/` |
| No crashes | Empty and not-found states, field validation, duplicate phone check, payment > balance blocked, notification permission refusal handled, missing SMS or WhatsApp app handled |
| Runs from a fresh clone | Gradle wrapper included; `local.properties` is created by Android Studio |

### Project structure

```
app/src/main/java/com/ngongole/app/
├── MainActivity.kt              single Activity, hosts Compose
├── NgongoleApplication.kt       creates AppContainer, schedules the daily worker
├── AppContainer.kt              creates the database + repository once
├── data/
│   ├── CustomerRepository.kt    the only door to the data for the rest of the app
│   └── local/                   Room: entities, DAO, database
├── domain/                      plain Kotlin: validation, money, reminder text (unit-tested)
├── reminders/                   WorkManager worker, scheduler, notification builder
└── ui/
    ├── NgongoleNavHost.kt       routes + back stack
    ├── AppViewModelProvider.kt  ViewModel factory
    ├── customers/               Customer List screen + ViewModel + permission banner
    ├── addcustomer/             Add Customer screen + ViewModel
    ├── details/                 Customer Details screen + ViewModel + dialogs + SMS/WhatsApp intents
    └── theme/
```

---

## Research Note

> ⚠️ **Team:** these notes describe what the code does. Before you submit, rewrite them **in your own words**, add the problems **you** actually hit, and name every source you used, including AI tools. The lecturer will ask the owner of each topic about it.

### Research topic 1: Room local database (owner: _name_)

**What it is.** Room is Android's official library on top of SQLite. You describe tables as Kotlin classes (`@Entity`), write the SQL you need in an interface (`@Dao`), and Room generates the code that runs it. It checks your SQL at **build time**, so a typo in a column name stops the build instead of crashing the app.

**Why our app needs it.** A credit book is a set of records that must never be lost, and the balance must always be right. We have two tables:
- `customers`: name, phone (unique), note
- `ledger_entries`: every credit or payment, linked to a customer by `customerId` (a **one-to-many** relationship with a foreign key; deleting a customer deletes their entries through `CASCADE`)

We do not store a "balance" column that could get out of step with the entries. The balance is **calculated by SQL** with `SUM(CASE WHEN type='CREDIT' THEN amount ELSE -amount END)` (see `CustomerDao.observeCustomers`). The DAO returns a `Flow`, so when a payment is saved, Room re-runs the query and the list and total update on their own. `CustomerWithEntries` uses `@Relation` to load a customer with all their entries. `insertCustomerWithOpeningCredit` is a `@Transaction`, so a customer is never saved without their first credit entry.

Money is stored as **ngwee in a `Long`** (K 12.50 = 1250), because `Double` cannot store values like 0.10 exactly.

**Where we learned it.**
- Android Developers, *Save data in a local database using Room*: https://developer.android.com/training/data-storage/room
- *Define relationships between objects*: https://developer.android.com/training/data-storage/room/relationships
- Codelab *Persist data with Room*: https://developer.android.com/codelabs/basic-android-kotlin-compose-persisting-data-room
- _(add any videos / AI tools you used)_

**What went wrong along the way** _(replace with your own experience)_
- Room needs an annotation processor (KSP). Without the `ksp` plugin and `ksp(libs.androidx.room.compiler)` the build fails with "cannot find implementation for NgongoleDatabase".
- `@Relation` cannot sort the child list, so the history is sorted in the ViewModel (`sortedByDescending`).
- Room will not run database queries on the main thread. Our DAO functions are `suspend` or return `Flow`, so they run in the background.
- Changing an entity after the app is installed crashes on start unless you raise the database `version` and add a migration. During development we uninstalled the app instead.

### Research topic 2: Notifications with WorkManager (owner: _name_)

**What it is.** WorkManager is the Jetpack library for background work that must run **even if the app is closed or the phone restarts**. You write a `Worker` and give WorkManager a request (one-time or periodic). It stores the request and runs it at a good time. A **notification** is the message that appears in the status bar. On Android 8+ it needs a **channel**, and on Android 13+ the app must ask the user for the `POST_NOTIFICATIONS` **runtime permission**.

**Why our app needs it.** Owners forget whom to chase. Once a day `OverdueReminderWorker` asks the repository for customers who still owe money and **have not paid anything for 7 days or more** (an SQL `HAVING` clause in `CustomerDao.getOverdueCustomers`). It then shows one notification listing them. Tapping it opens the app.
- `ReminderScheduler.scheduleDaily()` uses `enqueueUniquePeriodicWork(..., KEEP, ...)` so starting the app many times does not create duplicate jobs. The first run is delayed to the next 08:00.
- The bell icon on the list calls `ReminderScheduler.runNow()`, a one-time request, so the feature can be shown in the demo.
- `NotificationPermissionBanner` asks for the permission once. If the owner refuses, the app keeps working and shows a card with a link to Settings. The worker checks the permission and quietly does nothing instead of crashing.

**Where we learned it.**
- *Schedule tasks with WorkManager*: https://developer.android.com/develop/background-work/background-tasks/persistent
- *Create a notification*: https://developer.android.com/develop/ui/views/notifications/build-notification
- *Notification runtime permission*: https://developer.android.com/develop/ui/views/notifications/notification-permission
- Codelab *Background Work with WorkManager*: https://developer.android.com/codelabs/basic-android-kotlin-compose-workmanager
- _(add any videos / AI tools you used)_

**What went wrong along the way** _(replace with your own experience)_
- Periodic work cannot run more often than every 15 minutes, and Android may delay it to save battery, so "daily at 08:00" is approximate.
- Testing a 7-day rule is slow. To test it, add a customer, change the emulator's date forward 8 days, then tap the bell.
- On Android 13+ nothing appeared until we requested the permission. Notifications fail silently without it.
- The notification small icon must be a single-colour vector, otherwise it shows as a white square.

### Supporting feature (not a research topic): sharing intents

`ui/details/ReminderIntents.kt` opens the SMS app (`ACTION_SENDTO`, `smsto:`) or WhatsApp (`https://wa.me/260…?text=…`) with the message already typed in. Ngongole itself sends nothing and needs **no SMS permission**: the owner presses Send. If no suitable app is installed, a snackbar explains instead of the app crashing.

---

## Team

| # | Full name | Student number | GitHub username | Owns |
|---|---|---|---|---|
| 1 | Bristol Shalowa | 2421003 | | Room database (`data/`) |
| 2 | Robert Sichizya | 2410264 | | WorkManager + notifications (`reminders/`) |
| 3 | Isreal Mumba | 2420971 | | Customer List screen |
| 4 | Mike Muyambango | 2420983 | | Add Customer form + validation |
| 5 | Emmanuel Sichilima | 2410156 | | Customer Details + SMS/WhatsApp intents |

## Demo video
https://youtu.be/MAWKZ6y461k
https://youtu.be/MAWKZ6y461k
