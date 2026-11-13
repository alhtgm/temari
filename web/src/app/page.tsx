import Link from "next/link";
import { Header, NotStaff } from "@/components/header";
import {
  STALE_MINUTES,
  ago,
  formatDateTime,
  isStale,
  sumPoints,
  todayInTimeZone,
  weekdayLabel,
  type AppUser,
  type Points,
} from "@/lib/activity";
import { fetchBlocks, fetchExercises, fetchUsers, requireStaff } from "@/lib/data";

// 開くたびに最新のデータを読む（作り置きしない）。
export const dynamic = "force-dynamic";

/** 一覧の1行分。 */
type Row = {
  user: AppUser;
  points: Points;
  total: number;
  ratio: number;
  exercises: number;
  stale: boolean;
};

/**
 * 利用者一覧: 全員の本日の数値を表で並べ、見比べやすくする。
 * 記録停止の可能性がある人を先頭に、その次は達成率の低い順に並べる（声をかける必要がある人を上に）。
 */
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

  const rows: Row[] = users
    .map((user) => {
      const points = sumPoints(blocks.filter((b) => b.user_id === user.id));
      const total = points.walk + points.life;
      return {
        user,
        points,
        total,
        ratio: user.daily_goal > 0 ? total / user.daily_goal : 0,
        exercises: exercises.get(user.id) ?? 0,
        stale: isStale(user, now),
      };
    })
    .sort((a, b) => Number(b.stale) - Number(a.stale) || a.ratio - b.ratio);

  const achievedCount = rows.filter((r) => r.ratio >= 1).length;
  const staleCount = rows.filter((r) => r.stale).length;
  const averageRatio = rows.length ? rows.reduce((s, r) => s + r.ratio, 0) / rows.length : 0;

  return (
    <>
      <Header staffName={staffName} />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6">
        <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2">
          <h1 className="text-2xl font-bold">利用者一覧</h1>
          <p className="text-sm text-muted">
            {today}（{weekdayLabel(today)}）・日本時間・{formatDateTime(now.toISOString())} 時点
          </p>
        </div>

        <section className="mb-6 grid grid-cols-2 gap-3 md:grid-cols-4">
          <Kpi label="利用者数" value={`${rows.length}`} unit="人" />
          <Kpi label="本日の目標達成" value={`${achievedCount}`} unit={`/ ${rows.length} 人`} />
          <Kpi label="平均達成率" value={`${Math.round(averageRatio * 100)}`} unit="%" />
          <Kpi
            label="記録停止の可能性"
            value={`${staleCount}`}
            unit="人"
            alert={staleCount > 0}
            note={`最終記録から${STALE_MINUTES}分以上、または記録サービス停止`}
          />
        </section>

        {rows.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-border p-10 text-center text-muted">
            利用者のデータがまだ届いていません。アプリの初回設定を済ませ、アプリ内のスタッフ用画面で「今すぐ送る」を押すと表示されます。
          </div>
        ) : (
          <div className="overflow-x-auto rounded-2xl border border-border bg-surface shadow-sm">
            <table className="w-full min-w-[960px] text-sm">
              <thead className="bg-background text-left text-xs text-muted">
                <tr>
                  <Th>状態</Th>
                  <Th>利用者</Th>
                  <Th align="right">活動pt / 目標</Th>
                  <Th>達成率</Th>
                  <Th align="right">歩数</Th>
                  <Th align="right">歩行pt</Th>
                  <Th align="right">生活活動pt</Th>
                  <Th align="right">体操</Th>
                  <Th>最終記録</Th>
                  <Th>最終受信</Th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <UserRow key={row.user.id} row={row} />
                ))}
              </tbody>
            </table>
          </div>
        )}
        <p className="mt-3 text-xs text-muted">
          活動pt = 歩行pt（歩数 ÷ 10）+ 生活活動pt（家事などの動き 20 秒 = 1pt）。行を押すと詳細を表示します。データは端末から約15分ごとに届きます。
        </p>
      </main>
    </>
  );
}

