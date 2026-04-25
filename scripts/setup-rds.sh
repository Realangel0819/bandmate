#!/bin/bash
# ================================================================
# RDS + 자동 백업 EC2 초기 설정 스크립트
# EC2에서 한 번만 실행: bash scripts/setup-rds.sh
# ================================================================

set -euo pipefail

echo "=== AWS CLI 설치 확인 ==="
if ! command -v aws &>/dev/null; then
  curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
  unzip -q /tmp/awscliv2.zip -d /tmp
  sudo /tmp/aws/install
  rm -rf /tmp/aws /tmp/awscliv2.zip
fi
aws --version

echo ""
echo "=== MySQL 클라이언트 설치 (RDS 연결 테스트용) ==="
if ! command -v mysql &>/dev/null; then
  sudo apt-get update -qq
  sudo apt-get install -y mysql-client
fi

echo ""
echo "=== 백업 스크립트 실행 권한 부여 ==="
chmod +x "$(dirname "$0")/backup.sh"

echo ""
echo "=== cron 등록 (매일 새벽 3시 자동 백업) ==="
CRON_JOB="0 3 * * * $(realpath "$(dirname "$0")/backup.sh") >> /var/log/bandmate-backup.log 2>&1"
(crontab -l 2>/dev/null | grep -v 'backup.sh'; echo "$CRON_JOB") | crontab -
echo "등록된 cron:"
crontab -l | grep backup

echo ""
echo "=== .env에 S3_BUCKET 설정 확인 ==="
ENV_FILE="$(dirname "$0")/../.env"
if ! grep -q 'S3_BUCKET' "$ENV_FILE" 2>/dev/null; then
  echo "⚠️  .env에 S3_BUCKET 항목이 없습니다. 추가하세요:"
  echo "   S3_BUCKET=s3://your-bucket-name"
fi

echo ""
echo "✅ 설정 완료. 백업 테스트: bash scripts/backup.sh"
