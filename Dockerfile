FROM maven:3.3.9-jdk-8-alpine as builder

RUN apk update && apk add jq

WORKDIR /build

COPY pom.xml .

# Cacheamos dependencias en esta layer, que no deberia ser modifica muy seguido.
RUN mvn -B dependency:go-offline package -DskipTests=true && rm -rf target/

COPY src ./src

# Test image =====================================================================
FROM builder as tester

ENTRYPOINT ["mvn", "test"]