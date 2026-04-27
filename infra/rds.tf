resource "aws_db_subnet_group" "chat" {
  name       = "${var.project}-db"
  subnet_ids = data.aws_subnets.private.ids

  tags = {
    Project = var.project
  }
}

resource "aws_db_instance" "chat" {
  identifier     = "${var.project}-db"
  engine         = "postgres"
  engine_version = "16"
  instance_class = "db.t3.micro"

  allocated_storage = 20
  db_name           = "chat"
  username          = var.db_username
  password          = var.db_password

  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.chat.name

  skip_final_snapshot = true
  multi_az            = false
  apply_immediately   = true

  tags = {
    Project = var.project
  }
}
