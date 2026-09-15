# Builds Nami's jar with Maven, then runs it on a plain Java 17 runtime.
#
# Built from the repository root (see compose.yml). Tests are skipped here because
# the only test starts the whole app, which needs a database.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
# Dependencies first, in their own layer, so a code change does not download them again.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
# Not root: the app only needs to read its jar and talk to the network.
RUN useradd --system --no-create-home nami
USER nami
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
