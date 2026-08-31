# سند جامع نهایی طراحی و توسعه نرم‌افزار پایش مراکز و ارتباطات شبکه
## Canonical Product Requirements Document / Master Specification
### نسخه مرجع 1.1 – یکپارچه‌شده با Device Discovery، Camera Monitoring، PC Inventory و LAN Remote Access

---

## 1. وضعیت و اعتبار سند
این سند از این مرحله به‌عنوان سند Canonical و مرجع اصلی پروژه «نرم‌افزار پایش مراکز و ارتباطات شبکه» در نظر گرفته شود. طراحی دیتابیس، معماری نرم‌افزار، UI/UX، موتور Monitoring، موتور Discovery، گزارش‌ها، Backup/Restore، مدیریت تجهیزات، Camera Monitoring، PC Inventory، Remote Access، تست‌ها و مستندات باید با این سند تطبیق داده شوند. هر تغییر مهم بعدی باید در Git ثبت شود، نسخه سند افزایش یابد، از ایجاد معماری موازی جلوگیری شود و با مدل داده Canonical پروژه سازگار باقی بماند.

## 2. هدف پروژه
هدف، طراحی و توسعه یک نرم‌افزار اندرویدی APK برای پایش دائمی مراکز، IPها، تجهیزات و سرویس‌های شبکه است. محصول نباید صرفاً IP Scanner یا Port Scanner باشد؛ بلکه باید یک سامانه یکپارچه شامل پایش شبکه، مدیریت استان‌ها و گروه‌ها، مدیریت مراکز، مدیریت شبکه‌ها و Rangeها، مدیریت IPها، تجهیزات، سرویس‌ها و Portها، Network Discovery، Device Identification، Camera/NVR/DVR Monitoring، PC/Workstation Inventory، LAN Remote Access، تشخیص قطعی، Incident Management، پیگیری، مستندسازی، گزارش‌گیری، PDF، Print، Share، Backup، Restore و امنیت باشد و برای Backend مرکزی، چند کاربر، چند دستگاه و Web Dashboard در آینده آماده بماند.

## 3. اصول کلیدی محصول
اصل طراحی محصول «سادگی در ظاهر، قدرت در لایه‌های داخلی» است. رابط کاربری باید ساده، سریع، خلوت و قابل فهم باشد، اما کاربر با ورود به هر لایه بتواند به اطلاعات کامل مدیریتی و فنی دسترسی داشته باشد. هیچ اطلاعات مهمی نباید بدون امکان مشاهده و در صورت داشتن مجوز بدون امکان ویرایش ایجاد شود. هر مرکز، شبکه، تجهیز، IP، سرویس، Incident، Camera، NVR/DVR، PC و Remote Profile باید بتواند شناسنامه، وضعیت و تاریخچه مستقل داشته باشد. تمام اطلاعات ایجادشده توسط کاربر یا سیستم باید قابل مشاهده، جست‌وجو، فیلتر، گزارش، Backup و Restore باشند.

## 4. اصل عدم ایجاد معماری موازی
برای Settings، Monitoring، Discovery، Incident، History، Reports، Backup/Restore، Fonts، Custom Fields، Device Management، Camera Monitoring و PC Monitoring سیستم‌های موازی ایجاد نشوند. هر قابلیت فقط یک مسیر Canonical داشته باشد. Camera یک Device با Camera Profile، NVR/DVR یک Device با Recorder Profile و PC یک Device با PC Profile است.

## 5. ساختار Canonical اطلاعات
ساختار اصلی داده به‌صورت زیر توسعه یابد:

استان/گروه → مرکز/سایت → شبکه/VLAN/Range → تجهیز/IP → سرویس/Port → Incident → پیگیری

برای سامانه تصویری:

استان → مرکز → شبکه دوربین → NVR/DVR → Camera

برای PC:

استان → مرکز → شبکه → PC/Workstation → Services/Remote Profiles

این ساختار باید در تمام داشبوردها، گزارش‌ها، تاریخچه، Backup/Restore، Discovery و Incident حفظ شود.

## 6. استان، گروه‌بندی و مرکز
گروه‌بندی جغرافیایی، استانی، سازمانی، فنی، نوع مرکز، Range، CIDR، Backup Range و گروه سفارشی پشتیبانی شود. هر مرکز یک پرونده مادر با نام مرکز، کد مرکز، استان، گروه، نوع مرکز، نشانی، توضیح مکان، مسئول، شماره تماس، شماره تماس اضطراری، وضعیت فعال/غیرفعال، توضیحات و فیلدهای سفارشی داشته باشد. اطلاعات محاسباتی مرکز شامل تعداد شبکه‌ها، IPها، تجهیزات، سرویس‌ها، Cameraها، NVR/DVRها، PCها، سلامت مرکز، آخرین بررسی، آخرین قطعی، قطعی‌های جاری، Incidentهای باز و پیگیری‌های باز و سررسیدشده باشد.

