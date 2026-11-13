"use server";

import { redirect } from "next/navigation";
import { createClient } from "@/lib/supabase/server";

/** ログアウトして、ログイン画面に戻る。 */
export async function signOut() {
  const supabase = await createClient();
  await supabase.auth.signOut();
  redirect("/login");
}
