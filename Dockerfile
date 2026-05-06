###################################################################################################
# Stage 1: Build the application
###################################################################################################

FROM dhi.io/maven:3-jdk17-debian13-dev AS build
WORKDIR /app

# selectively add the POM file and install dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# add the src and package it
COPY src src
RUN mvn package

###################################################################################################
# Stage 2: Create the final runtime image
###################################################################################################

FROM dhi.io/eclipse-temurin:17-debian13
WORKDIR /app

# copy the translation service jar and dependency libs from the previous build
COPY --from=build /app/target/cqlTranslationServer-*.jar cqlTranslationServer.jar
COPY --from=build /app/target/libs libs/

# Expose the port and run it!
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/cqlTranslationServer.jar", "-d"]
