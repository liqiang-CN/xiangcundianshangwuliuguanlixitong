# 基础镜像：使用OpenJDK 17
FROM openjdk:17-jdk-slim

# 工作目录
WORKDIR /app

# 复制Maven构建产物
COPY target/*.jar app.jar

# 暴露端口（SpringBoot默认8080）
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
