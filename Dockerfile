FROM eclipse-temurin:17-jdk-alpine
WORKDIR /src
COPY . /src
RUN /src/gradlew clean assemble

FROM eclipse-temurin:17-jdk-alpine
RUN mkdir /opt/app
COPY --from=0 /src/build/libs/jeffdb-0.0.1-SNAPSHOT.jar /opt/app
EXPOSE 8080/tcp
ENV DATA_DIR=/data
VOLUME /data
CMD ["java", "-jar", "/opt/app/jeffdb-0.0.1-SNAPSHOT.jar"]
