# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/gradle-plugin/packaging-oci-image.html)
* [Spring Boot Testcontainers support](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/testing/testcontainers.html#testing.testcontainers)
* [Testcontainers Grafana Module Reference Guide](https://java.testcontainers.org/modules/grafana/)
* [Testcontainers Kafka Modules Reference Guide](https://java.testcontainers.org/modules/kafka/)
* [Testcontainers Postgres Module Reference Guide](https://java.testcontainers.org/modules/databases/postgres/)
* [Spring Web](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/web/servlet.html)
* [Spring for GraphQL](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/web/spring-graphql.html)
* [Spring REST Docs](https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/)
* [Flyway Migration](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/how-to/data-initialization.html#howto.data-initialization.migration-tool.flyway)
* [Spring Data Redis (Access+Driver)](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/data/nosql.html#data.nosql.redis)
* [Spring Cache Abstraction](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/io/caching.html)
* [Spring for Apache Kafka](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/messaging/kafka.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/actuator/index.html)
* [OpenTelemetry](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/actuator/observability.html#actuator.observability.opentelemetry)
* [Testcontainers](https://java.testcontainers.org/)
* [Validation](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/io/validation.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)
* [Building a GraphQL service](https://spring.io/guides/gs/graphql-server/)
* [Messaging with Redis](https://spring.io/guides/gs/messaging-redis/)
* [Caching Data with Spring](https://spring.io/guides/gs/caching/)
* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)

### Testcontainers support

This project uses [Testcontainers at development time](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/features/dev-services.html#features.dev-services.testcontainers).

Testcontainers has been configured to use the following Docker images:

* [`grafana/otel-lgtm:latest`](https://hub.docker.com/r/grafana/otel-lgtm)
* [`apache/kafka-native:latest`](https://hub.docker.com/r/apache/kafka-native)
* [`postgres:latest`](https://hub.docker.com/_/postgres)
* [`redis:latest`](https://hub.docker.com/_/redis)

Please review the tags of the used images and set them to the same as you're running in production.

