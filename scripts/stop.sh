#!/bin/bash
#
# ${rootArtifactId} 优雅停止脚本
#
# 用法:
#   ./stop.sh          # 默认等待 30 秒
#   ./stop.sh 60       # 自定义等待超时（秒）
#

set -euo pipefail

APP_NAME="${rootArtifactId}"
PID_FILE="logs/${APP_NAME}.pid"
TIMEOUT="${1:-30}"

# 检查 PID 文件
if [ ! -f "${PID_FILE}" ]; then
    echo "[WARN] PID 文件不存在: ${PID_FILE}"
    echo "       应用可能未在运行"
    exit 0
fi

PID=$(cat "${PID_FILE}")

# 检查进程是否存在
if ! kill -0 "${PID}" 2>/dev/null; then
    echo "[WARN] 进程不存在 (PID: ${PID})，清理 PID 文件"
    rm -f "${PID_FILE}"
    exit 0
fi

# 验证进程是否为 Java 应用（防止 PID 回收导致误杀）
if [ -f "/proc/${PID}/cmdline" ]; then
    CMDLINE=$(tr '\0' ' ' < "/proc/${PID}/cmdline" 2>/dev/null || echo "")
    if echo "${CMDLINE}" | grep -qv "java"; then
        echo "[ERROR] PID ${PID} 不是 Java 进程，可能已被回收，清理 PID 文件"
        rm -f "${PID_FILE}"
        exit 1
    fi
fi

echo "[INFO] 正在停止 ${APP_NAME} (PID: ${PID})..."

# 发送 SIGTERM（优雅停止，Spring Boot 会处理 shutdown hooks）
kill -15 "${PID}"

# 等待进程退出
ELAPSED=0
while kill -0 "${PID}" 2>/dev/null; do
    if [ "${ELAPSED}" -ge "${TIMEOUT}" ]; then
        echo "[WARN] 等待超时 (${TIMEOUT}s)，强制终止..."
        kill -9 "${PID}" 2>/dev/null || true
        rm -f "${PID_FILE}"
        echo "[OK] 已强制终止 (PID: ${PID})"
        exit 0
    fi
    sleep 1
    ELAPSED=$((ELAPSED + 1))
done

rm -f "${PID_FILE}"
echo "[OK] 应用已优雅停止 (PID: ${PID})，耗时 ${ELAPSED}s"