function UserRow({ row }: { row: Row }) {
  const { user, points, total, ratio, exercises, stale } = row;
  const href = `/users/${user.id}`;
  const achieved = ratio >= 1;
  return (
    <tr className={`border-t border-border hover:bg-primary-soft/40 ${stale ? "bg-vermilion-soft/40" : ""}`}>
      <Td>
        {stale ? (
          <span className="inline-flex items-center gap-1 rounded-full bg-vermilion px-2 py-0.5 text-xs font-bold text-white">
            ⚠ 停止の可能性
          </span>
        ) : (
          <span className="inline-flex items-center gap-1 rounded-full bg-green-soft px-2 py-0.5 text-xs font-bold text-green">
            ● 記録中
          </span>
        )}
      </Td>
      <Td>
        <Link href={href} className="font-bold text-primary hover:underline">
          {user.nickname || "（呼び名なし）"}
        </Link>
      </Td>
      <Td align="right">
        <span className="text-base font-bold tabular-nums">{total.toLocaleString()}</span>
        <span className="text-muted tabular-nums"> / {user.daily_goal.toLocaleString()}</span>
      </Td>
      <Td>
        <div className="flex items-center gap-2">
          <div className="h-2 w-24 overflow-hidden rounded-full bg-primary-soft" aria-hidden>
            <div
              className={`h-full rounded-full ${achieved ? "bg-yellow" : "bg-primary"}`}
              style={{ width: `${Math.min(100, ratio * 100)}%` }}
            />
          </div>
          <span className="w-16 font-bold tabular-nums">
            {Math.round(ratio * 100)}%{achieved && <span className="text-vermilion"> ★</span>}
          </span>
        </div>
      </Td>
      <Td align="right" className="tabular-nums">{points.steps.toLocaleString()}</Td>
      <Td align="right" className="tabular-nums">{points.walk.toLocaleString()}</Td>
      <Td align="right" className="tabular-nums">{points.life.toLocaleString()}</Td>
      <Td align="right" className="tabular-nums">{exercises}回</Td>
      <Td className={stale ? "font-bold text-vermilion" : ""}>
        {formatDateTime(user.last_recorded_at)}
        <span className="block text-xs text-muted">{ago(user.last_recorded_at)}</span>
      </Td>
      <Td>
        {formatDateTime(user.last_synced_at)}
        <span className="block text-xs text-muted">{ago(user.last_synced_at)}</span>
      </Td>
    </tr>
  );
}

function Kpi({
  label,
  value,
  unit,
  alert,
  note,
}: {
  label: string;
  value: string;
  unit: string;
  alert?: boolean;
  note?: string;
}) {
  return (
    <div
      className={`rounded-2xl border p-4 ${alert ? "border-vermilion bg-vermilion-soft" : "border-border bg-surface"}`}
    >
      <div className={`text-xs ${alert ? "font-bold text-vermilion" : "text-muted"}`}>
        {alert && "⚠ "}
        {label}
      </div>
      <div className="mt-1 flex items-baseline gap-1">
        <span className="text-3xl font-bold tabular-nums">{value}</span>
        <span className="text-sm text-muted">{unit}</span>
      </div>
      {note && <div className="mt-1 text-[11px] text-muted">{note}</div>}
    </div>
  );
}

function Th({ children, align = "left" }: { children: React.ReactNode; align?: "left" | "right" }) {
  return (
    <th scope="col" className={`whitespace-nowrap px-3 py-2 font-medium ${align === "right" ? "text-right" : ""}`}>
      {children}
    </th>
  );
}

function Td({
  children,
  align = "left",
  className = "",
}: {
  children: React.ReactNode;
  align?: "left" | "right";
  className?: string;
}) {
  return (
    <td className={`whitespace-nowrap px-3 py-2.5 align-middle ${align === "right" ? "text-right" : ""} ${className}`}>
      {children}
    </td>
  );
}
