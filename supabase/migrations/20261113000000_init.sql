-- step11: てまりフィットネスのデータを集める表と、誰が見られるかの決まり（RLS）。
--
-- 登場人物:
--   - 端末（利用者のスマホ）: Supabase の「匿名ログイン」で自動でログインする。自分のデータだけ書けて、読める。
--   - スタッフ: メールアドレスとパスワードでログインする。staff 表に登録された人だけが、全員のデータを読める。
--
-- RLS（Row Level Security）: 表の1行ごとに「誰が読めるか・書けるか」を決める Postgres のしくみ。
-- すべての表で有効にし、決まりに書いていない操作はできないようにする。

-- ---------------------------------------------------------------------------
-- 利用者（端末1台に1人）
-- ---------------------------------------------------------------------------
create table public.app_users (
    -- アプリの初回設定で発行した利用者ID（UUID）。
    id uuid primary key,
    -- この利用者のデータを書いてよい、端末のログインID。端末が初めて送るときに自動で入る。
    auth_uid uuid not null unique default auth.uid() references auth.users (id) on delete cascade,
    -- アプリで入れた「よび名」。
    nickname text not null default '' check (char_length(nickname) <= 20),
    -- 1日の目標ポイント。
    daily_goal integer not null default 600 check (daily_goal between 1 and 100000),
    -- 記録するサービスが動いていたか（最後に送ったとき）。
    recording boolean not null default false,
    -- 端末で最後にセンサーの値を記録した日時。
    last_recorded_at timestamptz,
    -- 最後にデータが届いた日時。
    last_synced_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);
comment on table public.app_users is '利用者（端末1台に1人）。よび名・目標・記録の状態。';

-- ---------------------------------------------------------------------------
-- 日ごと・時間帯ごとの集計（アプリの ActivityLog の block 行と同じ中身）
-- ---------------------------------------------------------------------------
create table public.block_records (
    user_id uuid not null references public.app_users (id) on delete cascade,
    date date not null,
    -- 時間帯（アプリの TimeBlock の名前）。
    block text not null check (block in ('Morning', 'Noon', 'Afternoon', 'Night')),
    steps integer not null default 0 check (steps >= 0),
    -- 「家のこと」として数えた秒数。ポイントへの換算は見るときに行う（換算の式を後で変えても使えるように）。
    life_seconds integer not null default 0 check (life_seconds >= 0),
    updated_at timestamptz not null default now(),
    primary key (user_id, date, block)
);
comment on table public.block_records is '日ごと・時間帯ごとの歩数と家のことの秒数。';

-- ---------------------------------------------------------------------------
-- 日ごとのその他（体操の回数）
-- ---------------------------------------------------------------------------
create table public.daily_extras (
    user_id uuid not null references public.app_users (id) on delete cascade,
    date date not null,
    exercises integer not null default 0 check (exercises >= 0),
    updated_at timestamptz not null default now(),
    primary key (user_id, date)
);
comment on table public.daily_extras is '日ごとの体操の回数。';

-- ---------------------------------------------------------------------------
-- 10秒ごとの判別の記録（しきい値の調整に使う。古いものは自動で消す）
-- ---------------------------------------------------------------------------
create table public.epochs (
    user_id uuid not null references public.app_users (id) on delete cascade,
    recorded_at timestamptz not null,
    steps integer not null check (steps >= 0),
    life_seconds integer not null check (life_seconds >= 0),
    -- 揺れの平均（ENMO、mg）。
    enmo_mg real not null check (enmo_mg >= 0),
    walking boolean not null,
    primary key (user_id, recorded_at)
);
comment on table public.epochs is '10秒ごとの判別の結果。30日たったら自動で消す。';

-- ---------------------------------------------------------------------------
-- スタッフ（Web 画面で全員のデータを見られる人）
-- Supabase の管理画面でユーザーを作ってから、この表に1行足して登録する。
-- ---------------------------------------------------------------------------
create table public.staff (
    auth_uid uuid primary key references auth.users (id) on delete cascade,
    name text not null default '',
    created_at timestamptz not null default now()
);
comment on table public.staff is 'Web 画面に入れるスタッフ。管理画面からだけ登録する。';