## 7. شبکه‌های داخل هر مرکز
هر مرکز بتواند تعداد نامحدودی Network/VLAN/Range داشته باشد. فیلدها شامل نام شبکه، نوع شبکه، CIDR، From IP، To IP، Gateway، Subnet Mask، VLAN ID، روش دسترسی، Monitoring Enabled، Discovery Enabled، Scan Profile، Timeout، Retry، Rate Limit و توضیحات باشد. نوع شبکه حداقل General، Camera، Management، Server، VoIP، Industrial، Office و Custom را پشتیبانی کند. روش دسترسی حداقل LAN، Private APN، Routed APN، VPN، Route، Port Forward، Local Agent و Custom باشد. اینترنت عمومی نباید الزام معماری باشد.

## 8. مدل عمومی Device
تمام تجهیزات از یک Entity پایه Device استفاده کنند. انواع اولیه شامل Router، Firewall، Modem، MikroTik، Switch، Access Point، Server، Database Server، PC، Workstation، Camera، NVR، DVR، Network Printer، VoIP، PBX، PLC، Industrial Device، UPS، Storage، NAS، Unknown و Custom باشد. فیلدهای پایه شامل Internal Device ID، Province، Group، Center، Network، Display Name، Device Type، Manufacturer، Brand، Model، Hostname، Serial Number، Asset Number، Firmware، OS، IMEI، SIM Serial، Operator، شماره اختصاصی، شماره تماس، IP Addresses، Interfaces، MAC Addresses، Ports، Services، Access Method، Monitoring Enabled، Status، Last Seen، Last Status Change، Discovery Source، Notes، Tags و Custom Fields باشد.

هر Device بتواند یک یا چند IP، چند Interface و چند MAC داشته باشد. این قابلیت برای Server، Router، Firewall، NVR، PC و Storage الزامی است.

## 9. روابط بین تجهیزات
سیستم روابط Parent/Child، Connected-To، Managed-By، Recorded-By، Connected-Via و Network-Parent را نگهداری کند. یک Camera می‌تواند همزمان به Switch متصل باشد و روی NVR مشخص با Channel مشخص ثبت شده باشد.

## 10. پرونده IP
برای هر IP پرونده مستقل شامل عنوان، IP Address، Device، Center، Province، Group، Network، Range، Backup Range، Monitoring Enabled، Ping Enabled، Ping Interval، نوع تجهیز، توضیحات عمومی و توضیحات فنی ایجاد شود. هر IP بتواند چند سرویس و Port مستقل داشته باشد.

## 11. اطلاعات تجهیزات و دارایی
در پرونده IP یا Device حداقل شماره تماس اضطراری، شماره اموال، IMEI، شماره سریال سیم‌کارت، شماره سریال دستگاه، شماره اختصاصی، نام اپراتور، نوع تجهیز، سازنده، مدل و توضیحات وجود داشته باشد.

## 12. فیلدهای سفارشی
کاربر بتواند تعداد نامحدودی Custom Field با عنوان، نوع داده، ترتیب، Required، Enabled و Description ایجاد کند. Data Type شامل Text، Number، Date، DateTime، Phone، Boolean، Select و Multi-line باشد. اگر فیلد هیچ استفاده‌ای ندارد Hard Delete مجاز است؛ در غیر این صورت فقط Disable/Archive و حفظ مقادیر تاریخی مجاز باشد.

## 13. سرویس‌ها، Ping و Port Monitoring
هر IP بتواند تعداد نامحدودی سرویس با Service Name، Port، TCP/UDP، Importance، Monitoring Enabled، Check Interval، Timeout، Retry، Notes و Standard/Vendor/Custom داشته باشد. Ping/ICMP مستقل از Port Monitoring باشد و Port ندارد. خاموش کردن Ping نباید Port Monitoring را متوقف کند و برعکس.

کاربر بتواند Ping را روی یک IP، چند IP، مرکز، گروه، استان، Range یا نتایج فیلتر جاری فعال/غیرفعال کند. قبل از Bulk Change تعداد موارد تحت تأثیر نمایش داده شود.

## 14. Monitoring Hierarchy و Timing
اولویت تنظیمات Global → Group → Center → Network → Device/IP → Service باشد و سطح پایین‌تر Override کند. فواصل نمونه 10، 20، 30، 60 ثانیه، 2 دقیقه یا Custom باشند. Timeout، Retry و فاصله Retry برای Ping، TCP، UDP، HTTP، RTSP، ONVIF، Vendor API و Discovery مستقل قابل تنظیم باشند.

## 15. معماری موتور Monitoring
موتور باید Queue، Batch، Bounded Concurrency، Coroutines، Worker Pool، Connection Limit، Timeout، Retry، Backoff، Cancellation و Resume داشته باشد. UI نباید Block شود. Restart/Resume یا بسته‌شدن UI نباید Incident باز، Current State یا Monitoring History را از بین ببرد.

