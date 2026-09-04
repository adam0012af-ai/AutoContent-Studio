# AutoContent Studio

منصة مستقلة لإنتاج المحتوى بالذكاء الاصطناعي وتجهيزه للنشر، مع موافقة يدوية قبل النشر في النسخة الأولى.

## MVP الحالي

- TikTok كأول منصة
- إعداد فكرة الفيديو
- تحديد عدد الفيديوهات اليومية
- Pipeline مرئي: Idea → Script → Voice → Visuals → Edit → Captions → QA → Preview
- Preview رأسي 9:16
- Approve & Publish
- Reject & Regenerate
- لا يحتاج API keys في النسخة الحالية

## التشغيل محليًا

```bash
npm install
npm run dev
```

ثم افتح:

```text
http://localhost:3000
```

## المرحلة التالية

1. ربط مولد Script حقيقي.
2. إضافة TTS.
3. إضافة توليد/اختيار Visuals.
4. إضافة FFmpeg rendering worker.
5. إضافة TikTok OAuth و Content Posting API.
6. إضافة Queue وجدولة ونظام مراجعة قبل النشر.

> لا تضع أي مفاتيح API أو أسرار داخل GitHub. استخدم environment variables / repository secrets فقط.
