variable "region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-1"
}

variable "project" {
  description = "Project name"
  type        = string
  default     = "chat"
}

variable "vpc_name" {
  description = "Name of the existing VPC"
  type        = string
  default     = "shared-vpc"
}

variable "k3s_iam_role_name" {
  description = "IAM role name of the K3s instance profile"
  type        = string
  default     = "shared-k3s"
}

variable "github_token" {
  description = "GitHub token for deploy key management"
  type        = string
  sensitive   = true
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "chat"
}

variable "db_password" {
  description = "Database master password"
  type        = string
  sensitive   = true
}