## 16. ورود IP و Range
روش‌های ورود شامل IP دستی، From-To Range و CIDR استاندارد IPv4 باشد. در ورود گروهی کاربر بتواند Province، Group، Center، Network، Range، Backup Range، Monitoring Method، Ping، Port، Interval، Device Preset و Notes را تعیین کند. حالت‌های فقط Ping، Ping+Port و فقط Port وجود داشته باشند و Ping Enabled پیش‌فرض پیشنهادی باشد. Port مشترک روی کل Range قابل اعمال باشد و بعد از ایجاد همه IPها و سرویس‌ها مستقل قابل ویرایش باشند.

قبل از ثبت Range، Preview شامل تعداد IP، تعداد Probe، Portها، Monitoring Method، Center، Network، Interval و Estimated Load نمایش داده شود. Rangeهای بزرگ مثل /16 بدون Bounded Concurrency فعال نشوند و بار تقریبی قبل از فعال‌سازی نمایش داده شود.

## 17. پورت‌های استاندارد و سفارشی
فهرست اولیه شامل 22 SSH، 23 Telnet، 25 SMTP، 53 DNS، 80 HTTP، 110 POP3، 123 NTP، 143 IMAP، 161/162 SNMP، 389 LDAP، 443 HTTPS، 445 SMB، 502 Modbus TCP، 554 RTSP، 993 IMAPS، 995 POP3S، 1433 SQL Server، 1521 Oracle، 1883 MQTT، 3306 MySQL، 3389 RDP، 5432 PostgreSQL، 5900 VNC، 5060/5061 SIP، 8080 HTTP Alternative، 8291 MikroTik Winbox، 8443 HTTPS Alternative، 8883 MQTT TLS و 9100 JetDirect باشد. Port سفارشی همیشه مجاز باشد. برچسب Standard و Vendor/Recommended برای Portها نگهداری شود.

## 18. Device Preset
Presetهای اولیه Router/Firewall، Modem، MikroTik، DVR/NVR، IP Camera، PLC/Industrial، VoIP/PBX، Server، Database Server، Network Printer و PC/Workstation باشند. هر Preset شامل نام، دسته تجهیز، Portها، TCP/UDP، Standard/Vendor و Default Enabled باشد. قبل از اعمال، فهرست Portها نمایش داده شود و Preset بدون تأیید Port موجود را حذف یا بازنویسی نکند. Preset سفارشی قابل Create/Copy/Edit/Disable باشد.

## 19. Network Discovery
یک Discovery Engine عمومی در هسته وجود داشته باشد و Scanner جداگانه موازی برای Camera یا PC ساخته نشود. Modeهای اصلی Network Scan و Device-Specific Scan باشند. کاربر Province، Center، Network و Range را مشخص کند و سیستم Hostهای قابل دسترس را بررسی و در حد امکان نوع تجهیز را تشخیص دهد.

روش‌های Discovery در حد مجاز و کم‌فشار شامل ARP، ICMP، TCP Port Probe، UDP Probe در موارد لازم، HTTP، HTTPS، RTSP، ONVIF، Hostname، MAC Vendor، Service Fingerprint، Vendor API، Authenticated Inventory و Local Agent باشند.

Scan دارای Slow، Normal، Fast و Custom Rate، Timeout، Retry، Pause، Stop، Resume، Progress و Duplicate Detection باشد. نتیجه قبل از ثبت Preview شود و کاربر بتواند یک، چند یا همه موارد را Select و به همان Province/Center/Network اضافه کند.

Unknown Device از بین نرود و با IP، MAC، Ports و Fingerprints موجود ثبت شود. Device Type و Capability می‌توانند Confidence/Compatibility Score داشته باشند و صرف باز بودن یک Port برای تشخیص قطعی نوع تجهیز کافی نباشد.

## 20. Camera Monitoring
Camera Monitoring ماژول تخصصی داخل همان نرم‌افزار اصلی باشد. Camera یک Device با Camera Profile، NVR و DVR نیز Device با Recorder Profile هستند. ساختار Province → Center → Camera Network → NVR/DVR → Camera باشد و Camera مستقل نیز مجاز باشد.

### شناسنامه Camera
فیلدهای انسانی شامل نام/عنوان دوربین، نام محیط نصب، مکان نصب و توضیح محل نصب باشند. فیلدهای فنی شامل Brand، Model، Camera Type، IP، MAC، Serial، Firmware، HTTP/HTTPS/RTSP/ONVIF Ports، Codec، Resolution، FPS، Bitrate، Main/Sub Stream، ONVIF، RTSP، PTZ، Audio، Recording Status، NVR/DVR، Channel Number، Last Seen، Status، Notes، Tags و Custom Fields باشند.

اصل طراحی این است که تا جای ممکن اطلاعات فنی از Network، ONVIF، RTSP، HTTP/HTTPS، NVR/DVR، Vendor API یا SDK به‌صورت Auto استخراج شود. اطلاعات محلی مانند Environment و Install Location معمولاً Manual هستند. همه فیلدها توسط کاربر قابل ویرایش باشند.

