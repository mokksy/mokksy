# Runtime stage
FROM bellsoft/liberica-openjre-alpine-musl:26

RUN addgroup -S mokksy && adduser -S mokksy -G mokksy
WORKDIR /app

COPY --chown=mokksy:mokksy mokksy-jvm.jar ./
COPY --chown=mokksy:mokksy lib/ ./lib/

USER mokksy
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENV MOKKSY_CONFIG=/config/stubs.yaml
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp /app/mokksy-jvm.jar:/app/lib/* dev.mokksy.MainKt"]
