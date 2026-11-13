import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";
import type { AppUser, BlockRecord, Epoch } from "@/lib/activity";

/** 1回の問い合わせで読む最大の行数（Supabase は既定で 1,000 行までしか返さないため、それに合わせて分けて読む）。 */
const PAGE_SIZE = 1000;

/**
 * ログインしていて、スタッフとして登録されているかを確かめる。
 *
 * ログインしていなければログイン画面へ送る。スタッフでなければ isStaff = false を返す
 * （データは決まり（RLS）でどのみち読めないが、画面で理由を伝えるため）。
 */
export async function requireStaff() {
  const supabase = await createClient();
  const { data: claims } = await supabase.auth.getClaims();
  const userId = claims?.claims?.sub;
  if (!userId) redirect("/login");
  const { data: staff } = await supabase.from("staff").select("name").eq("auth_uid", userId).maybeSingle();
  return { supabase, isStaff: Boolean(staff), staffName: (staff?.name as string | undefined) ?? "" };
}

type Client = Awaited<ReturnType<typeof createClient>>;

/** 全利用者（新しく登録した順）。 */
export async function fetchUsers(supabase: Client): Promise<AppUser[]> {
  const { data, error } = await supabase.from("app_users").select("*").order("created_at", { ascending: false });
  if (error) throw error;
  return data as AppUser[];
}

/** 1人の利用者。見つからなければ null。 */
export async function fetchUser(supabase: Client, id: string): Promise<AppUser | null> {
  const { data, error } = await supabase.from("app_users").select("*").eq("id", id).maybeSingle();
  if (error) throw error;
  return data as AppUser | null;
}

/** [from]〜[to] の日の時間帯ごとの記録。userId を指定すればその人だけ。 */
export async function fetchBlocks(
  supabase: Client,
  from: string,
  to: string,
  userId?: string,
): Promise<BlockRecord[]> {
  let query = supabase.from("block_records").select("user_id,date,block,steps,life_seconds").gte("date", from).lte("date", to);
  if (userId) query = query.eq("user_id", userId);
  const { data, error } = await query;
  if (error) throw error;
  return data as BlockRecord[];
}

/** 1人の、[fromIso] から [toIso] までの 10 秒ごとの記録（時刻の順）。多いので 1,000 行ずつ分けて読む。 */
export async function fetchEpochs(supabase: Client, userId: string, fromIso: string, toIso: string): Promise<Epoch[]> {
  const rows: Epoch[] = [];
  for (let start = 0; ; start += PAGE_SIZE) {
    const { data, error } = await supabase
      .from("epochs")
      .select("recorded_at,steps,life_seconds,enmo_mg,walking")
      .eq("user_id", userId)
      .gte("recorded_at", fromIso)
      .lt("recorded_at", toIso)
      .order("recorded_at")
      .range(start, start + PAGE_SIZE - 1);
    if (error) throw error;
    rows.push(...(data as Epoch[]));
    if (!data || data.length < PAGE_SIZE) return rows;
  }
}

/** [date] の体操の回数（userId ごと）。 */
export async function fetchExercises(supabase: Client, date: string): Promise<Map<string, number>> {
  const { data, error } = await supabase.from("daily_extras").select("user_id,exercises").eq("date", date);
  if (error) throw error;
  return new Map((data ?? []).map((row) => [row.user_id as string, row.exercises as number]));
}