برای هر فیلد Source = Auto/Manual/Imported و Last Discovery/Last Update نگهداری شود. Discovery جدید نباید Manual Value را بدون اجازه تغییر دهد و در اختلاف Old Value/Detected Value نمایش و Update نیازمند تأیید باشد.

## 21. ONVIF و معماری چندبرندی
برای تجهیزات جدید ONVIF Profile T اولویت داشته باشد؛ Profile S برای Legacy، Profile G برای Recording/Replay و Profile M برای Metadata/Analytics در صورت پشتیبانی استفاده شوند. هسته Vendor-Neutral باشد.

Adapterهای اولیه:
- Hikvision: ONVIF + ISAPI + HCNetSDK
- Dahua: ONVIF + NetSDK + CGI/API
- Uniview: ONVIF + NetDEVSDK
- Axis: ONVIF + VAPIX
- Hanwha: ONVIF + SUNAPI
- آمادگی برای Bosch، Vivotek، Milesight، Avigilon، TVT، Xiongmai/XMeye و OEMهای متداول

Capability Discovery در حد امکان Manufacturer، Model، Firmware، Serial، MAC، Hostname، ONVIF/Profiles، RTSP، HTTP/HTTPS، Main/Sub Stream، PTZ، Audio، Events، Channel Count، Recording، HDD Monitoring و Vendor API را کشف کند. Feature صرفاً بر اساس برند فرض نشود.

## 22. Camera Scan و APN/NAT
Camera Scan روی همان Discovery Engine عمومی اجرا شود و Range/CIDR و Ports یا Auto Detect بگیرد. ONVIF WS-Discovery در LAN محلی پشتیبانی شود ولی Multicast از NAT/Port Forward قابل فرض نیست.

در ساختار Server → Private APN → Modem → LAN → Camera/NVR، اگر فقط Port Forward وجود دارد، Discovery کامل LAN از سرور مرکزی الزاماً ممکن نیست. راهکارهای قابل پشتیبانی: Routed APN، VPN/Route، Routing مناسب مودم و Local Scanner Agent.

## 23. Local Scanner Agent
Agent سبک اختیاری داخل مرکز بتواند LAN Discovery، Camera Discovery، Device Health، PC Inventory و Remote Software Detection انجام دهد و Metadata/Status به سامانه ارسال کند. Agent نباید Stream تصویری دائمی ارسال کند و نباید یک سامانه موازی مستقل ایجاد کند.

## 24. Camera Health و پهنای باند
در صورت پشتیبانی دستگاه Camera Online/Offline، Channel Status، Video Loss، Authentication Error، Network Error، Recording Active/Stopped، HDD Normal/Warning/Error/Full، Free Space، Last Recording، Motion/Tamper، Stream Availability و Firmware مانیتور شوند.

برای APN و شبکه کم‌سرعت، Stream دائمی ممنوع باشد. سه Level تعریف شود:
1. Health Check: Ping/Port/API/ONVIF/Channel Status با مصرف کم.
2. Snapshot: فقط On Demand، هنگام باز شدن صفحه یا Schedule محدود.
3. Live Stream: فقط هنگام درخواست کاربر.

Live View ابتدا Sub Stream را باز کند، Main Stream فقط با درخواست فعال شود، با خروج کاربر فوراً بسته شود، Timeout داشته باشد و Maximum Concurrent Streams برای هر Center قابل تنظیم باشد. Bandwidth Profile شامل Low Bandwidth، Normal، High Speed و Custom باشد. Monitoring تطبیقی باشد: Device سالم با Interval عادی و Device مشکوک/Offline با Retry کوتاه‌تر و سپس بازگشت به Interval عادی.

## 25. PC / Workstation Profile
PC و Workstation نیز Device هستند و Profile تخصصی داشته باشند. فیلدها در صورت قابل استخراج بودن شامل Computer Name، Hostname، IP، MAC، Manufacturer، Model، Serial، Asset Number، Windows Edition/Version/Build، Architecture، CPU، RAM، Storage، Free Disk، Network Interfaces، Uptime، Domain، Workgroup، Last User در صورت مجاز بودن، Defender/Antivirus Status، Windows Update Status، Notes و Custom Fields باشند.

روش استخراج سه‌سطحی باشد:
1. Network Inventory: IP، MAC، Hostname، Ports، Services.
2. Authenticated Inventory با مجوز: Installed Apps، Services، Windows Info، Hardware Info و Remote Software.
3. Local Agent اختیاری برای Inventory دقیق‌تر.

## 26. LAN Remote Access Manager
Remote Access بخشی از Device Management باشد و LAN-Only / Private Network First طراحی شود. اینترنت عمومی و Cloud برای عملکرد اصلی الزامی نباشند. Private APN، LAN، Routed Private Network و VPN خصوصی پشتیبانی شوند.

