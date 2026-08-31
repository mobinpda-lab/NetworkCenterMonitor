# NetworkCenterMonitor
سامانه اندرویدی یکپارچه برای پایش مراکز، شبکه‌ها، IPها، تجهیزات و سرویس‌ها؛ مدیریت قطعی‌ها و Incidentها، پیگیری، Discovery، Camera/NVR/DVR Monitoring، PC Inventory، LAN Remote Access، گزارش‌گیری و پشتیبان‌گیری کامل.

## مرجع محصول
سند Canonical نیازمندی‌ها: `docs/MASTER_SPEC_V1.md` — MASTER SPEC v1.1

معماری اجرایی: `docs/ARCHITECTURE.md`

## معماری فعلی
- Kotlin + Jetpack Compose + Room
- Local-first و Server-ready
- ساختار Canonical: استان/گروه → مرکز → شبکه/VLAN/Range → Device/IP → سرویس/Port → Incident → پیگیری
- یک Entity عمومی `Device` برای تجهیزات شبکه، Camera/NVR/DVR و PC/Workstation
- Profile/Adapter تخصصی روی همان هسته مشترک؛ بدون دیتابیس، Incident Engine یا Scanner موازی
- Discovery Engine مشترک برای Network/Camera/PC
- Local Scanner Agent اختیاری برای مراکز پشت APN/NAT
- Remote Access با اولویت LAN/Private Network

## اصول کلیدی
- UI فارسی RTL با Vazirmatn
- گزارش/PDF/Print با Vazirmatn UI FD
- سبز=وصل، قرمز=قطع، نارنجی=هشدار، بنفش=پیگیری، خاکستری=غیرفعال/نامشخص
- Ping/ICMP مستقل از Port Monitoring
- اطلاعات Auto/Manual/Imported منبع‌گذاری می‌شوند و داده Manual بدون تأیید کاربر بازنویسی نمی‌شود
- Backup/Restore باید تمام موجودیت‌ها، تنظیمات، تاریخچه، Incidentها، Profileها و داده‌های جدید را پوشش دهد
- تمام زمان‌های قابل نمایش همراه تاریخ هجری شمسی هستند

## توسعه و مستندسازی
GitHub مرجع وضعیت اجرایی پروژه است. هر Track توسعه باید از مسیر Issue → Branch → Code/Test → Documentation → PR → CI → Review → Merge عبور کند. مستندسازی بخشی از Definition of Done است؛ Featureی که کد آن با `MASTER_SPEC`, `ARCHITECTURE`, README یا Issue/PR مربوطه ناهماهنگ باشد کامل محسوب نمی‌شود.

Epic فعلی توسعه: Issue #10 — Device Discovery, Camera Monitoring, PC Inventory & LAN Remote Access.