import type { ReactNode } from 'react';
import './globals.css';

export const metadata = {
  title: 'AutoContent Studio',
  description: 'AI-assisted content production and publishing workflow',
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ar" dir="rtl">
      <body>{children}</body>
    </html>
  );
}