Remote Methodهای اولیه:
- RDP
- Radmin
- RustDesk
- VNC
- TeamViewer LAN Mode در صورت پیکربندی
- Custom Remote Method

معماری برای اضافه‌شدن روش‌های دیگر آماده باشد.

هر IP/PC بتواند صفر تا چند Remote Profile داشته باشد. فیلد هر Profile شامل Remote Type، Display Name، IP، Default Port، Custom Port، Effective Port، Protocol، Installed، Running، Available، Auto Detected، Manual، Version، Last Check و Notes باشد.

## 27. Custom Remote Port و Manual Override
Port پیش‌فرض فقط پیشنهاد باشد. اگر کاربر Port را عوض کرده، Custom Port ذخیره و Effective Port از آن گرفته شود. Effective Port برای Monitoring، Connection Test و Remote Launch استفاده شود. کاربر بتواند برای هر IP به‌صورت Manual تعیین کند چه Remote Softwareی، روی چه Portی و با چه اولویتی استفاده شود. Manual Value بر Auto Discovery اولویت داشته باشد و Discovery آن را بدون تأیید بازنویسی نکند.

Remote Software Detection می‌تواند از Port Detection، Service Fingerprinting، Authenticated Inventory، Windows Service، Installed Software و Local Agent استفاده کند. Port Detection به‌تنهایی برای اعلام نصب قطعی همه نرم‌افزارها کافی نیست.

## 28. Remote Icons و دکمه Remote
در لیست هر IP/PC آیکون Remote Methodهای شناسایی یا تعریف‌شده نمایش داده شود. وضعیت پیشنهادی:
- سبز: Active/Reachable
- زرد: Configured but Unreachable
- خاکستری: Unknown/Disabled

آیکون قابل لمس باشد و Profile همان روش را باز کند. هر PC یک دکمه واحد Remote داشته باشد. اگر فقط یک روش قابل اتصال است مستقیم همان را Launch کند و اگر چند روش موجود است Method Picker نمایش دهد.

اولویت پیشنهادی LAN قابل تنظیم باشد و به‌صورت اولیه RustDesk Direct/LAN، RDP، Radmin، VNC، TeamViewer LAN، Custom در نظر گرفته شود.

## 29. LAN-Only Policy و Remote Security
اصل قطعی Remote Access این است که اتصال باید بدون اینترنت عمومی قابل انجام باشد. RustDesk Direct IP/Self-hosted LAN، RDP Direct LAN IP+Port، Radmin Direct LAN IP+Custom Port و VNC Direct LAN IP+Custom Port پشتیبانی شوند. TeamViewer فقط در LAN Mode به‌عنوان روش LAN در نظر گرفته شود.

Passwordهای Remote Plain Text ذخیره نشوند. در صورت نیاز Secure Storage، Encrypted Credential Store یا Ask-on-Connect استفاده شود.

## 30. تشخیص قطعی و Incident
یک Failure منفرد نباید Incident قطعی ایجاد کند. Flow: Failure → Suspected → Retry → Confirmed Outage → Incident. زمان پیش‌فرض تأیید قطعی یک دقیقه و قابل تغییر باشد. Recovery نیز با تعداد Probe یا مدت قابل تنظیم تأیید شود تا وصل لحظه‌ای Incident را اشتباه نبندد.

وضعیت‌ها حداقل Online، Offline، Suspected، Warning، Monitoring Disabled و Unknown/Not Checked باشند. رنگ‌ها: سبز=وصل، قرمز=قطع، نارنجی=هشدار/مشکوک/قطعی جدید، بنفش=پیگیری، زرد=در حال پیگیری، خاکستری=غیرفعال/نامشخص و آبی=Action/UI.

هر Incident شامل Number، Device/IP، Port، Service، Center، Province، Start Time، End Time، Duration، Failure Count، Possible Cause، Final Result، Follow-ups و Notes باشد. با Recovery، زمان وصل ثبت، Duration محاسبه و Incident بسته شود.

## 31. Flapping، Maintenance و Importance
اگر سرویس در مدت کوتاه بارها Up/Down شد، ده‌ها اعلان یا Incident مستقل تولید نشود و Flapping Pattern ثبت شود. Maintenance Mode برای Group، Center، Network، Device، IP و Service قابل تعریف باشد؛ Monitoring می‌تواند ادامه یابد ولی Alert عادی Suppress شود و Downtime Maintenance در آمار عادی محاسبه نشود. Importance/Severity شامل Normal، Important و Critical باشد و قطع Critical Service بتواند Center Health را Red کند.

## 32. سلامت مرکز
Center Health بر اساس Device Status، Service Status، Importance، Current Incidents، Camera/NVR Problems و Critical Services محاسبه شود. خلاصه‌ای مانند تعداد سرویس‌ها، تعداد وصل/قطع، آخرین قطعی و تعداد پیگیری باز نمایش داده شود.

