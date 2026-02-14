# Service Initializer

Default backend service: Spring Boot, PostgreSQL, Redis, Kafka, GraphQL. Use this repo as a template to add your own domains.

## Domain structure (package-by-feature)

Code is organized **by domain** so each feature lives in one place and is easy to copy when adding new ones.

| Layer        | Location (example domain)        | Role |
|--------------|-----------------------------------|------|
| Entity       | `example/Example.kt`              | JPA entity, table mapping |
| Repository   | `example/ExampleRepository.kt`    | Spring Data JPA, persistence |
| Service      | `example/ExampleService.kt`       | Business logic, transactions |
| Controller   | `example/ExampleGraphQLController.kt` | GraphQL queries/mutations |
| Schema       | `graphql/example/schema.graphqls` | GraphQL types and operations |
| Migration    | `db/migration/V*__*.sql`          | Flyway, table DDL |

**Adding a new domain:** duplicate the `example` package and `graphql/example` schema, rename to your domain, then add a Flyway migration for the new table. See [DOMAINS.md](DOMAINS.md) for a step-by-step.

## Run

1. Start dependencies: `docker compose up -d`
2. Run the app: **Gradle** extension → **bootRun** task

## Links (when running locally)

| Service | URL |
|---------|-----|
| pgAdmin (PostgreSQL UI) | http://localhost:5050 (admin@local.dev / admin) |
| GraphiQL | http://localhost:8081/graphiql |
| Health | http://localhost:8081/actuator/health |
| Info | http://localhost:8081/actuator/info |
| Prometheus (actuator) | http://localhost:8081/actuator/prometheus |
| Kafka UI | http://localhost:8080 |
| Grafana | http://localhost:3000 (pre-provisioned: Prometheus, Tempo, Spring Boot dashboard in "Service" folder) |
| Prometheus | http://localhost:9090 |

## Reference Documentation

* [Spring Boot Gradle Plugin](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/gradle-plugin)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.1.0-SNAPSHOT/reference/actuator/index.html)
