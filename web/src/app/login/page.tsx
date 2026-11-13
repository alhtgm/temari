import { LoginForm } from "./login-form";

/** ログイン画面。新規登録は作らない（スタッフは Supabase の管理画面で登録する）。 */
export default function LoginPage() {
  return (
    <main className="flex flex-1 items-center justify-center px-4 py-16">
      <div className="w-full max-w-sm rounded-3xl border border-border bg-surface p-8 shadow-sm">
        <div className="mb-6 text-center">
          <div className="text-4xl" aria-hidden>
            🧶
          </div>
          <h1 className="mt-2 text-2xl font-bold">てまり スタッフ</h1>
          <p className="mt-1 text-sm text-muted">利用者の活動データ管理画面</p>
        </div>
        <LoginForm />
      </div>
    </main>
  );
}
