"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { createClient } from "@/lib/supabase/client";

/** メールアドレスとパスワードの入力欄。ブラウザの中で Supabase にログインする。 */
export function LoginForm() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function onSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const { error } = await createClient().auth.signInWithPassword({ email, password });
    setBusy(false);
    if (error) {
      // 何が違ったか（メールかパスワードか）は、安全のために分けて伝えない。
      setError("メールアドレスかパスワードが違います");
      return;
    }
    router.replace("/");
    router.refresh();
  }

  return (
    <form onSubmit={onSubmit} className="flex flex-col gap-4">
      <label className="flex flex-col gap-1 text-sm font-medium">
        メールアドレス
        <input
          type="email"
          required
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className="rounded-xl border border-border bg-background px-3 py-2 text-base outline-none focus:border-primary"
        />
      </label>
      <label className="flex flex-col gap-1 text-sm font-medium">
        パスワード
        <input
          type="password"
          required
          autoComplete="current-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="rounded-xl border border-border bg-background px-3 py-2 text-base outline-none focus:border-primary"
        />
      </label>
      {error && (
        <p role="alert" className="rounded-xl bg-vermilion-soft px-3 py-2 text-sm text-vermilion">
          {error}
        </p>
      )}
      <button
        type="submit"
        disabled={busy}
        className="rounded-xl bg-primary px-4 py-3 font-bold text-white transition hover:opacity-90 disabled:opacity-50"
      >
        {busy ? "ログインしています…" : "ログイン"}
      </button>
    </form>
  );
}
