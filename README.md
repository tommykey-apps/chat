# Chat

リアルタイムチャットアプリ。フレンド機能、Elasticsearch全文検索、画像送信、WebSocket (STOMP) によるリアルタイムメッセージングを実装。

## 構成図

![Architecture](docs/architecture.svg)

> [docs/architecture.drawio](docs/architecture.drawio) を draw.io で開くと編集できる

## 使った技術

| | |
|---|---|
| バックエンド | Java 21 + Spring Boot 3.4 |
| フロント | SvelteKit 2, Svelte 5, Tailwind CSS v4, shadcn-svelte |
| 認証 | Cognito (JWT, SRP認証) |
| DB | PostgreSQL (RDS) |
| キャッシュ | Redis (ElastiCache) |
| キュー | SQS |
| 検索 | Elasticsearch (自前ホスト、EKS Pod) |
| リアルタイム | WebSocket (STOMP) — CloudFront経由 |
| 通知 | Web Push (VAPID) + アプリ内トースト + 未読バッジ |
| テスト | JUnit 5 + Mockito + Testcontainers / Vitest / Playwright |
| IaC | Terraform (S3バックエンド + DynamoDB state lock) |
| CI/CD | GitHub Actions + ArgoCD Image Updater |
| 配信 | CloudFront (S3 + ALB を同一ドメインで配信) |

## 使ってるAWSサービス

| サービス | 何してるか |
|---------|-----------|
| EKS | Spring Boot + Elasticsearch の Pod を動かすクラスタ |
| EC2 | EKSのワーカーノード (t3.medium) |
| ECR | Docker イメージ置き場 |
| RDS (PostgreSQL) | チャットルーム、メッセージ、メンバー、ユーザー、フレンドシップの保存 |
| ElastiCache (Redis) | オンライン状態の管理、未読カウント |
| Cognito | ユーザー登録、ログイン、JWT 発行 |
| SQS | メッセージ送信時の通知処理キュー (DLQ付き) |
| S3 | フロントの配信 + チャットで送るファイルの保存 + Terraform state |
| CloudFront | フロント + REST API + WebSocket の HTTPS 配信 |
| ALB | リクエスト振り分け + WebSocket 接続 |
| Route 53 | カスタムドメイン (chat.tommykeyapp.com) のDNS管理 |
| ACM | SSL証明書 (*.tommykeyapp.com ワイルドカード) |
| IAM | Pod に S3/SQS のアクセス権を付与 (IRSA) |

## 機能

- ユーザー認証（サインアップ、ログイン、JWT）
- チャットルームの作成・参加・退出
- WebSocket (STOMP) によるリアルタイムメッセージ送受信
- 画像・ファイルのアップロード（S3 presigned URL）
- Elasticsearch による全文検索
- フレンド機能（ユーザー検索、申請、承認、DM開始）
- アプリ内通知（トースト + 未読バッジ、STOMP `/user/queue/notifications`）
- ブラウザ Push 通知（Web Push API + VAPID + Service Worker）
- SQS による非同期通知パイプライン（ローカル開発は Spring Event fallback）

## ディレクトリ構成

```
chat/
├── api/          # Spring Boot (Java 21)
├── web/          # SvelteKit フロント（shadcn-svelte）
├── infra/        # Terraform（EKS, VPC, RDS, Redis, S3, CloudFront 等）
├── manifests/    # K8s マニフェスト + ArgoCD
├── docs/         # 構成図 (draw.io)
└── .github/      # GitHub Actions（Docker build + Terraform apply）
```

## API

| Method | Path | 何するか |
|--------|------|---------|
| POST | `/api/rooms` | ルーム作成 |
| GET | `/api/rooms` | ルーム一覧 |
| GET | `/api/rooms/{id}` | ルーム詳細 |
| POST | `/api/rooms/{id}/join` | 参加 |
| DELETE | `/api/rooms/{id}/leave` | 退出 |
| GET | `/api/rooms/{id}/messages` | メッセージ履歴 |
| GET | `/api/rooms/{id}/messages/search` | メッセージ検索 (Elasticsearch) |
| POST | `/api/files/presign-upload` | ファイルアップロード URL |
| GET | `/api/files/presign-download/**` | ファイルダウンロード URL |
| GET | `/api/users/me` | 自分の情報 |
| GET | `/api/users/search` | ユーザー検索 |
| GET | `/api/friends` | フレンド一覧 |
| GET | `/api/friends/requests` | 受信した申請一覧 |
| POST | `/api/friends/{id}/request` | フレンド申請 |
| POST | `/api/friends/{id}/accept` | 申請承認 |
| DELETE | `/api/friends/{id}` | フレンド削除 |
| GET | `/api/notifications/unread` | 未読カウント取得 |
| DELETE | `/api/notifications/unread/{roomId}` | 未読クリア |
| GET | `/api/push/vapid-key` | VAPID 公開鍵取得 |
| POST | `/api/push/subscribe` | Push 通知購読登録 |
| DELETE | `/api/push/unsubscribe` | Push 通知購読解除 |
| WebSocket | `/ws` | STOMP でリアルタイムメッセージ |

## ローカルで動かす

```bash
flox activate                     # Java, Gradle, pnpm 等が使える
# PostgreSQL + Redis + Elasticsearch が自動起動 (docker-compose)

cd api && gradle bootRun --args='--spring.profiles.active=local' &
cd web && pnpm install && pnpm dev
```

フロントは DEV モードで Cognito 認証をスキップし、`dev-user` として自動ログインする。
API は `local` プロファイルで `LocalSecurityConfig` が有効になり、`dev-token` を受け付ける。
Vite のプロキシで `/api/*` と `/ws` が Spring Boot に流れる。

## デプロイ

main に push すると自動デプロイ。

```
main push → GitHub Actions
  ├── deploy-api: Docker build → ECR push (:prod タグ上書き)
  ├── deploy-web: pnpm build → S3 sync → CloudFront invalidate
  └── deploy-infra: terraform apply (infra/ 変更時のみ)

ECR push 後:
  ArgoCD Image Updater が prod タグの digest 変更を検知 → ArgoCD sync → Pod 更新
```

マニフェストへの git push は不要。Image Updater がクラスタ内から ECR を監視して自動反映。

```bash
# インフラを手動で立てる/壊す場合
cd ~/dev/chat/infra && terraform apply
cd ~/dev/chat/infra && terraform destroy
```

## テスト

```bash
cd api && gradle test              # Backend ユニット + 結合テスト (Testcontainers)
cd web && pnpm test                # Frontend ユニットテスト (Vitest)
cd web && pnpm test:e2e            # E2E テスト (Playwright)
```

| カテゴリ | テスト数 | ツール |
|---------|---------|-------|
| Backend ユニット | 62 | JUnit 5 + Mockito |
| Backend 結合 | 24 | Testcontainers (PostgreSQL + Redis) |
| Frontend ユニット | 33 | Vitest |
| Frontend E2E | 7 | Playwright |

## WebSocket について

CloudFront 経由で WebSocket (STOMP) を通している。
`/ws` パスを ALB オリジンに振り分け、`Upgrade` / `Connection` ヘッダーを転送する設定。
