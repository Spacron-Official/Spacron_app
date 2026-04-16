# Spacron_app

## Build requirements

- Java JDK 17 or newer for both Android and Spring Boot.
- Android SDK installed and configured via `ANDROID_HOME` or `android-app/local.properties`.
- `springboot-api` currently does not include a Gradle wrapper, so use the system Gradle or add a wrapper for reproducible builds.

## Recommended local setup

1. Install a JDK 17+ and set `JAVA_HOME`.
2. Install Android SDK and set `ANDROID_HOME` or update `android-app/local.properties`.
3. From `/workspaces/Spacron_app/springboot-api`, run:
   `JAVA_HOME=/path/to/jdk gradle build`
4. From `/workspaces/Spacron_app/android-app`, run:
   `JAVA_HOME=/path/to/jdk ./gradlew build`
