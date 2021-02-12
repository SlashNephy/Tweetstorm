# Gradle Cache Dependencies Stage
# This stage caches plugin/project dependencies from *.gradle.kts and gradle.properties.
# Gradle image erases GRADLE_USER_HOME each layer. So we need COPY GRADLE_USER_HOME.
# Refer https://stackoverflow.com/a/59022743
FROM gradle:jdk8 AS cache
WORKDIR /app
ENV GRADLE_USER_HOME /app/gradle
COPY *.gradle.kts gradle.properties /app/
# Full build if there are any deps changes
RUN gradle shadowJar --parallel --no-daemon --quiet

# Gradle Build Stage
# This stage builds and generates fat jar.
FROM gradle:jdk8 AS build
WORKDIR /app
COPY --from=cache /app/gradle /home/gradle/.gradle
COPY *.gradle.kts gradle.properties /app/
COPY src/jvmMain/ /app/src/jvmMain/
# Stop printing Welcome
RUN gradle -version > /dev/null \
    && gradle shadowJar --parallel --no-daemon

# Final Stage
FROM openjdk:17-jdk-alpine

COPY --from=build /app/build/libs/tweetstorm-all.jar /app/tweetstorm.jar

WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/tweetstorm.jar"]