## 33. صفحه اصلی و UI
صفحه اصلی ساده، خلوت، سریع، RTL و Card-based باشد. کارت‌های اصلی شامل کل مراکز، وصل، قطع، قطعی جدید، پیگیری باز و پیگیری سررسیدشده باشند. جزئیات سنگین Camera، PC، Ports و History در Home نمایش داده نشوند و از طریق Drill-down قابل دسترسی باشند.

Bottom Navigation شامل خانه | قطعی‌ها | پیگیری‌ها | گزارش‌ها | تنظیمات باشد. Device، Discovery، Camera و Remote از Center/Device Drill-down یا منوهای داخلی مناسب قابل دسترس باشند. FAB + برای Center، Network، IP، Range، Device و Service استفاده شود.

تمام کارت‌ها و ردیف‌ها فعال باشند. Drill-down نمونه: Province → Center → Network → Device/IP → Service → History یا Province → Center → NVR → Camera.

## 34. Selection Mode و Bulk Operations
Long Press، Selection Mode را فعال کند. Multi Select و Select All Current Filter وجود داشته باشد. عملیات گروهی حداقل PDF، Print، Share، Ping Enable/Disable، Port Monitoring Enable/Disable، Change Group، Change Center در موارد امن، Change Interval، Change Monitoring Profile و Apply Preset را پشتیبانی کند. یک آیتم = Full Edit؛ چند آیتم = فقط Shared Safe Fields تا اطلاعات اختصاصی ناخواسته بازنویسی نشوند.

## 35. جست‌وجوی هوشمند و مرتب‌سازی
Search سراسری در Center، Province، Group، IP، Network، Range، Backup Range، Port، Service، Device Name، Brand، Model، Hostname، MAC، IMEI، Asset Number، Serial، SIM Serial، Phone، Camera Name، Camera Environment، PC Name، Remote Type، Notes و Custom Fields جست‌وجو کند. Sort بر اساس Province، Group، Center، Name، IP، Status، Downtime، Failure Count، Last Connection، Last Outage، Device Type و Brand با Asc/Desc فراهم باشد.

## 36. قطعی‌ها و فیلترها
تب قطعی‌ها دو Mode Current Outages و Outage History داشته باشد. فیلترها حداقل All/Country، Province، Group، Center، Network، Range، CIDR، Backup Range، Device، IP، Port، Service، Camera، NVR/DVR، PC، Date Range، Importance، Maintenance و Follow-up Status را پوشش دهند.

## 37. پیگیری‌ها، یادداشت و فایل
هر Center، Device، IP، Service و Incident تعداد نامحدودی Follow-up داشته باشد. وضعیت‌ها New، Open، In Progress، Waiting، Referred، Overdue، Done و Closed باشند. اطلاعات شامل Subject، Date/Time، Owner، Description، Result، Status، Next Follow-up، Reminder، File، Note و Related Incident باشد. برای Center، Network، Device، IP، Service، Camera، NVR/DVR، PC، Incident و Follow-up امکان Note، Image و File وجود داشته باشد.

## 38. تاریخ و ساعت
زمان در دیتابیس به‌صورت Timestamp استاندارد ذخیره شود، ولی UI، Notification، PDF، Print، Share و Reports تاریخ هجری شمسی نمایش دهند. نمونه عادی «۱۴۰۵/۰۶/۰۹ - ۱۵:۴۲» و نمونه رسمی «دوشنبه ۱۴۰۵/۰۶/۰۹ - ساعت ۱۵:۴۲:۱۸» باشد.

## 39. اعلان‌ها
Outage Notification شامل Center، IP، Service، Port و Jalali Start Time باشد. Recovery Notification شامل Center، IP، Service، Duration و Jalali Recovery Time باشد.

## 40. گزارش‌ها
Report برای کل کشور، Province، Group، Center، Network، Device، IP، Port، Service، Range، Backup Range، CIDR، Camera، NVR/DVR، PC، Remote Access Inventory، Selected Items و Current Filter قابل تولید باشد. گزارش‌های آماری شامل Current Outages، Highest Failure Count، Longest Downtime، Today/Week/Month، Center/Device Outages، Camera Offline، NVR HDD Errors، PC Offline، Open/Overdue Follow-ups و Incidents Without Follow-up باشند.

PDF برای Center، Device، IP، Service، Province، Group، Range، Camera، NVR، PC، Outages، Follow-ups و Reports قابل تولید باشد. همه گزارش‌ها در Header دارای Scope و «تاریخ تهیه گزارش: ... - ساعت ...» به هجری شمسی باشند. Print مستقیم و Android Share Sheet پشتیبانی شوند.

## 41. Typography و Style Guide
فونت UI اصلی Vazirmatn باشد. Report/PDF/Print از Vazirmatn UI FD مستقل از UI Font استفاده کند و Font Embed شود. وزن‌های پیشنهادی: Main Title Bold 700/18-20، Section SemiBold 600/14-16، Subtitle Medium 500/12-14، Body Regular 400/11-12، Table Header SemiBold 600/10-11، Table Body Regular 400/9-10، Date/Numbers Regular/Medium 9-11، Important Result Bold 700/11-12 و Footer Regular 8-9.

