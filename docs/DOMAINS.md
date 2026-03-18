# Domain structure guide

This project uses **package-by-feature** (domain-driven layout): each business domain has its own package with Entity, Repository, Service, and GraphQL Controller. The `example` domain is the reference implementation.

## Example domain (reference)

- **`com.mafauser.service.example.Example`** – JPA entity: UUID id, version (optimistic locking), name, description, createdAt, updatedAt.
- **`ExampleRepository`** – `JpaRepository<Example, UUID>` with `existsByName`.
- **`ExampleService`** – `@Transactional` CRUD; uses `CreateExampleInput` / `UpdateExampleInput` (Bean Validation); throws shared `NotFoundException`, `ConflictException`, `InvalidIdException` from `exception/`.
- **`ExampleController`** – REST `@RestController`: CRUD at `/examples` (GET, POST, PUT, DELETE).
- **`ExampleGraphQLController`** – GraphQL `@Controller`: queries `examples`, `example(id)`; mutations `createExample`, `updateExample`, `deleteExample`.
- **`graphql/example/schema.graphqls`** – Type `Example`, inputs `CreateExampleInput`, `UpdateExampleInput`, and `extend type Query` / `extend type Mutation`.
- **`db/migration/V1__create_example_table.sql`** – Table `examples` and unique index on `name`.

## Adding a new domain (e.g. `product`)

1. **Package**  
   Create `src/main/kotlin/.../product/` with:
   - `Product.kt` (entity)
   - `ProductRepository.kt` (extends `JpaRepository<Product, UUID>`)
   - `ProductService.kt` (constructor-inject repository, input DTOs, domain exceptions)
   - `ProductController.kt` (REST `@RestController`)
   - `ProductGraphQLController.kt` (GraphQL `@Controller`)

2. **GraphQL schema**  
   Create `src/main/resources/graphql/product/schema.graphqls` with:
   - `type Product { ... }`
   - `input CreateProductInput { ... }` / `input UpdateProductInput { ... }`
   - `extend type Query { products: [Product!]!, product(id: ID!): Product }`
   - `extend type Mutation { createProduct(...): Product!, ... }`

3. **Database**  
   Add a Flyway migration under `src/main/resources/db/migration/`, e.g. `V3__create_product_table.sql`, with `CREATE TABLE products (...)` and any indexes. Use the next available version number (`V3__`, `V4__`, etc.).

4. **Conventions**
   - Use **UUID** for primary keys.
   - Use **version** (BIGINT) for optimistic locking.
   - Use **createdAt** / **updatedAt** (TIMESTAMP) for audit.
   - Keep input DTOs in the service package (e.g. `CreateExampleInput`, `UpdateExampleInput`).
   - Throw shared exceptions (`NotFoundException`, `ConflictException`). `GlobalExceptionHandler` maps them to HTTP status codes; GraphQL maps them to errors.

Copy from the `example` package and rename; then adjust fields and validation to match your domain.

## GraphQL schema (schema-first)

This project uses **Spring GraphQL**, which is **schema-first**: you write `.graphqls` files by hand; the schema is the source of truth and must stay in sync with your controllers.

## Tests

- **Unit**: `ExampleServiceTest` – mocks `ExampleRepository`, tests all service methods and domain exceptions.
- **Integration**: `ExampleControllerIntegrationTest` (GraphQL), `ExampleRestControllerIntegrationTest` (REST) – both extend `BaseIntegrationTest` (Testcontainers: Postgres, Redis, Kafka). Requires Docker.
- GraphQL documents: `src/test/resources/graphql-test/` (e.g. `examples-query.graphql`, `delete-example-mutation.graphql`).