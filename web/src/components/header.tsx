import Link from "next/link";
import { signOut } from "@/app/actions";

/** どのページにも出す上の帯（アプリ名・スタッフ名・ログアウト）。 */
export function Header({ staffName }: { staffName: string }) {
  return (
    <header className="sticky top-0 z-10 border-b border-border bg-surface/90 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
        <Link href="/" className="flex items-center gap-2 font-bold">
          <span aria-hidden className="text-2xl">
            🧶
          </span>
          <span>てまり スタッフ</span>
        </Link>
        <div className="flex items-center gap-3 text-sm">
          {staffName && <span className="text-muted">{staffName} さん</span>}
          <form action={signOut}>
            <button type="submit" className="rounded-full border border-border px-3 py-1.5 hover:bg-primary-soft">
              ログアウト
            </button>
          </form>
        </div>
      </div>
    </header>
  );
}

/** スタッフとして登録されていないときの案内。 */
export function NotStaff() {
  return (
    <main className="mx-auto max-w-xl flex-1 px-4 py-16">
      <div className="rounded-3xl border border-border bg-surface p-8">
        <h1 className="text-xl font-bold">スタッフとして登録されていません</h1>
        <p className="mt-2 text-muted">
          このアカウントでは利用者のデータを見られません。Supabase の管理画面で staff 表に登録してから、もう一度開いてください。
        </p>
        <form action={signOut} className="mt-6">
          <button type="submit" className="rounded-xl bg-primary px-4 py-2 font-bold text-white">
            ログアウト
          </button>
        </form>
      </div>
    </main>
  );
}
