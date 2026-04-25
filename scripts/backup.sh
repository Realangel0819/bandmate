#!/bin/bash
# ================================================================
# BandMate 데이터베이스 자동 백업 스크립트
# S3에 일별 백업 + 7일 보관
#
# 설정:
#   1. .env 또는 환경변수로 아래 값 설정
#   2. crontab -e 로 스케줄 등록:
#      0 3 * * * /home/ubuntu/bandmate/scripts/backup.sh >> /var/log/bandmate-backup.log 2>&1
# ================================================================

set -euo pipefail

# ── 환경변수 (없으면 .env에서 로드) ──────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"
if [[ -f "$ENV_FILE" ]]; then
  export $(grep -v '^#' "$ENV_FILE" | xargs)
fi

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USERNAME="${DB_USERNAME:?DB_USERNAME is required}"
DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"
S3_BUCKET="${S3_BUCKET:?S3_BUCKET is required}"   # 예: s3://bandmate-backups
RETENTION_DAYS="${RETENTION_DAYS:-7}"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="/tmp/bandmate_${TIMESTAMP}.sql.gz"

echo "[$(date)] 백업 시작: $BACKUP_FILE"

# ── mysqldump ─────────────────────────────────────────────────
# Docker MySQL 사용 시: docker exec로 실행
if docker ps --format '{{.Names}}' 2>/dev/null | grep -q 'bandmate-db\|db'; then
  CONTAINER=$(docker ps --format '{{.Names}}' | grep -E 'bandmate-db|_db_')
  docker exec "$CONTAINER" \
    mysqldump -u"$DB_USERNAME" -p"$DB_PASSWORD" \
    --single-transaction --routines --triggers --add-drop-table \
    bandmatedb | gzip > "$BACKUP_FILE"
else
  # RDS 또는 외부 MySQL
  mysqldump -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USERNAME" -p"$DB_PASSWORD" \
    --single-transaction --routines --triggers --add-drop-table \
    bandmatedb | gzip > "$BACKUP_FILE"
fi

# ── S3 업로드 ─────────────────────────────────────────────────
aws s3 cp "$BACKUP_FILE" "${S3_BUCKET}/$(date +%Y/%m)/$(basename "$BACKUP_FILE")"
rm -f "$BACKUP_FILE"

# ── 오래된 백업 삭제 (S3 Lifecycle Policy로 대체 가능) ──────────
aws s3 ls "${S3_BUCKET}/" --recursive \
  | awk '{print $4}' \
  | while read -r KEY; do
      FILE_DATE=$(echo "$KEY" | grep -oE '[0-9]{8}' | head -1)
      if [[ -n "$FILE_DATE" ]]; then
        CUTOFF=$(date -d "-${RETENTION_DAYS} days" +%Y%m%d 2>/dev/null \
                 || date -v-${RETENTION_DAYS}d +%Y%m%d)
        if [[ "$FILE_DATE" -lt "$CUTOFF" ]]; then
          echo "  삭제: $KEY"
          aws s3 rm "${S3_BUCKET}/${KEY}"
        fi
      fi
    done

echo "[$(date)] 백업 완료 → ${S3_BUCKET}"
