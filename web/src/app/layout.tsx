import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "てまり スタッフ",
  description: "てまりフィットネスの利用者の活動を、スタッフが確かめる画面",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ja" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
