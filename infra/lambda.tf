resource "aws_iam_role" "thumbnail_lambda" {
  name = "${var.project}-thumbnail-lambda"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Project = var.project
  }
}

resource "aws_iam_role_policy" "thumbnail_lambda" {
  name = "${var.project}-thumbnail-lambda-policy"
  role = aws_iam_role.thumbnail_lambda.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3Access"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
        ]
        Resource = "${aws_s3_bucket.chat_uploads.arn}/*"
      },
      {
        Sid    = "CloudWatchLogs"
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ]
        Resource = "arn:aws:logs:${var.region}:${data.aws_caller_identity.current.account_id}:*"
      },
    ]
  })
}

data "archive_file" "thumbnail" {
  type             = "zip"
  source_dir       = "${path.module}/../lambda/thumbnail"
  output_path      = "${path.module}/lambda-thumbnail.zip"
  output_file_mode = "0644"
}

resource "aws_lambda_function" "thumbnail" {
  filename         = data.archive_file.thumbnail.output_path
  function_name    = "${var.project}-thumbnail"
  role             = aws_iam_role.thumbnail_lambda.arn
  handler          = "index.handler"
  runtime          = "nodejs22.x"
  architectures    = ["x86_64"]
  timeout          = 30
  memory_size      = 512
  source_code_hash = data.archive_file.thumbnail.output_base64sha256

  tags = {
    Project = var.project
  }
}

resource "aws_lambda_permission" "s3_invoke" {
  statement_id  = "AllowS3Invoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.thumbnail.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.chat_uploads.arn
}

resource "aws_s3_bucket_notification" "uploads" {
  bucket = aws_s3_bucket.chat_uploads.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.thumbnail.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "uploads/"
  }
}
