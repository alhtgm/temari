import Link from "next/link";
import { Header, NotStaff } from "@/components/header";
import { ago, isStale, sumPoints, todayInTimeZone, type AppUser, type BlockRecord } from "@/lib/activity";
import { fetchBlocks, fetchExercises, fetchUsers, requireStaff } from "@/lib/data";

// 開くたびに最新のデータを読む（作り置きしない）。
export const dynamic = "force-dynamic";

/** 一覧: 全利用者の今日のようす。記録が止まっている人を上に出す。 */
export default async function HomePage() {
  const { supabase, isStaff, staffName } = await requireStaff();
  if (!isStaff) return <NotStaff />;

  const today = todayInTimeZone();
  const [users, blocks, exercises] = await Promise.all([
    fetchUsers(supabase),
    fetchBlocks(supabase, today, today),
    fetchExercises(supabase, today),
  ]);
  const now = new Date();
  // 気づいてほしい人（記録が止まっているかもしれない人）を先に並べる。
  const sorted = [...users].sort((a, b) => Number(isStale(b, now)) - Number(isStale(a, now)));
  const staleCount = users.filter((u) => isStale(u, now)).length;

  return (
    <>
      <Header staffName={staffName} />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <div className="mb-6 flex flex-wrap items-end justify-between gap-2">
          <div>
            <h1 className="text-2xl font-bold">きょうの みなさん</h1>
            <p className="text-sm text-muted">{today}（日本時間）・{users.length}人</p>
          </div>
          {staleCount > 0 && (
            <p className="rounded-full bg-vermilion-soft px-3 py-1 text-sm font-bold text-vermilion">
              ⚠ 記録が止まっているかもしれない人: {staleCount}人
            </p>
          )}
        </div>
        {users.length === 0 ? (
          <div className="rounded-3xl border border-dashed border-border p-10 text-center text-muted">
            まだ利用者のデータが届いていません。アプリの初回設定を済ませ、スタッフ用画面の「今すぐ送る」を押すと表示されます。
          </div>
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {sorted.map((user) => (
              <li key={user.id}>
                <UserCard
                  user={user}
                  blocks={blocks.filter((b) => b.user_id === user.id)}
                  exercises={exercises.get(user.id) ?? 0}
                  stale={isStale(user, now)}
                />
              </li>
            ))}
          </ul>
        )}
      </main>
    </>
  );
}

/** 1人分のカード。 */
function UserCard({
  user,
  blocks,
  exercises,
  stale,
}: {
  user: AppUser;
  blocks: BlockRecord[];
  exercises: number;
  stale: boolean;
}) {
  const points = sumPoints(blocks);
  const total = points.walk + points.life;
  const ratio = user.daily_goal > 0 ? total / user.daily_goal : 1;
  const achieved = ratio >= 1;
  return (
    <Link
      href={`/users/${user.id}`}
      className={`block rounded-3xl border bg-surface p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md ${
        stale ? "border-vermilion border-2" : "border-border"
      }`}
    >
      <div className="flex items-start justify-between gap-2">
        <h2 className="text-lg font-bold">{user.nickname || "（よび名なし）"}</h2>
        {achieved && (
          <span className="rounded-full bg-yellow/20 px-2 py-0.5 text-xs font-bold text-vermilion">★ たっせい</span>
        )}
      </div>
      <div className="mt-3 flex items-baseline gap-1">
        <span className="text-4xl font-bold text-primary">{total.toLocaleString()}</span>
        <span className="text-sm text-muted">/ {user.daily_goal.toLocaleString()} pt</span>
      </div>
      {/* 進み具合は色だけでなく % の数字でも示す。 */}
      <div className="mt-2 flex items-center gap-2">
        <div className="h-2.5 flex-1 overflow-hidden rounded-full bg-primary-soft" aria-hidden>
          <div
            className={`h-full rounded-full ${achieved ? "bg-yellow" : "bg-primary"}`}
            style={{ width: `${Math.min(100, ratio * 100)}%` }}
          />
        </div>
        <span className="w-12 text-right text-sm font-bold">{Math.round(ratio * 100)}%</span>
      </div>
      <dl className="mt-4 grid grid-cols-3 gap-2 text-center text-xs">
        <Stat label="歩いた" value={`${points.walk}pt`} sub={`${points.steps.toLocaleString()}歩`} />
        <Stat label="家のこと" value={`${points.life}pt`} />
        <Stat label="たいそう" value={`${exercises}回`} />
      </dl>
      <p className={`mt-4 text-xs ${stale ? "font-bold text-vermilion" : "text-muted"}`}>
        {stale ? "⚠ 記録が止まっているかもしれません・" : "● 記録中・"}最後の記録 {ago(user.last_recorded_at)}
      </p>
    </Link>
  );
}

function Stat({ label, value, sub }: { label: string; value: string; sub?: string }) {
  return (
    <div className="rounded-xl bg-background px-2 py-2">
      <dt className="text-muted">{label}</dt>
      <dd className="text-base font-bold">{value}</dd>
      {sub && <dd className="text-muted">{sub}</dd>}
    </div>
  );
}
