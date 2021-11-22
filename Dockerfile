FROM openjdk:15

COPY build/install/bob /app

ENTRYPOINT ["/app/bin/bob"]
