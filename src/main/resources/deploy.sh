#!/bin/bash

cd G:/FLYING_FORUM/flying-forum || exit
mvn clean
mvn package -Dmaven.test.skip=true

# 强制使用 UTF-8
export LANG="en_US.UTF-8"
export LC_ALL="en_US.UTF-8"
# 配置参数
SOURCE_ROOT="G:/FLYING_FORUM/flying-forum"
DEST_ROOT="G:/FLYING_FORUM/部署/生产/flying-forum/service"
TYPE1_SERVICES=("apidoc" "auth" "gateway" "oss")
TYPE2_SERVICES=("comment" "like" "post" "rag" "search" "user" "forum")

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

echo -e "\n====== 所有操作已完成 ======"
echo -e "\n按 Enter 键退出..."
read -r
