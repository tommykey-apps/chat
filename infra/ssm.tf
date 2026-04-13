resource "aws_ssm_parameter" "cloudfront_distribution_id" {
  name  = "/chat/cloudfront-distribution-id"
  type  = "String"
  value = aws_cloudfront_distribution.chat.id

  tags = {
    Project = var.project
  }
}

resource "aws_ssm_parameter" "frontend_bucket" {
  name  = "/chat/frontend-bucket"
  type  = "String"
  value = aws_s3_bucket.frontend.bucket

  tags = {
    Project = var.project
  }
}
