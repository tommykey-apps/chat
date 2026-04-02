data "aws_vpc" "existing" {
  filter {
    name   = "tag:Name"
    values = [var.vpc_name]
  }
}

data "aws_subnets" "private" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.existing.id]
  }

  tags = {
    "kubernetes.io/role/internal-elb" = "1"
  }
}

data "aws_subnets" "public" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.existing.id]
  }

  tags = {
    "kubernetes.io/role/elb" = "1"
  }
}

data "aws_eks_cluster" "existing" {
  name = var.eks_cluster_name
}

data "aws_eks_cluster_auth" "existing" {
  name = var.eks_cluster_name
}

data "aws_caller_identity" "current" {}

data "aws_lb" "chat" {
  tags = {
    "ingress.k8s.aws/stack" = "chat/chat-api"
  }
}

data "aws_security_groups" "eks_nodes" {
  filter {
    name   = "tag:kubernetes.io/cluster/${var.eks_cluster_name}"
    values = ["owned"]
  }

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.existing.id]
  }
}
