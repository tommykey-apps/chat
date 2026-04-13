terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
    github = {
      source  = "integrations/github"
      version = "~> 6.0"
    }
  }
}

provider "aws" {
  region = var.region
}

provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}

provider "github" {
  owner = "tommykey-apps"
  token = var.github_token
}

provider "kubernetes" {
  host                   = yamldecode(data.aws_ssm_parameter.k3s_kubeconfig.value).clusters[0].cluster.server
  cluster_ca_certificate = base64decode(yamldecode(data.aws_ssm_parameter.k3s_kubeconfig.value).clusters[0].cluster.certificate-authority-data)
  client_certificate     = base64decode(yamldecode(data.aws_ssm_parameter.k3s_kubeconfig.value).users[0].user.client-certificate-data)
  client_key             = base64decode(yamldecode(data.aws_ssm_parameter.k3s_kubeconfig.value).users[0].user.client-key-data)
}
