FROM eclipse-temurin:21-jre

COPY build/install/bob /app

ENTRYPOINT ["/app/bin/bob"]
