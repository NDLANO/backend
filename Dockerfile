ARG JAVA_MAJOR_VERSION=25

FROM eclipse-temurin:${JAVA_MAJOR_VERSION}-alpine AS builder
ARG MODULE
ARG JAVA_MAJOR_VERSION

WORKDIR /app

# Build Scala backend module
RUN apk add --no-cache curl jq

ARG OTEL_AGENT_VERSION=2.27.0
RUN curl -sSfL -o /otel-agent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"

COPY . .
RUN ./mill -i ${MODULE}.assembly

# Create list of required Java modules
RUN ./mill -i show ${MODULE}.runClasspath | jq -r 'join(":")' > classpath.info
RUN $JAVA_HOME/bin/jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    --recursive \
    --multi-release ${JAVA_MAJOR_VERSION} \
    --class-path "$(cat classpath.info)" \
    out/${MODULE}/compile.dest/classes > deps.info

RUN $JAVA_HOME/bin/jlink \
         --add-modules "$(cat deps.info),java.instrument,jdk.unsupported,java.management,jdk.management,jdk.attach,java.naming,java.sql,java.net.http,jdk.jfr,java.security.sasl" \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=zip-6 \
         --output /javaruntime


FROM alpine:3.23
ARG MODULE

WORKDIR /app

RUN apk add fontconfig && apk add ttf-dejavu

# Set up custom JRE
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=builder /javaruntime $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Set up and run Scala app
COPY --from=builder /app/out/${MODULE}/assembly.dest/out.jar /app/out.jar
COPY --from=builder /otel-agent.jar /app/opentelemetry-javaagent.jar
ENV LOG_APPENDER=Docker
COPY jvm-runtime-options /app/jvm-runtime-options
ENTRYPOINT ["sh", "-c", "exec java ${OTEL_JAVAAGENT:+-javaagent:/app/opentelemetry-javaagent.jar} @/app/jvm-runtime-options $JAVA_OPTS -jar /app/out.jar"]
