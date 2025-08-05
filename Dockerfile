FROM openjdk:21-jdk-slim AS cms-demo-app
WORKDIR /app
# Copy the pre-built JAR from build/libs
COPY build/libs/*SNAPSHOT.jar /app.jar
# --enable-preview required for cf-reeve-platform dependencies
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app.jar"]
