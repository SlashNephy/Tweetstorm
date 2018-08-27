FROM gradle:latest as build
LABEL maintainer="Suzuka Asagiri <suzutan0s2@suzutan.jp>"
LABEL description="A simple substitute implementation for the Twitter UserStream"

RUN git clone https://github.com/SlashNephy/Tweetstorm
WORKDIR Tweetstorm
RUN gradle wrapper && \
    ./gradlew build

FROM openjdk:8-alpine
COPY --from=build /home/gradle/Tweetstorm/build/libs/tweetstorm-full.jar /

ENTRYPOINT [ "java", "-jar", "tweetstorm-full.jar" ]

