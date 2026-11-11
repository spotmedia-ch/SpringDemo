FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
# Resolving dependencies before copying the sources keeps this layer cached until pom.xml changes.
RUN ./mvnw -q dependency:go-offline
COPY src src
# Tests need Docker (Testcontainers), which is not available inside a build; CI runs them separately.
RUN ./mvnw -q package -DskipTests
RUN java -Djarmode=tools -jar target/registry-*.jar extract --layers --destination /workspace/extracted

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "registry-0.0.1-SNAPSHOT.jar"]
