FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY ./build/libs/webgam-0.0.1-SNAPSHOT.jar /app/webgam-server.jar
EXPOSE 8080
CMD java -jar /app/webgam-server.jar
