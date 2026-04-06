resource "aws_ses_domain_identity" "chat" {
  domain = "tommykeyapp.com"
}

resource "aws_ses_domain_dkim" "chat" {
  domain = aws_ses_domain_identity.chat.domain
}

resource "aws_ses_domain_mail_from" "chat" {
  domain           = aws_ses_domain_identity.chat.domain
  mail_from_domain = "mail.tommykeyapp.com"

  behavior_on_mx_failure = "UseDefaultValue"
}

# Domain verification TXT record
resource "aws_route53_record" "ses_verification" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "_amazonses.tommykeyapp.com"
  type    = "TXT"
  ttl     = 600
  records = [aws_ses_domain_identity.chat.verification_token]
}

# DKIM CNAME records (3 records)
resource "aws_route53_record" "ses_dkim" {
  count   = 3
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "${aws_ses_domain_dkim.chat.dkim_tokens[count.index]}._domainkey.tommykeyapp.com"
  type    = "CNAME"
  ttl     = 600
  records = ["${aws_ses_domain_dkim.chat.dkim_tokens[count.index]}.dkim.amazonses.com"]
}

# MAIL FROM: MX record
resource "aws_route53_record" "ses_mail_from_mx" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "mail.tommykeyapp.com"
  type    = "MX"
  ttl     = 600
  records = ["10 feedback-smtp.${var.region}.amazonses.com"]
}

# MAIL FROM: SPF TXT record
resource "aws_route53_record" "ses_mail_from_spf" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "mail.tommykeyapp.com"
  type    = "TXT"
  ttl     = 600
  records = ["v=spf1 include:amazonses.com ~all"]
}

# DMARC TXT record
resource "aws_route53_record" "ses_dmarc" {
  zone_id = data.aws_route53_zone.main.zone_id
  name    = "_dmarc.tommykeyapp.com"
  type    = "TXT"
  ttl     = 600
  records = ["v=DMARC1; p=quarantine; adkim=r; aspf=r"]
}
