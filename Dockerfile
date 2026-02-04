ARG JAVA_MAJOR_VERSION=25

FROM eclipse-temurin:${JAVA_MAJOR_VERSION}-alpine AS builder
ARG MODULE
ARG JAVA_MAJOR_VERSION

WORKDIR /app

# Build Scala backend module
RUN apk add --no-cache curl
COPY . .
RUN ./mill -i ${MODULE}.assembly

# Create list of required Java modules
RUN $JAVA_HOME/bin/jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    --multi-release ${JAVA_MAJOR_VERSION} \
    out/${MODULE}/assembly.dest/out.jar > /deps.info

# Create custom JRE with the above modules
RUN $JAVA_HOME/bin/jlink \
         --add-modules $(cat /deps.info) \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=zip-6 \
         --output /javaruntime


FROM alpine:3.23
ARG MODULE

WORKDIR /app

# Set up custom JRE
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=builder /javaruntime $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Set up and run Scala app
COPY --from=builder /app/out/${MODULE}/assembly.dest/out.jar /app/out.jar
ENV LOG_APPENDER=Docker
ENTRYPOINT ["sh", "-c", "exec java -jar /app/out.jar $JAVA_OPTS"]
