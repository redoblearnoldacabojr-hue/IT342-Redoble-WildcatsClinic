FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# copy Maven wrapper and pom first to leverage Docker layer caching
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src ./src

RUN chmod +x mvnw || true
RUN ./mvnw -DskipTests package -DskipTests -B

EXPOSE 8080

CMD ["java", "-jar", "target/wildcatclinic-0.0.1-SNAPSHOT.jar"]
