# ADR-002 Production Orchestrator

## هدف
ایجاد لایه هماهنگ‌کننده تولید نرم‌افزار برای NetworkCenterMonitor.

## مسئولیت‌ها
- مدیریت صف کارها
- جلوگیری از اجرای تکراری Taskها
- هماهنگی توسعه موازی
- کنترل مسیر Issue → Branch → PR → CI → Merge
- ثبت وضعیت و شواهد اجرا

## چرخه
Idea → Design → Task → Worker → Test → PR → Gate → Merge → Release

## اصل مهم
Orchestrator نباید جایگزین Quality Gate شود؛ فقط هماهنگ‌کننده فرآیند است.
