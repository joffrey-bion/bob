# focal == ubuntu-20.04 == GitHub's ubuntu-latest (at the moment)
FROM eclipse-temurin:17-jre-focal

COPY build/install/bob /app

ENTRYPOINT ["/app/bin/bob"]
