resource "aws_iam_policy" "chat_api_access" {
  name        = "${var.project}-api-access"
  description = "Policy for chat API to access S3, SQS, and SES"

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
      {
        Sid    = "SESAccess"
        Effect = "Allow"
        Action = [
          "ses:SendEmail",
          "ses:SendRawEmail",
        ]
        Resource = "arn:aws:ses:${var.region}:${data.aws_caller_identity.current.account_id}:identity/tommykeyapp.com"
      },
    ]
  })

  tags = {
    Project = var.project
  }
}

resource "aws_iam_role_policy_attachment" "k3s_chat_api" {
  role       = var.k3s_iam_role_name
  policy_arn = aws_iam_policy.chat_api_access.arn
}
