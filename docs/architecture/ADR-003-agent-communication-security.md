# ADR-003 Agent Communication Security

## هدف
تعریف ارتباط امن بین Central Server و Local Agent.

## اصول
- هر Agent دارای Instance ID مستقل است.
- Discovery به معنی اعتماد نیست.
- Pairing نیازمند تأیید کاربر است.
- اطلاعات حساس مانند رمزها ذخیره یا منتقل نمی‌شوند.
- ارتباط باید قابل Audit باشد.

## مدل
Agent → Authentication → Secure Channel → Metadata Sync
