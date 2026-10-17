#!/usr/bin/env bash
# GitHub'ga yuklash + imzolash kalitini Secrets'ga joylash + birinchi versiyani chiqarish.
# Talab: gh (GitHub CLI) o'rnatilgan va `gh auth login` qilingan bo'lishi kerak.
# Ishlatish:  scripts/publish-to-github.sh <github-login> [repo-nomi] [signing-papka]
set -euo pipefail

OWNER="${1:?GitHub login kerak: scripts/publish-to-github.sh <login> [repo] [signing-papka]}"
REPO="${2:-Nomoz-Vaqtlarim}"
SIGNING="${3:-$(cd "$(dirname "$0")/.." && pwd)/signing}"
FULL="$OWNER/$REPO"

[ -f "$SIGNING/secrets.env" ] || { echo "Imzolash papkasi topilmadi: $SIGNING"; exit 1; }
# shellcheck disable=SC1091
source "$SIGNING/secrets.env"

gh auth status >/dev/null || { echo "Avval: gh auth login"; exit 1; }

if ! gh repo view "$FULL" >/dev/null 2>&1; then
  gh repo create "$FULL" --public --source=. --remote=origin --description "Namoz Vaqtlarim — namoz vaqtlari, eslatmalar, budilnik (Android)"
else
  git remote get-url origin >/dev/null 2>&1 || git remote add origin "https://github.com/$FULL.git"
fi

echo "Secrets joylanmoqda…"
gh secret set KEYSTORE_BASE64   --repo "$FULL" < "$SIGNING/keystore.base64"
gh secret set KEYSTORE_PASSWORD --repo "$FULL" --body "$KEYSTORE_PASSWORD"
gh secret set KEY_ALIAS         --repo "$FULL" --body "$KEY_ALIAS"
gh secret set KEY_PASSWORD      --repo "$FULL" --body "$KEY_PASSWORD"

git push -u origin main
if ! git rev-parse v1.0.0 >/dev/null 2>&1; then git tag v1.0.0; fi
git push origin v1.0.0

echo
echo "Tayyor! Jarayonni kuzating:  https://github.com/$FULL/actions"
echo "APK tayyor bo'lgach:         https://github.com/$FULL/releases/latest"
