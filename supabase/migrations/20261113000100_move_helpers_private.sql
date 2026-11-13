-- step11: 決まり（RLS）で使う関数を、API から直接呼べない private スキーマに移す。
--
-- public スキーマの関数は /rest/v1/rpc/... から誰でも（ログインした人なら）呼べてしまう。
-- private スキーマは API に出していないので、決まりの中からだけ使われる。
-- 決まりは関数を名前ではなく中の番号で覚えているので、移しても作り直さなくてよい。

create schema if not exists private;

alter function public.is_staff() set schema private;
alter function public.owns_user(uuid) set schema private;

-- 決まりの中で関数を使うには、スキーマを使う権利が要る。
grant usage on schema private to authenticated;
