import Link from "next/link";
import { notFound } from "next/navigation";
import { EnmoChart, StackedBarChart, type Bar } from "@/components/charts";
import { Header, NotStaff } from "@/components/header";
import {
  BLOCKS,
  LIFE_ENMO_THRESHOLD_MG,
  STALE_MINUTES,
  addDays,
  ago,
  blockPoints,
  formatDateTime,
  isStale,
  startOfDayIso,
  sumPoints,
  todayInTimeZone,
  weekStartOf,
  weekdayLabel,
} from "@/lib/activity";
import { fetchBlocks, fetchEpochs, fetchExercises, fetchUser, requireStaff } from "@/lib/data";

export const dynamic = "force-dynamic";

/** 10 秒ごとの記録 1 件の長さ（秒）。アプリの SensingConfig.EPOCH_SECONDS と同じ。 */
const EPOCH_SECONDS = 10;

/**
 * 利用者詳細: 指定日の時間帯別・週間推移・10 秒ごとの加速度（ENMO）。
 *
 * ?date=YYYY-MM-DD で別の日を表示する（10 秒ごとの記録は 30 日で自動削除）。
 */
export default async function UserPage({ params, searchParams }: PageProps<"/users/[id]">) {
  const { id } = await params;
  const query = await searchParams;
  const { supabase, isStaff, staffName } = await requireStaff();
  if (!isStaff) return <NotStaff />;

  const today = todayInTimeZone();
  const requested = typeof query.date === "string" && /^\d{4}-\d{2}-\d{2}$/.test(query.date) ? query.date : today;
  const day = requested > today ? today : requested;
  const weekStart = weekStartOf(day);

  const user = await fetchUser(supabase, id);
  if (!user) notFound();
  const [weekBlocks, epochs, exercises] = await Promise.all([
    fetchBlocks(supabase, weekStart, addDays(weekStart, 6), id),
    fetchEpochs(supabase, id, startOfDayIso(day), startOfDayIso(addDays(day, 1))),
    fetchExercises(supabase, day),
  ]);

  const dayBlocks = weekBlocks.filter((b) => b.date === day);
  const dayPoints = sumPoints(dayBlocks);
  const dayTotal = dayPoints.walk + dayPoints.life;
  const ratio = user.daily_goal > 0 ? dayTotal / user.daily_goal : 0;

  const blockBars: Bar[] = BLOCKS.map((block) => {
    const record = dayBlocks.find((b) => b.block === block.key);
    const p = record ? blockPoints(record) : { walk: 0, life: 0, steps: 0 };
    return { label: block.label, sub: block.range, walk: p.walk, life: p.life };
  });
  const weekDays = Array.from({ length: 7 }, (_, i) => {
    const date = addDays(weekStart, i);
    return { date, points: sumPoints(weekBlocks.filter((b) => b.date === date)) };
  });
  const weekBars: Bar[] = weekDays.map(({ date, points }) => ({
    label: weekdayLabel(date),
    sub: date.slice(5).replace("-", "/"),
    walk: points.walk,
    life: points.life,
    achieved: points.walk + points.life >= user.daily_goal,
  }));
  const weekAchieved = weekBars.filter((b) => b.achieved).length;
  const weekSteps = weekDays.reduce((s, d) => s + d.points.steps, 0);

  const walkMinutes = Math.round((epochs.filter((e) => e.walking).length * EPOCH_SECONDS) / 60);
  const lifeMinutes = Math.round((epochs.filter((e) => e.life_seconds > 0).length * EPOCH_SECONDS) / 60);
  const recordedMinutes = Math.round((epochs.length * EPOCH_SECONDS) / 60);
  const nearThreshold = epochs.filter(
    (e) => !e.walking && e.life_seconds === 0 && e.enmo_mg >= LIFE_ENMO_THRESHOLD_MG * 0.6,
  ).length;
  const stale = isStale(user);

  return (
    <>
      <Header staffName={staffName} />
      <main className="mx-auto w-full max-w-7xl flex-1 px-4 py-6">
        <Link href="/" className="text-sm text-primary hover:underline">
          ← 利用者一覧
        </Link>

        <div className="mt-2 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold">{user.nickname || "（呼び名なし）"}</h1>
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm">
              {stale ? (
                <span className="rounded-full bg-vermilion px-2 py-0.5 text-xs font-bold text-white">⚠ 記録停止の可能性</span>
              ) : (
                <span className="rounded-full bg-green-soft px-2 py-0.5 text-xs font-bold text-green">● 記録中</span>
              )}
              <span className="text-muted">
                最終記録 {formatDateTime(user.last_recorded_at)}（{ago(user.last_recorded_at)}）／最終受信{" "}
                {formatDateTime(user.last_synced_at)}（{ago(user.last_synced_at)}）
              </span>
            </div>
          </div>
          <DayPicker id={id} day={day} today={today} />
        </div>

        <section className="mt-5 grid grid-cols-2 gap-3 md:grid-cols-3 lg:grid-cols-6">
          <Kpi label="活動pt" value={dayTotal.toLocaleString()} unit={`/ ${user.daily_goal.toLocaleString()}`} strong />
          <Kpi label="達成率" value={`${Math.round(ratio * 100)}`} unit={ratio >= 1 ? "% ★達成" : "%"} />
          <Kpi label="歩行pt" value={dayPoints.walk.toLocaleString()} unit={`（${dayPoints.steps.toLocaleString()}歩）`} />
          <Kpi label="生活活動pt" value={dayPoints.life.toLocaleString()} unit="pt" />
          <Kpi label="体操" value={`${exercises.get(id) ?? 0}`} unit="回" />
          <Kpi
            label="目標まで"
            value={dayTotal >= user.daily_goal ? "0" : (user.daily_goal - dayTotal).toLocaleString()}
            unit="pt"
          />
        </section>

        <div className="mt-5 grid gap-5 lg:grid-cols-2">
          <Card title={`時間帯別（${day}）`}>
            <StackedBarChart bars={blockBars} />
            <BlockTable blocks={dayBlocks} />
          </Card>
          <Card
            title={`週間推移（${weekStart} 〜 ${addDays(weekStart, 6)}）`}
            note={`目標達成 ${weekAchieved}日／週合計 ${weekSteps.toLocaleString()}歩`}
          >
            <StackedBarChart bars={weekBars} goal={user.daily_goal} />
          </Card>
        </div>

        <Card
          title={`10秒ごとの加速度（ENMO）— ${day}`}
          className="mt-5"
          note={
            `記録 ${epochs.length.toLocaleString()}件（約${recordedMinutes}分）／歩行と判定 約${walkMinutes}分／` +
            `生活活動と判定 約${lifeMinutes}分。` +
            `生活活動のしきい値は ${LIFE_ENMO_THRESHOLD_MG}mg（仮）。しきい値の 60% 以上〜未満で「静止」と判定された記録が ` +
            `${nearThreshold.toLocaleString()}件あり、多い場合はしきい値が高すぎる可能性があります。`
          }
        >
          <EnmoChart epochs={epochs} />
        </Card>

        <dl className="mt-5 grid gap-x-6 gap-y-1 rounded-2xl border border-border bg-surface p-4 text-xs text-muted sm:grid-cols-2">
          <div><dt className="inline">利用者ID: </dt><dd className="inline font-mono">{user.id}</dd></div>
          <div><dt className="inline">登録日時: </dt><dd className="inline">{formatDateTime(user.created_at)}</dd></div>
          <div><dt className="inline">1日の目標: </dt><dd className="inline">{user.daily_goal}pt</dd></div>
          <div>
            <dt className="inline">停止の判定: </dt>
            <dd className="inline">記録サービス停止、または最終記録から{STALE_MINUTES}分以上</dd>
          </div>
        </dl>
      </main>
    </>
  );
}

