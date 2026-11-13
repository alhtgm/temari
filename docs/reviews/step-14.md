# step14: 公開（Vercel）

## 1. やったこと
- Vercel にプロジェクト `temari-staff` を作り、GitHub の `alhtgm/temari` の `web/` フォルダとつないだ。`main` に送る（プッシュする）たびに、自動で作り直して公開される。
- Vercel の環境変数に Supabase の URL と公開用の鍵（`NEXT_PUBLIC_SUPABASE_URL`・`NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY`）を入れた。
- GitHub がメールアドレスの公開を止める設定になっていたので、このリポジトリのコミットの作者を GitHub の代わりのアドレス（`...@users.noreply.github.com`）にした。

## 2. 追加・変更したもの
| もの | 役割 |
|---|---|
| Vercel プロジェクト `temari-staff`（ID `prj_kYsUW6KfHQZcwG03aQKdad1YA1JZ`） | スタッフ用 Web 画面の公開先。Root Directory は `web` |
| Vercel の環境変数 | Supabase の URL と公開用の鍵（production・preview・development） |
| git の設定（このリポジトリだけ） | `user.email` を GitHub の代わりのアドレスにした |
| `docs/reviews/step-14.md` | この記録 |

## 3. 確認する手順と、期待される見た目・動き
1. Vercel の管理画面 → `temari-staff` → Deployments で、最新の公開が Ready になっている。
2. 公開した URL（Domains に出ている `https://temari-staff....vercel.app`）を開くと、ログイン画面が出る。
3. スタッフのメールアドレスとパスワードでログイン → 一覧画面。スタッフに登録していないアカウントでは「スタッフとして登録されていません」。
4. アプリの「今すぐ送る」で送ったデータが、一覧と利用者ごとの画面に出る。

## 4. 判断を保留した点
- **URL を知っていれば誰でもログイン画面までは開ける**: データはスタッフだけが読める（Supabase の RLS）。施設の外から開けないようにするなら、Vercel の「Deployment Protection」や IP の制限を考える（有料の機能もある）。
- **GitHub に載ったメールアドレス**: step01〜10 のコミットには本当のメールアドレスが残っている。消すには公開済みの履歴の書き換えが必要なので、行っていない。
- **プレビューの公開**: main 以外のブランチに送ると、プレビューの URL も作られる（今は main だけを使っている）。
