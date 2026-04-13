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

data "aws_caller_identity" "current" {}

data "aws_instance" "k3s" {
  filter {
    name   = "tag:Name"
    values = ["shared-k3s"]
  }

  filter {
    name   = "instance-state-name"
    values = ["running"]
  }
}

data "aws_ssm_parameter" "k3s_kubeconfig" {
  name            = "/shared/k3s/kubeconfig"
  with_decryption = true
}
