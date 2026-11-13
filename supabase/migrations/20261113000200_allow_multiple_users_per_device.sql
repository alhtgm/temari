-- step12: 1台の端末（1つの匿名ログイン）が、複数の利用者IDを持てるようにする。
--
-- アプリの「初回設定のやり直し」では、利用者IDは新しくなるが、端末の匿名ログインはそのまま使い続ける。
-- auth_uid に「重なってはいけない」決まりがあると、新しい利用者IDを登録できないため外す。
-- 誰がどの行を書けるかは RLS の決まり（auth_uid が自分かどうか）で守られているので、外しても他人の行は触れない。

alter table public.app_users drop constraint app_users_auth_uid_key;

-- 決まりの中で「auth_uid が自分か」を調べるので、速く探せるよう索引を付ける。
create index app_users_auth_uid_idx on public.app_users (auth_uid);
