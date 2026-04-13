# --- Deploy key for ArgoCD repo access ---

resource "tls_private_key" "argocd_deploy_key" {
  algorithm = "ED25519"
}

resource "github_repository_deploy_key" "argocd" {
  title      = "argocd"
  repository = var.project
  key        = tls_private_key.argocd_deploy_key.public_key_openssh
  read_only  = true
}

# --- ArgoCD repository secret ---

resource "kubernetes_secret" "argocd_repo" {
  metadata {
    name      = "repo-${var.project}"
    namespace = "argocd"
    labels = {
      "argocd.argoproj.io/secret-type" = "repository"
    }
  }

  data = {
    type          = "git"
    url           = "git@github.com:tommykey-apps/${var.project}.git"
    sshPrivateKey = tls_private_key.argocd_deploy_key.private_key_openssh
  }
}

# --- ArgoCD Application ---

resource "kubernetes_manifest" "argocd_app" {
  manifest = {
    apiVersion = "argoproj.io/v1alpha1"
    kind       = "Application"
    metadata = {
      name      = var.project
      namespace = "argocd"
    }
    spec = {
      project = "default"
      source = {
        repoURL        = "git@github.com:tommykey-apps/${var.project}.git"
        targetRevision = "main"
        path           = "manifests/base"
      }
      destination = {
        server    = "https://kubernetes.default.svc"
        namespace = var.project
      }
      syncPolicy = {
        automated = {
          prune    = true
          selfHeal = true
        }
        syncOptions = ["CreateNamespace=true"]
      }
    }
  }
}
