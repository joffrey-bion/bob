FROM openjdk:15

COPY build/install/github-secrets-wizard /app

ENTRYPOINT ["/app/bin/github-secrets-wizard"]
