import Link from "next/link";
import { notFound } from "next/navigation";
import { EnmoChart, StackedBarChart, type Bar } from "@/components/charts";
import { Header, NotStaff } from "@/components/header";
import {
  BLOCKS,
  LIFE_ENMO_THRESHOLD_MG,
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
import { fetchBlocks, fetchEpochs, fetchUser, requireStaff } from "@/lib/data";

export const dynamic = "force-dynamic";

/**
 * 利用者ごとの画面: きょうの時間帯・今週の7日・きょうの 10 秒ごとの揺れ。
 *
 * ?date=2026-11-12 で、ほかの日を見られる（10 秒ごとの記録は 30 日で消える）。
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
  const [weekBlocks, epochs] = await Promise.all([
    fetchBlocks(supabase, weekStart, addDays(weekStart, 6), id),
    fetchEpochs(supabase, id, startOfDayIso(day), startOfDayIso(addDays(day, 1))),
  ]);

  const dayBlocks = weekBlocks.filter((b) => b.date === day);
  const dayPoints = sumPoints(dayBlocks);
  const dayTotal = dayPoints.walk + dayPoints.life;

  const todayBars: Bar[] = BLOCKS.map((block) => {
    const record = dayBlocks.find((b) => b.block === block.key);
    const p = record ? blockPoints(record) : { walk: 0, life: 0, steps: 0 };
    return { label: block.label, sub: block.range, walk: p.walk, life: p.life };
  });
  const weekBars: Bar[] = Array.from({ length: 7 }, (_, i) => {
    const date = addDays(weekStart, i);
    const p = sumPoints(weekBlocks.filter((b) => b.date === date));
    return {
      label: weekdayLabel(date),
      sub: date.slice(5).replace("-", "/"),
      walk: p.walk,
      life: p.life,
      achieved: p.walk + p.life >= user.daily_goal,
    };
  });
  const lifeEpochs = epochs.filter((e) => e.life_seconds > 0).length;
  const walkEpochs = epochs.filter((e) => e.walking).length;
  const stale = isStale(user);

  return (
    <>
      <Header staffName={staffName} />
      <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">
        <Link href="/" className="text-sm text-primary hover:underline">
          ← 一覧にもどる
        </Link>
        <div className="mt-2 flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold">{user.nickname || "（よび名なし）"}</h1>
            <p className={`mt-1 text-sm ${stale ? "font-bold text-vermilion" : "text-muted"}`}>
              {stale ? "⚠ 記録が止まっているかもしれません" : "● 記録中"}・最後の記録 {formatDateTime(user.last_recorded_at)}（
              {ago(user.last_recorded_at)}）・最後に届いた {ago(user.last_synced_at)}
            </p>
          </div>
          <DayPicker id={id} day={day} today={today} />
        </div>

        <section className="mt-6 grid gap-4 sm:grid-cols-4">
          <Summary label="活動ポイント" value={`${dayTotal}`} unit={`/ ${user.daily_goal} pt`} strong />
          <Summary label="歩いた" value={`${dayPoints.walk}`} unit={`pt（${dayPoints.steps.toLocaleString()}歩）`} />
          <Summary label="家のこと" value={`${dayPoints.life}`} unit="pt" />
          <Summary label="目標まで" value={dayTotal >= user.daily_goal ? "たっせい" : `${user.daily_goal - dayTotal}`}
            unit={dayTotal >= user.daily_goal ? "★" : "pt"} />
        </section>

        <div className="mt-6 grid gap-6 lg:grid-cols-2">
          <Card title={`${day === today ? "きょう" : day} の時間帯`}>
            <StackedBarChart bars={todayBars} />
          </Card>
          <Card title="この週（月〜日）">
            <StackedBarChart bars={weekBars} goal={user.daily_goal} />
          </Card>
        </div>

        <Card
          title="10秒ごとの揺れ（しきい値の調整用）"
          className="mt-6"
          note={`記録 ${epochs.length.toLocaleString()}件・歩いた ${walkEpochs}件・家のこと ${lifeEpochs}件。` +
            `灰色の点がしきい値 ${LIFE_ENMO_THRESHOLD_MG}mg の線のすぐ下に多いときは、しきい値が高すぎるかもしれません。`}
        >
          <EnmoChart epochs={epochs} />
        </Card>
      </main>
    </>
  );
}

/** 見る日を前後に動かすリンク。 */
function DayPicker({ id, day, today }: { id: string; day: string; today: string }) {
  const prev = addDays(day, -1);
  const next = addDays(day, 1);
  return (
    <nav className="flex items-center gap-2 text-sm" aria-label="見る日">
      <Link href={`/users/${id}?date=${prev}`} className="rounded-full border border-border px-3 py-1.5 hover:bg-primary-soft">
        ← 前の日
      </Link>
      <span className="font-bold">{day}（{weekdayLabel(day)}）</span>
      {day < today ? (
        <Link href={`/users/${id}?date=${next}`} className="rounded-full border border-border px-3 py-1.5 hover:bg-primary-soft">
          次の日 →
        </Link>
      ) : (
        <span className="rounded-full border border-border px-3 py-1.5 text-muted opacity-50">次の日 →</span>
      )}
    </nav>
  );
}

function Summary({ label, value, unit, strong }: { label: string; value: string; unit: string; strong?: boolean }) {
  return (
    <div className={`rounded-3xl p-5 ${strong ? "bg-primary text-white" : "border border-border bg-surface"}`}>
      <div className={`text-sm ${strong ? "text-white/80" : "text-muted"}`}>{label}</div>
      <div className="mt-1 flex items-baseline gap-1">
        <span className="text-3xl font-bold">{value}</span>
        <span className={`text-sm ${strong ? "text-white/80" : "text-muted"}`}>{unit}</span>
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
    <section className={`rounded-3xl border border-border bg-surface p-5 shadow-sm ${className}`}>
      <h2 className="mb-3 font-bold">{title}</h2>
      {children}
      {note && <p className="mt-3 text-xs text-muted">{note}</p>}
    </section>
  );
}
