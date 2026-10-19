# The JAR is built on the CI runner rather than here: Java bytecode is platform neutral,
# so only this runtime layer needs to be arm64 and no QEMU emulation is involved.
FROM eclipse-temurin:25-jre

RUN useradd --system --create-home --uid 10001 app
USER app
WORKDIR /app

COPY --chown=app:app build/libs/*.jar app.jar

EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar \"$@\"", "--"]
