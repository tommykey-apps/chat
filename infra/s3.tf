resource "aws_s3_bucket" "chat_uploads" {
  bucket        = "${var.project}-uploads-${data.aws_caller_identity.current.account_id}"
  force_destroy = true

  tags = {
    Project = var.project
  }
}

resource "aws_s3_bucket_public_access_block" "chat_uploads" {
  bucket = aws_s3_bucket.chat_uploads.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_lifecycle_configuration" "chat_uploads" {
  bucket = aws_s3_bucket.chat_uploads.id

  rule {
    id     = "expire-after-90-days"
    status = "Enabled"

    filter {}

    expiration {
      days = 90
    }
  }
}

resource "aws_s3_bucket_policy" "chat_uploads" {
  bucket = aws_s3_bucket.chat_uploads.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AllowCloudFrontOAC"
        Effect    = "Allow"
        Principal = {
          Service = "cloudfront.amazonaws.com"
        }
        Action   = "s3:GetObject"
        Resource = "${aws_s3_bucket.chat_uploads.arn}/*"
        Condition = {
          StringEquals = {
            "AWS:SourceArn" = aws_cloudfront_distribution.chat.arn
          }
        }
      }
    ]
  })
}

resource "aws_s3_bucket_cors_configuration" "chat_uploads" {
  bucket = aws_s3_bucket.chat_uploads.id

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["PUT"]
    allowed_origins = ["*"]
    max_age_seconds = 3600
  }
}
