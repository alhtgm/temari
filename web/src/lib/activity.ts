/**
 * 活動データの計算（ポイントへの換算・日付・時間帯）。
 *
 * 換算の数字は Android アプリの config（ActivityConfig・SensingConfig）と同じにしている。
 * アプリ側を変えたら、ここも合わせて変える（仮の値）。
 */

/** 歩数をいくつで「歩いたポイント 1」とするか（アプリの ActivityConfig.STEPS_PER_WALK_POINT）。 */
export const STEPS_PER_WALK_POINT = 10;

/** 「家のこと」として動いた何秒で 1 ポイントか（アプリの SensingConfig.LIFE_SECONDS_PER_POINT）。 */
export const LIFE_SECONDS_PER_POINT = 20;

/** 「家のこと」と判別する揺れのしきい値（mg）（アプリの SensingConfig.LIFE_ENMO_THRESHOLD_MG）。 */
export const LIFE_ENMO_THRESHOLD_MG = 25;

/** 最後の記録からこれ以上たったら「記録が止まっているかも」と示す（分）。送る間隔 15 分の 4 倍。 */
export const STALE_MINUTES = 60;

/** 施設の時間帯（日付と時刻はこの地域で数える）。 */
export const TIME_ZONE = "Asia/Tokyo";

/** 時間帯（アプリの TimeBlock と同じ名前と順番）。 */
export const BLOCKS = [
  { key: "Morning", label: "朝", range: "5〜10時" },
  { key: "Noon", label: "昼", range: "10〜14時" },
  { key: "Afternoon", label: "午後", range: "14〜18時" },
  { key: "Night", label: "夜", range: "18〜24時" },
] as const;

export type BlockKey = (typeof BLOCKS)[number]["key"];

/** Supabase の app_users 表の1行。 */
export type AppUser = {
  id: string;
  nickname: string;
  daily_goal: number;
  recording: boolean;
  last_recorded_at: string | null;
  last_synced_at: string;
  created_at: string;
};

/** Supabase の block_records 表の1行。 */
export type BlockRecord = {
  user_id: string;
  date: string;
  block: BlockKey;
  steps: number;
  life_seconds: number;
};

/** Supabase の epochs 表の1行。 */
export type Epoch = {
  recorded_at: string;
  steps: number;
  life_seconds: number;
  enmo_mg: number;
  walking: boolean;
};

/** 1つの時間帯（または1日）のポイント。 */
export type Points = { walk: number; life: number; steps: number };

/**
 * 時間帯1つ分の記録をポイントに直す。
 * アプリと同じく、時間帯ごとに割り算して小数を切り捨ててから足す（アプリの表示と数字をそろえるため）。
 */
export function blockPoints(record: Pick<BlockRecord, "steps" | "life_seconds">): Points {
  return {
    walk: Math.floor(record.steps / STEPS_PER_WALK_POINT),
    life: Math.floor(record.life_seconds / LIFE_SECONDS_PER_POINT),
    steps: record.steps,
  };
}

/** 記録を足し合わせたポイント。 */
export function sumPoints(records: Pick<BlockRecord, "steps" | "life_seconds">[]): Points {
  return records.map(blockPoints).reduce(
    (a, b) => ({ walk: a.walk + b.walk, life: a.life + b.life, steps: a.steps + b.steps }),
    { walk: 0, life: 0, steps: 0 },
  );
}

/** 日本時間の今日の日付（"2026-11-13"）。 */
export function todayInTimeZone(now: Date = new Date()): string {
  // en-CA は「年-月-日」の形で日付を出すので、そのまま表の date 列と比べられる。
  return new Intl.DateTimeFormat("en-CA", { timeZone: TIME_ZONE }).format(now);
}

/** "2026-11-13" に days 日を足した日付。 */
export function addDays(date: string, days: number): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

/** その日を含む週の月曜日。 */
export function weekStartOf(date: string): string {
  const day = new Date(`${date}T00:00:00Z`).getUTCDay(); // 0 = 日曜
  return addDays(date, -((day + 6) % 7));
}

/** 曜日の名前（月〜日）。 */
export function weekdayLabel(date: string): string {
  return ["日", "月", "火", "水", "木", "金", "土"][new Date(`${date}T00:00:00Z`).getUTCDay()];
}

/** 日時を日本時間の「11/13 10:05」の形にする。 */
export function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  return new Intl.DateTimeFormat("ja-JP", {
    timeZone: TIME_ZONE,
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

/** 何分前か（「3分前」「2時間前」）。 */
export function ago(iso: string | null, now: Date = new Date()): string {
  if (!iso) return "まだ記録がありません";
  const minutes = Math.round((now.getTime() - new Date(iso).getTime()) / 60000);
  if (minutes < 1) return "たった今";
  if (minutes < 60) return `${minutes}分前`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}時間前`;
  return `${Math.floor(hours / 24)}日前`;
}

/** 記録が止まっているかもしれないか（サービスが止まっている、または最後の記録が古い）。 */
export function isStale(user: AppUser, now: Date = new Date()): boolean {
  if (!user.recording) return true;
  if (!user.last_recorded_at) return true;
  return now.getTime() - new Date(user.last_recorded_at).getTime() > STALE_MINUTES * 60000;
}

/** 日本時間のその日の 0:00 を表す、世界共通の時刻（ISO 形式）。 */
export function startOfDayIso(date: string): string {
  // 日本は夏時間が無いので、いつも +09:00。
  return new Date(`${date}T00:00:00+09:00`).toISOString();
}
