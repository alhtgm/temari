import { createServerClient } from "@supabase/ssr";
import { cookies } from "next/headers";

/**
 * サーバー側（ページを作るとき）で使う Supabase の窓口。
 *
 * ログインの状態はブラウザの cookie に入っているので、それを読んで「誰として」データを読むかを決める。
 * 読める範囲は Supabase の決まり（RLS）で制限されるので、スタッフでなければ何も読めない。
 */
export async function createClient() {
  const cookieStore = await cookies();
  return createServerClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY!,
    {
      cookies: {
        getAll() {
          return cookieStore.getAll();
        },
        setAll(cookiesToSet) {
          try {
            cookiesToSet.forEach(({ name, value, options }) => cookieStore.set(name, value, options));
          } catch {
            // ページを作っている途中では cookie を書けない。ログインの更新は proxy.ts が行うので、ここでは無視してよい。
          }
        },
      },
    },
  );
}
