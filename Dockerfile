ARG JAVA_MAJOR_VERSION=25

FROM eclipse-temurin:${JAVA_MAJOR_VERSION}-alpine AS builder
ARG MODULE
WORKDIR /app
RUN apk add --no-cache curl
COPY . .
RUN ./mill -i ${MODULE}.assembly

FROM eclipse-temurin:${JAVA_MAJOR_VERSION}-jre-alpine
ARG MODULE
WORKDIR /app
RUN apk add fontconfig && apk add ttf-dejavu
COPY --from=builder /app/out/${MODULE}/assembly.dest/out.jar /app/out.jar
ENV LOG_APPENDER=Docker
COPY jvm-runtime-options /app/jvm-runtime-options
ENTRYPOINT ["sh", "-c", "exec java @/app/jvm-runtime-options $JAVA_OPTS -jar /app/out.jar"]
