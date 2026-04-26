resource "kubernetes_secret" "chat_api_secret" {
  metadata {
    name      = "chat-api-secret"
    namespace = "chat"
  }

  data = {
    "db-password" = var.db_password
  }
}
