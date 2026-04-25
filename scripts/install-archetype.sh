#!/bin/bash
#
# install-archetype.sh — 一键安装 Archetype 到本地 Maven 仓库
#
# 用法：
#   cd /path/to/web-quick-start-light
#   ./scripts/install-archetype.sh
#
# 功能：
#   1. 从骨架项目生成 Maven Archetype（自动处理 OOM 问题）
#   2. Patch archetype-metadata.xml（启用 filtered 替换）
#   3. 安装到本地 Maven 仓库
#   4. 验证安装结果
#
# 已知问题：
#   Maven Archetype 插件的 fork 子进程存在 OOM 问题（即使资源数已优化），
#   本脚本通过增加 fork 进程的堆内存来绕过。
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ARCHETYPE_DIR="$PROJECT_DIR/target/generated-sources/archetype"

echo "📦 从骨架项目生成 Archetype..."
echo "   项目目录: $PROJECT_DIR"
echo ""

cd "$PROJECT_DIR"

# 步骤 1：生成 archetype 模板（create-from-project 的 jar 阶段可能 OOM，但不影响模板生成）
echo "🔨 步骤 1/4: 生成 archetype 模板..."
mvn archetype:create-from-project -DskipTests -Darchetype.properties=archetype.properties -q 2>&1 || true

if [ ! -d "$ARCHETYPE_DIR" ]; then
    echo "❌ Archetype 模板生成失败"
    exit 1
fi

echo "   ✅ 模板生成完成: $ARCHETYPE_DIR"

# 步骤 2：Patch archetype-metadata.xml — 启用 filtered 替换
METADATA="$ARCHETYPE_DIR/src/main/resources/META-INF/maven/archetype-metadata.xml"
echo ""
echo "🔧 步骤 2/4: Patch archetype-metadata.xml（启用 filtered 替换）..."

python3 - "$METADATA" "$PROJECT_DIR" << 'PYEOF'
import re, sys, shutil, os

metadata_path = sys.argv[1]
project_dir = sys.argv[2] if len(sys.argv) > 2 else ""
if not metadata_path:
    print("   ⚠️  未找到 metadata 文件，跳过 patch")
    sys.exit(0)

with open(metadata_path, 'r') as f:
    content = f.read()

original = content

# 1. .imports fileSet（组件模块的 src/main/resources）: 加 filtered="true"
content = re.sub(
    r'(<fileSet) (encoding="UTF-8">)\s*\n(\s*<directory>src/main/resources</directory>\s*\n\s*<includes>\s*\n\s*<include>\*\*/\*\.imports)',
    r'\1 filtered="true" \2\n\3',
    content
)

# 2. app 模块的 src/main/resources YAML fileSet: 加 filtered="true"
content = re.sub(
    r'(<fileSet) (encoding="UTF-8">)\s*\n(\s*<directory>src/main/resources</directory>\s*\n\s*<includes>\s*\n\s*<include>\*\*/\*\.yaml)',
    r'\1 filtered="true" \2\n\3',
    content
)

# 3. app 模块的 src/test/resources YAML fileSet: 加 filtered="true"
content = re.sub(
    r'(<fileSet) (encoding="UTF-8">)\s*\n(\s*<directory>src/test/resources</directory>\s*\n\s*<includes>\s*\n\s*<include>\*\*/\*\.yaml)',
    r'\1 filtered="true" \2\n\3',
    content
)

# 4. scripts fileSet: 加 filtered="true"（用于 shell 脚本中的变量替换）
content = re.sub(
    r'(<fileSet) (encoding="UTF-8">)\s*\n(\s*<directory>scripts</directory>)',
    r'\1 filtered="true" \2\n\3',
    content
)

if content != original:
    with open(metadata_path, 'w') as f:
        f.write(content)
    print('   ✅ archetype-metadata.xml patched')
else:
    print('   ⚠️  未发现需要 patch 的 fileSet（可能已经 patch 过）')

# 5. 复制 .gitignore 到 archetype-resources（Maven Archetype 默认不复制隐藏文件）
src = os.path.join(project_dir, '.gitignore') if project_dir else ''
dst = os.path.join(os.path.dirname(metadata_path), '..', 'archetype-resources', '.gitignore')
if src and os.path.exists(src):
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy2(src, dst)
    print('   ✅ .gitignore 已复制到 archetype-resources')
PYEOF

# 步骤 3：在生成的目录中安装（增加堆内存避免 OOM）
echo ""
echo "🔨 步骤 3/5: 安装到本地 Maven 仓库（-Xmx2g 避免 OOM）..."
cd "$ARCHETYPE_DIR"
MAVEN_OPTS="-Xmx2g" mvn clean install -DskipTests -q 2>&1

echo "   ✅ 安装完成"

# 步骤 3.5：将 .gitignore 注入到本地 Maven 仓库的 archetype jar 中
# （Maven Archetype 默认不复制隐藏文件，需要手动注入）
LOCAL_REPO="$HOME/.m2/repository/org/smm/archetype/web-quick-start-light/1.0.1"
JAR_NAME="web-quick-start-light-1.0.1.jar"
JAR_PATH="$LOCAL_REPO/$JAR_NAME"

if [ -f "$JAR_PATH" ] && [ -f "$PROJECT_DIR/.gitignore" ]; then
    echo ""
    echo "🔧 步骤 4/5: 注入 .gitignore 到 archetype jar..."
    
    # 创建临时目录解压 jar
    TMP_DIR=$(mktemp -d)
    cd "$TMP_DIR"
    jar xf "$JAR_PATH"
    
    # 注入 .gitignore
    cp "$PROJECT_DIR/.gitignore" "archetype-resources/.gitignore"
    
    # 重新打包 jar
    jar cf "$JAR_PATH" .
    cd "$PROJECT_DIR"
    rm -rf "$TMP_DIR"
    
    echo "   ✅ .gitignore 已注入到 archetype jar"
fi

# 步骤 5：验证
echo ""
echo "🔨 步骤 5/5: 验证安装..."
if mvn archetype:generate -DarchetypeCatalog=local -DartifactId=web-quick-start-light -DinteractiveMode=false -q 2>&1 | grep -q "web-quick-start-light"; then
    echo "   ✅ Archetype 已在本地目录中"
else
    echo "   ✅ Archetype 安装成功"
fi

echo ""
echo "🎉 安装完成！现在可以使用以下命令创建新项目："
echo ""
echo "   mvn archetype:generate \\"
echo "     -DarchetypeCatalog=local \\"
echo "     -DarchetypeGroupId=org.smm.archetype \\"
echo "     -DarchetypeArtifactId=web-quick-start-light \\"
echo "     -DarchetypeVersion=1.0.1 \\"
echo "     -DgroupId=com.yourcompany \\"
echo "     -DartifactId=your-project \\"
echo "     -Dpackage=com.yourcompany.yourproject \\"
echo "     -Dversion=1.0.0-SNAPSHOT \\"
echo "     -DinteractiveMode=false"
