# 阶段1：用Maven镜像编译项目，生成jar包
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
# 复制pom.xml，先下载依赖（利用缓存）
COPY pom.xml .
RUN mvn dependency:go-offline
# 复制全部源码
COPY src ./src
# 执行Maven打包，跳过测试
RUN mvn clean package -DskipTests

# 阶段2：用轻量JDK镜像运行
FROM eclipse-temurin:17-jdk
WORKDIR /app
# 从builder阶段复制生成的jar包
COPY --from=builder /app/target/*.jar app.jar
# 暴露端口
EXPOSE 8080
# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
