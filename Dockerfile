# 第一阶段：构建阶段
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# 创建必要的目录
RUN mkdir -p /root/.gradle /root/.npm

# 配置 Gradle 镜像
RUN echo "allprojects { \n\
    repositories { \n\
        maven { url 'https://maven.aliyun.com/repository/public' } \n\
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' } \n\
        mavenLocal() \n\
        mavenCentral() \n\
    } \n\
}" > /root/.gradle/init.gradle

RUN apt-get update && apt-get install -y \
    rsync

# 复制整个项目
COPY . .

# 设置 Gradle Wrapper 执行权限
RUN chmod +x ./gradlew

# 执行 buildAll 任务（包含前端和后端构建）
RUN #./gradlew build --no-daemon --info --stacktrace --debug
RUN ./gradlew build --no-daemon --info


# 第二阶段：运行阶段
FROM eclipse-temurin:21-jre

WORKDIR /app

# 安装必要的运行时工具
RUN apt-get update && apt-get install -y \
    bash \
    curl \
    rsync \
    && rm -rf /var/lib/apt/lists/*

# 创建应用用户和组
ARG USER_ID=1000
ARG GROUP_ID=1000

# 检查组是否存在，不存在则创建
RUN if getent group $GROUP_ID > /dev/null 2>&1; then \
        echo "Group with ID $GROUP_ID already exists"; \
    else \
        groupadd -g $GROUP_ID appuser; \
    fi

# 检查用户是否存在，不存在则创建
RUN if id -u $USER_ID > /dev/null 2>&1; then \
        echo "User with ID $USER_ID already exists"; \
        # 获取现有用户名
        EXISTING_USER=$(getent passwd $USER_ID | cut -d: -f1); \
        # 修改现有用户的组ID
        usermod -g $GROUP_ID $EXISTING_USER; \
    else \
        useradd -u $USER_ID -g $GROUP_ID -m -s /bin/bash appuser; \
    fi

# 获取实际的用户名（无论是新建的还是已存在的）
RUN ACTUAL_USER=$(getent passwd $USER_ID | cut -d: -f1)

# 创建持久化所需的目录和默认配置文件
RUN mkdir -p /app/config/conf \
    /app/config/db \
    /app/config/logs \
    /app/data && \
    chown -R $USER_ID:$GROUP_ID /app

# 从构建阶段复制构建结果
COPY --from=builder --chown=$USER_ID:$GROUP_ID /app/backend/interfaces/build/libs/*.jar /app/app.jar

# 设置环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

# 声明数据卷
VOLUME ["/app/config", "/app/data"]

# 暴露端口
EXPOSE 8080

# 切换到实际用户
USER $USER_ID

# 启动命令
ENTRYPOINT ["java", "-jar", "/app/app.jar"] 