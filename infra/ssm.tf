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

resource "aws_ssm_parameter" "cognito_user_pool_id" {
  name  = "/chat/cognito-user-pool-id"
  type  = "String"
  value = aws_cognito_user_pool.chat.id

  tags = {
    Project = var.project
  }
}

resource "aws_ssm_parameter" "cognito_client_id" {
  name  = "/chat/cognito-client-id"
  type  = "String"
  value = aws_cognito_user_pool_client.chat_web.id

  tags = {
    Project = var.project
  }
}
