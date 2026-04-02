locals {
  oidc_provider_arn = replace(
    data.aws_eks_cluster.existing.identity[0].oidc[0].issuer,
    "https://",
    ""
  )
}

module "irsa_chat_api" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.0"

  role_name = "${var.project}-api"

  oidc_providers = {
    main = {
      provider_arn               = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/${local.oidc_provider_arn}"
      namespace_service_accounts = ["chat:chat-api"]
    }
  }

  role_policy_arns = {
    chat_api_access = aws_iam_policy.chat_api_access.arn
  }

  tags = {
    Project = var.project
  }
}

module "irsa_image_updater" {
  source  = "terraform-aws-modules/iam/aws//modules/iam-role-for-service-accounts-eks"
  version = "~> 5.0"

  role_name = "argocd-image-updater"

  oidc_providers = {
    main = {
      provider_arn               = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/${local.oidc_provider_arn}"
      namespace_service_accounts = ["argocd:argocd-image-updater"]
    }
  }

  role_policy_arns = {
    ecr_read = aws_iam_policy.image_updater_ecr.arn
  }

  tags = {
    Project = var.project
  }
}

resource "aws_iam_policy" "image_updater_ecr" {
  name        = "argocd-image-updater-ecr"
  description = "Allow Image Updater to read ECR"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ecr:GetAuthorizationToken",
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "ecr:BatchGetImage",
          "ecr:GetDownloadUrlForLayer",
          "ecr:DescribeImages",
          "ecr:ListImages",
        ]
        Resource = aws_ecr_repository.chat_api.arn
      },
    ]
  })
}

resource "aws_iam_policy" "chat_api_access" {
  name        = "${var.project}-api-access"
  description = "Policy for chat API to access S3 and SQS"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3Access"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
        ]
        Resource = "${aws_s3_bucket.chat_uploads.arn}/*"
      },
      {
        Sid    = "SQSAccess"
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl",
          "sqs:CreateQueue",
        ]
        Resource = aws_sqs_queue.chat_messages.arn
      },
    ]
  })

  tags = {
    Project = var.project
  }
}