RTL، اتصال حروف فارسی، اعداد فارسی، تاریخ شمسی، Line Wrap، Table Overflow، Font Weights و Preview/PDF/Print Consistency تست شوند. سبک بصری: Light/White Background، Blue Accent، Rounded Cards، Minimal، Modern، Smart، RTL و White Space مناسب. رنگ وضعیت‌ها در کل برنامه ثابت باشند.

## 42. Backup کامل
اصل قطعی پروژه: هیچ داده کاربر یا داده تولیدشده سیستم خارج از Backup نماند. Backup شامل Groups، Provinces، Centers، Networks، VLANs، Ranges، Backup Ranges، Devices، Interfaces، IPs، MACs، Services، Ports، Presets، Monitoring Settings، Discovery Profiles، Camera Profiles، NVR/DVR، Channels، PC Inventory، Remote Profiles، Custom Remote Ports، History، Incidents، Follow-ups، Notes، Files، Custom Fields و مقادیرشان، Asset Data، Settings، Report Settings و Security Data قابل انتقال امن باشد.

نام نمونه: `Backup_1405-06-09_15-42-18.backup`. Metadata شامل Jalali Creation Date، App Version، Database Version، Backup ID، Center Count، Network Count، Device Count، IP Count، Service Count، Camera Count و PC Count باشد.

## 43. Restore و امنیت Backup
Restore باید کل اطلاعات را بازگرداند. قبل از Restore، Emergency Backup کامل خودکار ساخته شود. App Version، DB Schema و Compatibility بررسی و در صورت نیاز Migration ایمن انجام شود. در Failure، دیتای فعلی از بین نرود. Backup قابلیت Encryption داشته باشد و Sensitive Data Plain Text ذخیره نشود. فایل Backup روی دستگاه دیگر نیز قابل Restore باشد.

## 44. امنیت ورود
نرم‌افزار Username/Password داخلی داشته باشد و قابل تغییر باشند. Recovery Question قابل تعریف باشد و Answer Plain Text ذخیره نشود. PIN، Biometric و Auto Lock اختیاری باشند. Auto Lock شامل Disabled، 1 Minute، 5 Minutes، 15 Minutes و On App Exit باشد.

## 45. Settings Canonical
Settings دسته‌بندی‌شده و بدون مسیر موازی باشد:
- Monitoring: Ping/Port Interval، Timeout، Retry، Confirm Failure/Recovery، Flapping، Maintenance
- Discovery: Scan Profile، Rate Limit، Timeout، Retry، Default Ports، Fingerprinting
- Devices: Standard/Custom Ports، Device Presets
- Camera: ONVIF، RTSP، Vendor Adapters، Snapshot، Stream Limits، Bandwidth Profile
- PC/Remote: Remote Types، Default Ports، LAN-only Policy، Custom Ports، Remote Priority
- Data: Custom Fields، Range، Backup Range
- Reports: PDF، Print، Typography
- Backup: Backup، Restore، Encryption
- Security: Username، Password، Recovery، PIN، Biometric، Auto Lock

## 46. حذف داده و Referential Integrity
حذف Center، Network، Device، IP، Range، Incident، History، Backup و داده مهم نیازمند Confirm باشد. Entity دارای Dependency فعال بدون بررسی حذف نشود. Historical Data حفظ شود. Archive/Disable بر Delete ترجیح داده شود.

## 47. Android Widget
در معماری آینده Widget برای Total Centers، Online، Offline، Open Follow-ups و Last Update در نظر گرفته شود و Tap وارد برنامه شود.

## 48. Local-first و Server-ready
نسخه اولیه بتواند کاملاً Local اجرا شود. معماری از ابتدا برای Central Backend، Multiple Phones، Multiple Users، Sync، Web Dashboard، Central Monitoring و Local Agents آماده باشد و توسعه این قابلیت‌ها نیازمند بازنویسی کامل Core نباشد.

## 49. فناوری پیشنهادی
Android با Kotlin و Jetpack Compose، Room برای دیتابیس محلی، Coroutines و Flow برای پردازش و State، Foreground Service برای Monitoring فعال و WorkManager فقط برای وظایف غیرلحظه‌ای استفاده شود.

## 50. معیار پذیرش Core نسخه 1.x
حداقل Manual IP، Range/CIDR، Ping مستقل، Port Monitoring، Bulk Ping، Timeout/Retry، Failure Confirmation، Recovery، Incident، Flapping، Maintenance، Severity، History، Follow-ups، Custom Fields، Port Presets، Device Presets، Search، Sort، Filters، Selection Mode، Group Operations، PDF، Print، Share، Jalali Date، Vazirmatn، Full Backup/Restore، Emergency Backup، Login/Recovery و UI Style Guide باید عملیاتی باشند.

