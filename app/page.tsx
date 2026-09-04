'use client';

import { useMemo, useState } from 'react';

type Stage = 'idle' | 'generating' | 'ready' | 'approved';

export default function HomePage() {
  const [topic, setTopic] = useState('فكرة فيديو قصير مفيد ومناسب للجمهور العربي');
  const [dailyCount, setDailyCount] = useState(2);
  const [stage, setStage] = useState<Stage>('idle');
  const [version, setVersion] = useState(1);

  const caption = useMemo(
    () => `نسخة ${version}: محتوى قصير واضح ومفيد، بمقدمة قوية ونهاية تشجع المشاهد على التفاعل. #محتوى #فيديو #تيك_توك`,
    [version],
  );

  function generate() {
    setStage('generating');
    window.setTimeout(() => setStage('ready'), 900);
  }

  function regenerate() {
    setVersion((value) => value + 1);
    setStage('generating');
    window.setTimeout(() => setStage('ready'), 700);
  }

  return (
    <main className="shell">
      <section className="hero">
        <div>
          <p className="eyebrow">AUTOCONTENT STUDIO</p>
          <h1>حوّل فكرة واحدة إلى فيديو جاهز للنشر</h1>
          <p className="lead">
            النسخة الأولى لا تستخدم أي مفاتيح API بعد. هدفها اختبار تجربة العمل كاملة: إعداد القناة، توليد المهمة، المراجعة، ثم الموافقة على النشر.
          </p>
        </div>
        <div className="statusCard">
          <span className="dot" />
          <div>
            <strong>المنصة الأولى</strong>
            <p>TikTok — Manual Approval</p>
          </div>
        </div>
      </section>

      <section className="grid">
        <div className="panel">
          <div className="panelHeader">
            <div>
              <span>01</span>
              <h2>إعداد المهمة</h2>
            </div>
            <span className="badge">MVP</span>
          </div>

          <label>
            موضوع الفيديو
            <textarea value={topic} onChange={(event) => setTopic(event.target.value)} rows={5} />
          </label>

          <div className="row">
            <label>
              المنصة
              <select defaultValue="tiktok">
                <option value="tiktok">TikTok</option>
              </select>
            </label>
            <label>
              فيديوهات يوميًا
              <input
                type="number"
                min={1}
                max={10}
                value={dailyCount}
                onChange={(event) => setDailyCount(Number(event.target.value || 1))}
              />
            </label>
          </div>

          <button className="primary" onClick={generate} disabled={stage === 'generating'}>
            {stage === 'generating' ? 'جاري تجهيز المهمة...' : 'Generate Content Job'}
          </button>
        </div>

        <div className="panel">
          <div className="panelHeader">
            <div>
              <span>02</span>
              <h2>خط الإنتاج</h2>
            </div>
          </div>

          <div className="pipeline">
            {['Idea', 'Script', 'Voice', 'Visuals', 'Edit', 'Captions', 'QA', 'Preview'].map((item, index) => (
              <div className="step" key={item}>
                <span className={stage !== 'idle' && (stage !== 'generating' || index < 4) ? 'done' : ''}>{index + 1}</span>
                <div>
                  <strong>{item}</strong>
                  <small>{stage === 'ready' || stage === 'approved' ? 'جاهز' : stage === 'generating' ? 'قيد التنفيذ' : 'بانتظار البدء'}</small>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="review">
        <div className="phone">
          <div className="phoneTop">9:16 Preview</div>
          <div className="videoMock">
            <span>{stage === 'ready' || stage === 'approved' ? 'PREVIEW READY' : 'VIDEO PREVIEW'}</span>
            <h3>{topic}</h3>
            <p>Auto edit • Captions • Voice • Visuals</p>
          </div>
        </div>

        <div className="reviewPanel">
          <div className="panelHeader">
            <div>
              <span>03</span>
              <h2>المراجعة قبل النشر</h2>
            </div>
            <span className={`badge ${stage === 'approved' ? 'success' : ''}`}>
              {stage === 'approved' ? 'Approved' : 'Needs Approval'}
            </span>
          </div>

          <div className="metaBox">
            <small>Caption + Hashtags</small>
            <p>{caption}</p>
          </div>

          <div className="metaGrid">
            <div><small>جدول اليوم</small><strong>{dailyCount} فيديو</strong></div>
            <div><small>وضع النشر</small><strong>موافقة يدوية</strong></div>
            <div><small>المنصة</small><strong>TikTok</strong></div>
          </div>

          <div className="actions">
            <button className="secondary" onClick={regenerate} disabled={stage !== 'ready'}>Reject & Regenerate</button>
            <button className="approve" onClick={() => setStage('approved')} disabled={stage !== 'ready'}>Approve & Publish</button>
          </div>

          {stage === 'approved' && <p className="notice">تمت الموافقة. في المرحلة القادمة، الزر ده هيكمل للنشر الرسمي عبر TikTok API.</p>}
        </div>
      </section>
    </main>
  );
}
