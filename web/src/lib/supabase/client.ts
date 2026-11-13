import { createBrowserClient } from "@supabase/ssr";

/** ブラウザ側（ログインの入力など）で使う Supabase の窓口。 */
export function createClient() {
  return createBrowserClient(
    process.env.NEXT_PUBLIC_SUPABASE_URL!,
    process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY!,
  );
}