## 51. معیار پذیرش Discovery
Discovery باید Center/Network Scope بگیرد، CIDR/Range اسکن کند، Host فعال پیدا کند، Portها را بررسی کند، Device Type را در حد امکان تشخیص دهد، Unknown Device را از دست ندهد، Preview و Selective Import داشته باشد، Rate Limit داشته باشد و Manual Data را حفظ کند.

## 52. معیار پذیرش Camera
حداقل Camera/NVR/DVR به‌عنوان Device، Camera Profile، Province/Center Classification، Camera Scan، ONVIF/RTSP Detection، Brand/Model Auto Discovery در صورت امکان، Manual Location، NVR/Channel Relation، Online/Offline، Low-Bandwidth Health Check، Snapshot On Demand، Live Sub Stream On Demand، No Permanent Stream، Incident Integration و Backup/Restore عملیاتی باشند.

## 53. معیار پذیرش PC
حداقل PC Discovery، PC Profile، Hostname، IP/MAC، Windows Info و Hardware Info در صورت امکان، Remote Profile، Remote Icons، Custom Remote Port، Manual Override، LAN-only Connection و Backup/Restore پشتیبانی شوند.

## 54. معیار پذیرش Remote Access
برای هر IP/PC صفر، یک یا چند Remote Profile، نمایش Icon، Effective Port، Custom Port، Manual Override، Auto Detection، Connection Test، Remote Button، Single Method Direct Launch و Multiple Method Picker پشتیبانی شود. Remote Access برای عملکرد اصلی نیازمند اینترنت عمومی نباشد.

## 55. معیار Backup
تست رسمی Create Data → Full Backup → Delete Data → Restore باید ثابت کند تمام Entityهای قابل ایجاد بدون از دست رفتن اطلاعات بازیابی می‌شوند.

## 56. معیار Report و UI
هر Report باید Scope، Jalali Date، Time و Report Type داشته باشد. تمام صفحات باید از Style Guide مرجع پیروی کنند و رنگ، Typography، Status و Interaction در کل برنامه یکسان باشند.

## 57. اصول نهایی پروژه
- Camera یک سیستم موازی نیست؛ Camera = Device + Camera Profile.
- NVR/DVR نیز Device هستند.
- PC = Device + PC Profile.
- Discovery = یک Discovery Engine عمومی + Adapterهای تخصصی.
- Remote Access بخشی از Device Management است.
- هر IP می‌تواند صفر تا چند Remote Method داشته باشد.
- Port پیش‌فرض فقط پیشنهاد است و Custom Port همیشه پشتیبانی می‌شود.
- Manual Override کاربر بر Auto Discovery اولویت دارد.
- سیستم برای LAN، Private APN و Routed Private Networks قابل استفاده باشد و اینترنت عمومی الزام معماری نباشد.
- «هر اطلاعاتی که بتوان به‌صورت مطمئن و کم‌فشار از شبکه استخراج کرد، سیستم خودش استخراج کند؛ ولی کاربر همیشه امکان مشاهده، اصلاح و تکمیل آن را داشته باشد.»
- Discovery نباید داده Manual را بدون اجازه تغییر دهد.
- «کمترین شلوغی، بیشترین اطلاعات مهم، دسترسی سریع به مشکل‌ها.»
- «هر داده مهم باید قابل مشاهده، قابل پیگیری، قابل گزارش، قابل Backup و قابل Restore باشد.»
- «سادگی در ظاهر، قدرت در عملکرد، حفظ کامل تاریخچه، استفاده از هسته مشترک، جلوگیری از معماری موازی و آمادگی برای توسعه آینده.»

## 58. وضعیت نسخه
این سند نسخه مرجع 1.1 و جایگزین نسخه مرجع 1.0 است. موارد افزوده و یکپارچه‌شده در نسخه 1.1 شامل Network Model داخل Center، Device Entity مشترک، Multiple Interfaces، Device Relations، Network Discovery، Device Classification، Capability Discovery، Camera/NVR/DVR Monitoring، ONVIF/RTSP، Vendor Adapter Architecture، Camera Identity Database، APN/NAT Discovery Strategy، Local Scanner Agent، Low-Bandwidth Camera Monitoring، Snapshot/Live Stream Policy، PC/Workstation Inventory، Remote Software Detection، RDP، Radmin، RustDesk، VNC، TeamViewer LAN، Remote Icons، Custom Remote Ports، Manual Remote Override، Unified Remote Button و LAN-Only/Private Network Remote Policy است.

از این مرحله دیتابیس، UI، معماری، تست‌ها، مستندات و کدنویسی باید با همین نسخه تطبیق داده شوند. هر توسعه جدید ابتدا بررسی کند آیا می‌تواند روی Entityها، Serviceها و Engineهای موجود ساخته شود؛ ایجاد سیستم موازی فقط زمانی مجاز است که دلیل معماری مستند و تأییدشده وجود داشته باشد.
