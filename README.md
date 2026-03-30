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
| IaC | Terraform (S3バックエンド + DynamoDB state lock) |
| CI/CD | GitHub Actions + flox (Terraform apply + Docker build 自動化) |
| 配信 | CloudFront (S3 + ALB を同一ドメインで配信) |

## 使ってるAWSサービス

| サービス | 何してるか |
|---------|-----------|
| EKS | Spring Boot + Elasticsearch の Pod を動かすクラスタ |
| EC2 | EKSのワーカーノード (t3.medium) |
| ECR | Docker イメージ置き場 |
| RDS (PostgreSQL) | チャットルーム、メッセージ、メンバー、ユーザー、フレンドシップの保存 |
| ElastiCache (Redis) | オンライン状態の管理、WebSocket セッション |
| Cognito | ユーザー登録、ログイン、JWT 発行 |
| SQS | メッセージの非同期処理キュー |
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
| WebSocket | `/ws` | STOMP でリアルタイムメッセージ |

## ローカルで動かす

```bash
flox activate                     # Java, Gradle, pnpm 等が使える
# PostgreSQL + Redis + Elasticsearch が自動起動 (docker-compose)

cd api && gradle bootRun --args='--spring.profiles.active=local' &
cd web && pnpm install && pnpm dev
```

フロントは DEV モードで Cognito 認証をスキップするので、ログインなしでUI確認できる。
Vite のプロキシで `/api/*` と `/ws` が Spring Boot に流れる。

## デプロイ

```bash
cd ~/dev/chat/infra && terraform apply
```

使い終わったら壊す。

```bash
cd ~/dev/chat/infra && terraform destroy
```

## WebSocket について

CloudFront 経由で WebSocket (STOMP) を通している。
`/ws` パスを ALB オリジンに振り分け、`Upgrade` / `Connection` ヘッダーを転送する設定。