/** 時間帯ごとの数値の表（グラフの数値を正確に読めるように）。 */
function BlockTable({ blocks }: { blocks: { block: string; steps: number; life_seconds: number }[] }) {
  return (
    <table className="mt-4 w-full text-sm">
      <thead className="text-xs text-muted">
        <tr className="border-b border-border">
          <th className="py-1 text-left font-medium">時間帯</th>
          <th className="py-1 text-right font-medium">歩数</th>
          <th className="py-1 text-right font-medium">歩行pt</th>
          <th className="py-1 text-right font-medium">生活活動</th>
          <th className="py-1 text-right font-medium">生活活動pt</th>
          <th className="py-1 text-right font-medium">合計pt</th>
        </tr>
      </thead>
      <tbody className="tabular-nums">
        {BLOCKS.map((block) => {
          const record = blocks.find((b) => b.block === block.key) ?? { steps: 0, life_seconds: 0 };
          const p = blockPoints(record);
          return (
            <tr key={block.key} className="border-b border-border last:border-0">
              <td className="py-1">
                {block.label} <span className="text-xs text-muted">{block.range}</span>
              </td>
              <td className="py-1 text-right">{record.steps.toLocaleString()}</td>
              <td className="py-1 text-right">{p.walk}</td>
              <td className="py-1 text-right">{Math.round(record.life_seconds / 60)}分</td>
              <td className="py-1 text-right">{p.life}</td>
              <td className="py-1 text-right font-bold">{p.walk + p.life}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

/** 表示する日を前後に移動するリンク。 */
function DayPicker({ id, day, today }: { id: string; day: string; today: string }) {
  const linkClass = "rounded-full border border-border px-3 py-1.5 hover:bg-primary-soft";
  return (
    <nav className="flex items-center gap-2 text-sm" aria-label="表示する日">
      <Link href={`/users/${id}?date=${addDays(day, -1)}`} className={linkClass}>
        ← 前日
      </Link>
      <span className="font-bold tabular-nums">
        {day}（{weekdayLabel(day)}）
      </span>
      {day < today ? (
        <>
          <Link href={`/users/${id}?date=${addDays(day, 1)}`} className={linkClass}>
            翌日 →
          </Link>
          <Link href={`/users/${id}`} className={linkClass}>
            本日
          </Link>
        </>
      ) : (
        <span className="rounded-full border border-border px-3 py-1.5 text-muted opacity-50">翌日 →</span>
      )}
    </nav>
  );
}

function Kpi({ label, value, unit, strong }: { label: string; value: string; unit: string; strong?: boolean }) {
  return (
    <div className={`rounded-2xl p-4 ${strong ? "bg-primary text-white" : "border border-border bg-surface"}`}>
      <div className={`text-xs ${strong ? "text-white/80" : "text-muted"}`}>{label}</div>
      <div className="mt-1 flex flex-wrap items-baseline gap-1">
        <span className="text-2xl font-bold tabular-nums">{value}</span>
        <span className={`text-xs ${strong ? "text-white/80" : "text-muted"}`}>{unit}</span>
      </div>
    </div>
  );
}

function Card({
  title,
  note,
  className = "",
  children,
}: {
  title: string;
  note?: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <section className={`rounded-2xl border border-border bg-surface p-5 shadow-sm ${className}`}>
      <h2 className="mb-3 font-bold">{title}</h2>
      {children}
      {note && <p className="mt-3 text-xs text-muted">{note}</p>}
    </section>
  );
}
