output "cognito_user_pool_id" {
  description = "Cognito User Pool ID"
  value       = aws_cognito_user_pool.chat.id
}

output "cognito_client_id" {
  description = "Cognito User Pool Client ID"
  value       = aws_cognito_user_pool_client.chat_web.id
}

output "cognito_issuer_uri" {
  description = "Cognito Issuer URI"
  value       = "https://cognito-idp.${var.region}.amazonaws.com/${aws_cognito_user_pool.chat.id}"
}

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.chat.endpoint
}

output "redis_endpoint" {
  description = "ElastiCache Redis endpoint"
  value       = aws_elasticache_cluster.chat.cache_nodes[0].address
}

output "sqs_queue_url" {
  description = "SQS queue URL for chat messages"
  value       = aws_sqs_queue.chat_messages.url
}

output "s3_uploads_bucket" {
  description = "S3 bucket name for uploads"
  value       = aws_s3_bucket.chat_uploads.bucket
}

output "ecr_repository_url" {
  description = "ECR repository URL for chat API"
  value       = aws_ecr_repository.chat_api.repository_url
}

output "cloudfront_distribution_id" {
  description = "CloudFront distribution ID"
  value       = aws_cloudfront_distribution.chat.id
}

output "cloudfront_domain_name" {
  description = "CloudFront distribution domain name"
  value       = aws_cloudfront_distribution.chat.domain_name
}

output "frontend_bucket_name" {
  description = "S3 bucket name for frontend static files"
  value       = aws_s3_bucket.frontend.bucket
}

output "k3s_iam_role_name" {
  description = "K3s IAM role name with chat API access"
  value       = var.k3s_iam_role_name
}

output "region" {
  description = "AWS region"
  value       = var.region
}

output "cf_key_pair_id" {
  description = "CloudFront signing key pair ID"
  value       = aws_cloudfront_public_key.signing.id
}

output "ses_domain_identity" {
  description = "SES verified domain"
  value       = aws_ses_domain_identity.chat.domain
}
