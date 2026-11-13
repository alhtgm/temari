# Supabase の準備と運用

てまりフィットネスのデータを集めるサーバー（Supabase）の準備と、スタッフの登録のしかた。

## 1. プロジェクト
| 項目 | 値 |
|---|---|
| 名前 | temari |
| ID | `iymxpeukzjgjnwxnjxwv` |
| 地域 | 東京（ap-northeast-1） |
| URL | `https://iymxpeukzjgjnwxnjxwv.supabase.co` |
| 管理画面 | https://supabase.com/dashboard/project/iymxpeukzjgjnwxnjxwv |

表と決まり（RLS）は `supabase/migrations/` の SQL で作ってある。表を変えるときは、新しい SQL ファイルを足して適用する（前のファイルは書き換えない）。

## 2. 表
| 表 | 中身 | 書く人 | 読める人 |
|---|---|---|---|
| `app_users` | 利用者ID・よび名・目標・記録中か・最後に記録した時刻 | 端末（自分の行だけ） | 自分・スタッフ |
| `block_records` | 日ごと・時間帯ごとの歩数と家のことの秒数 | 端末（自分の行だけ） | 自分・スタッフ |
| `daily_extras` | 日ごとの体操の回数 | 端末（自分の行だけ） | 自分・スタッフ |
| `epochs` | 10秒ごとの判別の結果（歩数・揺れ・判別） | 端末（自分の行だけ） | 自分・スタッフ |
| `staff` | Web 画面に入れるスタッフ | 管理画面からだけ | 自分の登録だけ |

- 端末は Supabase の「匿名ログイン」で自動でログインする。利用者がパスワードを入れる必要はない。
- `epochs` は 30 日たったものを毎日 3:00 に自動で消す（`pg_cron` の `delete-old-epochs`）。

## 3. 最初に1回だけ行う設定（管理画面で）
### 3.1 匿名ログインを有効にする（端末がデータを送るのに必要）
1. 管理画面 → **Authentication** → **Sign In / Providers**
2. **Allow anonymous sign-ins** をオンにして保存する。

### 3.2 だれでもアカウントを作れないようにする
1. 同じ画面の **Allow new users to sign up** をオフにする。
   - オフにしても、匿名ログインと、管理画面から作るスタッフのアカウントは使える。
   - Web 画面に「新しく登録」は作らない。

## 4. スタッフを登録する
1. 管理画面 → **Authentication** → **Users** → **Add user** → **Create new user**
2. メールアドレスとパスワードを入れ、**Auto Confirm User** にチェックを入れて作る。
3. 作ったユーザーの **UID** をコピーする。
4. 管理画面 → **SQL Editor** で次を実行する（UID と名前を書き換える）。
   ```sql
   insert into public.staff (auth_uid, name) values ('ここにUID', 'スタッフの名前');
   ```
5. スタッフをやめるときは `delete from public.staff where auth_uid = 'UID';` と、Users からのユーザー削除。

## 5. 鍵の扱い
- アプリと Web 画面には **公開用の鍵（publishable key, `sb_publishable_...`）** だけを入れる。この鍵は端末に入っても問題ないもので、読み書きできる範囲は上の決まり（RLS）で制限している。
- **secret key（`sb_secret_...`）と service_role の鍵は、アプリ・Web 画面・リポジトリのどこにも入れない。** 決まりを無視して全部読み書きできる鍵のため。
- リポジトリは公開なので、URL と鍵は `local.properties`（アプリ）と Vercel の環境変数（Web 画面）に書き、コミットしない。

## 6. 個人の情報について
- サーバーには **よび名** と **10秒ごとの動きの記録** が入る。生活のようすが細かく分かるデータなので、実際の利用者に使ってもらう前に、施設と利用者（ご家族）の同意を取ること。
- 端末を別の利用者に渡すときは、スタッフ用画面の「初回設定のやり直し」で新しい利用者IDにする。前の利用者のデータを消すときは、管理画面の SQL Editor で `delete from public.app_users where id = '利用者ID';`（関係する記録もいっしょに消える）。
