data "aws_route53_zone" "main" {
  name = "tommykeyapp.com"
}

# Reference the wildcard certificate created by url-shortener
data "aws_acm_certificate" "wildcard" {
  provider = aws.us_east_1
  domain   = "*.tommykeyapp.com"
  statuses = ["ISSUED"]
}

# api-origin.tommykeyapp.com -> K3s EIP (CloudFront origin)
resource "aws_route53_record" "api_origin" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "api-origin.tommykeyapp.com"
  type    = "A"
  ttl     = 300
  records = [data.aws_instance.k3s.public_ip]
}

# chat.tommykeyapp.com -> CloudFront
resource "aws_route53_record" "chat" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "chat.tommykeyapp.com"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.chat.domain_name
    zone_id                = aws_cloudfront_distribution.chat.hosted_zone_id
    evaluate_target_health = false
  }
}
