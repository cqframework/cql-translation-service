# fetch basic image
FROM maven:3.9.5-eclipse-temurin-11

# application placed into /opt/app
RUN mkdir -p /app
WORKDIR /app

# selectively add the POM file and
# install dependencies
COPY pom.xml /app/
RUN mvn install

# rest of the project
COPY src /app/src
RUN mvn package

# local application port
EXPOSE 8080

# execute it
# CMD ["mvn", "exec:java"]
CMD ["java", "-jar", "target/cqlTranslationServer-2.2.0.jar", "-d"]
