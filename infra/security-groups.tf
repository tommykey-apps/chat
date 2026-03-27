locals {
  eks_security_group_ids = distinct(concat(
    [data.aws_eks_cluster.existing.vpc_config[0].cluster_security_group_id],
    data.aws_security_groups.eks_nodes.ids,
  ))
}

resource "aws_security_group" "rds" {
  name        = "${var.project}-rds"
  description = "Security group for chat RDS instance"
  vpc_id      = data.aws_vpc.existing.id

  ingress {
    description     = "PostgreSQL from EKS nodes"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = local.eks_security_group_ids
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project = var.project
  }
}

resource "aws_security_group" "redis" {
  name        = "${var.project}-redis"
  description = "Security group for chat Redis cluster"
  vpc_id      = data.aws_vpc.existing.id

  ingress {
    description     = "Redis from EKS nodes"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = local.eks_security_group_ids
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project = var.project
  }
}
