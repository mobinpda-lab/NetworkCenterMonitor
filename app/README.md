# Android app module
این ماژول Foundation اولیه نسخه 1.0 است.

## Stack
- Kotlin 2.3.21
- Android Gradle Plugin 9.3.0
- Jetpack Compose BOM 2026.08.00
- Room 2.8.4
- Coroutines
- Java 17

## Current scope
- Application shell
- RTL layout
- Five canonical bottom-navigation tabs
- Canonical status color tokens
- Baseline unit test

Featureهای Monitoring، دیتابیس Canonical، Backup/Restore، گزارش و پیگیری در Taskهای مستقل بعدی اضافه می‌شوند.

> Gradle Wrapper در Task CI/Build ایجاد و اعتبارسنجی می‌شود؛ تا آن زمان CI از Gradle نسخه سازگار نصب‌شده استفاده خواهد کرد.
