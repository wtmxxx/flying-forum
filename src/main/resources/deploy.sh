#!/bin/bash

cd G:/FLYING_FORUM/flying-forum || exit
mvn clean
mvn package -DskipTests=true

# 强制使用 UTF-8
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"
# 配置参数
SOURCE_ROOT="G:/FLYING_FORUM/flying-forum"
DEST_ROOT="G:/FLYING_FORUM/部署/生产/flying-forum/service"
TYPE1_SERVICES=("apidoc" "auth" "gateway" "oss")
TYPE2_SERVICES=("comment" "like" "post" "search" "user" "forum" "ai")

# 远程服务器配置
export PATH="/usr/bin:/bin:$PATH"
REMOTE_USER="ubuntu"                   # 远程服务器用户名
REMOTE_HOST="119.45.93.228"           # 远程服务器 IP 或域名
REMOTE_PATH="/home/ubuntu/flying-forum/service/"  # 远程同步目录
CYG_DEST_ROOT="/cygdrive/g/FLYING_FORUM/部署/生产/flying-forum/service/" # Cygwin 本地同步路径

# 主处理函数
process_service() {
    local service_name="$1"
    local source_path="$2"
    local dest_dir="$DEST_ROOT/$service_name"
    local dest_file="$dest_dir/$service_name.jar"

    echo -e "\n[处理中] 服务: $service_name"
    echo "源目录: $source_path"

    # 查找最新 JAR 文件
    jar_file=$(ls -t "$source_path/$service_name-"*.jar 2>/dev/null | head -n 1)

    if [[ -z "$jar_file" ]]; then
        echo -e "[错误] JAR 文件未找到: $service_name" >&2
        echo -e "预期路径模式: $source_path/$service_name-*.jar" >&2
        return
    fi

    # 创建目标目录
    mkdir -p "$dest_dir"
    echo "[创建目录] $dest_dir"

    # 移动文件（强制覆盖）
    mv -f "$jar_file" "$dest_file"
    echo -e "[成功] 已部署: $(basename "$jar_file") => $dest_file"
}

# 执行部署
echo -e "====== 开始自动化部署 ======\n"

# 处理 Type1 服务
for service in "${TYPE1_SERVICES[@]}"; do
    source_path="$SOURCE_ROOT/$service/target"
    process_service "$service" "$source_path"
done

# 处理 Type2 服务
for service in "${TYPE2_SERVICES[@]}"; do
    source_path="$SOURCE_ROOT/service/$service/target"
    process_service "$service" "$source_path"
done

# 清理 Maven 缓存
cd G:/FLYING_FORUM/flying-forum || exit
mvn clean

# ==== 使用 rsync 同步到远程服务器 ====
echo -e "\n[远程同步] 开始同步文件到 $REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH"

rsync -avz --delete --rsync-path="sudo rsync" "$CYG_DEST_ROOT" "$REMOTE_USER@$REMOTE_HOST:$REMOTE_PATH"

if [[ $? -eq 0 ]]; then
    echo -e "\n[成功] 文件已成功同步到远程服务器"
else
    echo -e "\n[错误] 文件同步失败，请检查 SSH 连接"
    exit 1
fi

# ==== 在远程服务器上重启 Docker Compose ====
echo -e "\n[远程重启] 重新启动 Docker Compose 服务"

ssh "$REMOTE_USER@$REMOTE_HOST" <<EOF
    cd
    sudo docker compose down
    sudo docker compose up -d
EOF

if [[ $? -eq 0 ]]; then
    echo -e "\n[成功] Docker Compose 服务已重启"
else
    echo -e "\n[错误] 远程 Docker Compose 重启失败，请检查服务器状态"
    exit 1
fi

echo -e "\n====== 所有操作已完成 ======"
echo -e "\n按 Enter 键退出..."
read -r