-- ---------------------------------------------------------------------------
-- 決まりで使う関数
-- security definer: 関数を作った人の権限で staff 表を読む（RLS の決まりの中から staff 表を調べるため）。
-- search_path を空にして、ほかのスキーマの同じ名前の表に入れ替えられないようにする。
-- ---------------------------------------------------------------------------
create function public.is_staff()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (select 1 from public.staff where auth_uid = (select auth.uid()));
$$;

-- その利用者IDが、今ログインしている端末のものか。
create function public.owns_user(target uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.app_users where id = target and auth_uid = (select auth.uid())
    );
$$;

-- 関数はログインした人だけが使えるようにする。
revoke execute on function public.is_staff() from public, anon;
revoke execute on function public.owns_user(uuid) from public, anon;
grant execute on function public.is_staff() to authenticated;
grant execute on function public.owns_user(uuid) to authenticated;

-- ---------------------------------------------------------------------------
-- RLS（誰が読めるか・書けるか）
-- ---------------------------------------------------------------------------
alter table public.app_users enable row level security;
alter table public.block_records enable row level security;
alter table public.daily_extras enable row level security;
alter table public.epochs enable row level security;
alter table public.staff enable row level security;

-- app_users: 端末は自分の行だけ。スタッフは全員分を読める。
create policy "端末は自分を登録できる" on public.app_users
    for insert to authenticated with check (auth_uid = (select auth.uid()));
create policy "端末は自分を更新できる" on public.app_users
    for update to authenticated
    using (auth_uid = (select auth.uid()))
    with check (auth_uid = (select auth.uid()));
create policy "自分かスタッフが読める" on public.app_users
    for select to authenticated using (auth_uid = (select auth.uid()) or (select public.is_staff()));

-- 記録の表: 端末は自分の利用者IDの行だけ書ける。スタッフは全員分を読める。
create policy "端末は自分の記録を足せる" on public.block_records
    for insert to authenticated with check ((select public.owns_user(user_id)));
create policy "端末は自分の記録を更新できる" on public.block_records
    for update to authenticated
    using ((select public.owns_user(user_id)))
    with check ((select public.owns_user(user_id)));
create policy "自分かスタッフが読める" on public.block_records
    for select to authenticated using ((select public.owns_user(user_id)) or (select public.is_staff()));

create policy "端末は自分の記録を足せる" on public.daily_extras
    for insert to authenticated with check ((select public.owns_user(user_id)));
create policy "端末は自分の記録を更新できる" on public.daily_extras
    for update to authenticated
    using ((select public.owns_user(user_id)))
    with check ((select public.owns_user(user_id)));
create policy "自分かスタッフが読める" on public.daily_extras
    for select to authenticated using ((select public.owns_user(user_id)) or (select public.is_staff()));

create policy "端末は自分の記録を足せる" on public.epochs
    for insert to authenticated with check ((select public.owns_user(user_id)));
create policy "端末は自分の記録を更新できる" on public.epochs
    for update to authenticated
    using ((select public.owns_user(user_id)))
    with check ((select public.owns_user(user_id)));
create policy "自分かスタッフが読める" on public.epochs
    for select to authenticated using ((select public.owns_user(user_id)) or (select public.is_staff()));

-- staff: 自分が登録されているかだけ読める（Web 画面で「スタッフかどうか」を確かめるため）。書き込みは管理画面からだけ。
create policy "自分の登録だけ読める" on public.staff
    for select to authenticated using (auth_uid = (select auth.uid()));

-- ---------------------------------------------------------------------------
-- 10秒ごとの記録は 30 日たったら消す（毎日 3:00 JST = 18:00 UTC に実行）
-- ---------------------------------------------------------------------------
create extension if not exists pg_cron with schema pg_catalog;
select cron.schedule(
    'delete-old-epochs',
    '0 18 * * *',
    $$delete from public.epochs where recorded_at < now() - interval '30 days'$$
);
