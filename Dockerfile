ARG BASE_IMAGE=eclipse-temurin:21-alpine
FROM ${BASE_IMAGE} AS builder
WORKDIR /app
RUN apk add --no-cache bash curl coreutils
ARG MILL_VERSION=1.0.4-jvm
ENV MILL_VERSION=${MILL_VERSION}
ARG MODULE
COPY . .
RUN ./mill -i ${MODULE}.assembly


FROM ${BASE_IMAGE}
WORKDIR /
ENV LOG_APPENDER=Docker
ARG MODULE
ARG APP_JAR=/out.jar
COPY --from=builder /app/out/${MODULE}/assembly.dest/out.jar ${APP_JAR}
ENV APP_JAR=${APP_JAR}
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar ${APP_JAR}"]
