ARG JAVA_MAJOR_VERSION=25

FROM eclipse-temurin:${JAVA_MAJOR_VERSION}-alpine AS builder
ARG JAVA_MAJOR_VERSION

WORKDIR /app

# Build taxonomy-api
RUN apk add --no-cache maven
COPY . .
RUN mvn clean package -DskipTests

# Create list of required Java modules
RUN $JAVA_HOME/bin/jar xf target/taxonomy-service.jar
RUN $JAVA_HOME/bin/jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    --recursive \
    --multi-release ${JAVA_MAJOR_VERSION} \
    --class-path 'BOOT-INF/lib/*' \
    target/taxonomy-service.jar > deps.info

# Create custom JRE with the above modules
RUN $JAVA_HOME/bin/jlink \
         --add-modules $(cat deps.info) \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=zip-6 \
         --output /javaruntime


FROM alpine:3.23

WORKDIR /app

# Set up custom JRE
ENV JAVA_HOME=/opt/java/openjdk
COPY --from=builder /javaruntime $JAVA_HOME
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Set up and run taxonomy-api
COPY --from=builder /app/target/taxonomy-service.jar /app/out.jar
ENV SPRING_PROFILES_ACTIVE=""
ENV SPRING_PROFILES_ACTIVE="docker${SPRING_PROFILES_ACTIVE:+,$SPRING_PROFILES_ACTIVE}"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/out.jar"]
