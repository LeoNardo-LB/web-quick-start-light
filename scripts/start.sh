#!/bin/bash
#
# ${rootArtifactId} 生产环境启动脚本
# JVM 最大内存: 1G，适用于 Spring Boot 3.5 + Java 25 脚手架项目
#
# 用法:
#   ./start.sh                    # 默认 prod 环境
#   ./start.sh dev                # 指定 dev 环境
#   ./start.sh prod /custom/path  # 指定环境和 jar 路径
#

set -euo pipefail

# ========== 基础配置 ==========

APP_NAME="${rootArtifactId}"
APP_GROUP="org.smm.archetype"
PROFILE="${1:-prod}"
JAR_PATH="${2:-app/target/app-0.0.1-SNAPSHOT.jar}"
PID_FILE="logs/${APP_NAME}.pid"
LOG_DIR="logs"

# ========== JVM 内存参数（总内存 1G） ==========
#
# 分配方案:
#   堆内存 (Heap)          512MB  (50%)  -Xms512m -Xmx512m
#   元空间 (Metaspace)     128MB  (13%)  -XX:MaxMetaspaceSize=128m
#   代码缓存 (CodeCache)    64MB  ( 6%)  -XX:ReservedCodeCacheSize=64m
#   直接内存 (DirectMem)    64MB  ( 6%)  -XX:MaxDirectMemorySize=64m
#   线程栈 (ThreadStack)   ~100MB (10%)  -Xss512k × ~200线程
#   GC + JVM 内部          ~90MB ( 9%)  - Serial GC 几乎零开销
#   ──────────────────────────────────────
#   合计                   ~958MB ≈ 1G

JVM_MEMORY_OPTS="\
-Xms512m \
-Xmx512m \
-XX:MaxMetaspaceSize=128m \
-XX:ReservedCodeCacheSize=64m \
-XX:MaxDirectMemorySize=64m \
-Xss512k"

# ========== GC 配置 ==========
# Serial GC：小内存（≤1G堆）最优选择
# 优势：零额外内存开销（无 remembered sets / region tables / barriers）
#       吞吐量最高（无并发协调开销）
#       512MB 堆下 Full GC 暂停通常 <100ms
# 参考：Oracle 官方推荐小数据集使用 Serial GC

JVM_GC_OPTS="-XX:+UseSerialGC"

# ========== 诊断配置 ==========

JVM_DIAGNOSTICS_OPTS="\
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=${LOG_DIR}/heapdump.hprof \
-XX:+ExitOnOutOfMemoryError \
-XX:+PrintCommandLineFlags"

# ========== GC 日志（生产推荐） ==========

JVM_GC_LOG_OPTS="\
-Xlog:gc*:file=${LOG_DIR}/gc.log:time,uptime,level,tags:filecount=5,filesize=10M"

# ========== 组合所有 JVM 参数 ==========

JVM_OPTS="${JVM_MEMORY_OPTS} ${JVM_GC_OPTS} ${JVM_DIAGNOSTICS_OPTS} ${JVM_GC_LOG_OPTS}"

# ========== 执行 ==========

echo "=========================================="
echo "  ${APP_NAME} 启动中..."
echo "  Profile : ${PROFILE}"
echo "  JAR     : ${JAR_PATH}"
echo "  PID File: ${PID_FILE}"
echo "  JVM 最大: 1G"
echo "=========================================="

# 检查是否已运行
if [ -f "${PID_FILE}" ]; then
    OLD_PID=$(cat "${PID_FILE}")
    if kill -0 "${OLD_PID}" 2>/dev/null; then
        echo "[ERROR] 应用已在运行 (PID: ${OLD_PID})，请先执行 stop.sh"
        exit 1
    else
        echo "[WARN] 发现残留 PID 文件 (PID: ${OLD_PID})，清理中..."
        rm -f "${PID_FILE}"
    fi
fi

# 检查 jar 文件
if [ ! -f "${JAR_PATH}" ]; then
    echo "[ERROR] JAR 文件不存在: ${JAR_PATH}"
    echo "[HINT] 请先执行: mvn clean package -DskipTests"
    exit 1
fi

# 创建日志目录
mkdir -p "${LOG_DIR}"

# 启动应用
exec java ${JVM_OPTS} \
    -Dspring.profiles.active="${PROFILE}" \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -Djava.security.egd=file:/dev/./urandom \
    -jar "${JAR_PATH}" &
APP_PID=$!

# 写入 PID 文件
echo "${APP_PID}" > "${PID_FILE}"

echo "[OK] 应用已启动 (PID: ${APP_PID})"
echo "     日志目录: ${LOG_DIR}/"
if [ "${PROFILE}" = "prod" ]; then
    echo "     查看日志: tail -f ${LOG_DIR}/app.log"
else
    echo "     查看日志: tail -f ${LOG_DIR}/current.log"
fi
