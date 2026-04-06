resource "tls_private_key" "cf_signing" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "aws_cloudfront_public_key" "signing" {
  name        = "${var.project}-cf-signing-key"
  encoded_key = tls_private_key.cf_signing.public_key_pem
}

resource "aws_cloudfront_key_group" "signing" {
  name  = "${var.project}-cf-key-group"
  items = [aws_cloudfront_public_key.signing.id]
}

resource "kubernetes_secret" "cf_signing_key" {
  metadata {
    name      = "cf-signing-key"
    namespace = "chat"
  }

  data = {
    "private-key" = tls_private_key.cf_signing.private_key_pem
  }
}
