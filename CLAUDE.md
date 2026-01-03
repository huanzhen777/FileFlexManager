# FileFlexManager - Project Context

## Project Overview

**FileFlexManager** is a web-based file manager designed for Linux NAS systems with a mobile-first approach.

### Key Features
- File operations (browse, upload, download, delete, move, copy)
- Web-based user interface optimized for mobile devices
- Multi-user support with permission control
- Docker deployment for Linux environments

---

## Technology Stack

### Backend
- **Java**: JDK 21
- **Framework**: Spring Boot 3
- **Build Tool**: Gradle (multi-module project)
- **ORM**: MyBatis Plus
- **Object Mapping**: MapStruct
- **Database**: H2 / SQLite (embedded)
- **Architecture**: Domain-Driven Design (DDD)
- **Deployment**: Docker

### Frontend
- **Framework**: Vue 3 with Composition API
- **Language**: TypeScript
- **UI Library**: Vant (mobile-first components)
- **API Integration**: Unified configuration via `config.ts`

---

## Project Structure

### Backend Modules

```
backend/
├── domain/                    # Domain layer (core business logic)
│   ├── event/                # Domain events
│   ├── model/                # Domain models
│   ├── repository/           # Repository interfaces
│   ├── service/              # Domain services
│   └── utils/                # Domain utilities
│
├── application/              # Application layer (use cases)
│   ├── assembler/           # Application assemblers
│   ├── config/              # Application configuration
│   ├── dto/                 # Data Transfer Objects
│   ├── event/               # Application events
│   ├── scheduler/           # Scheduled tasks
│   └── service/             # Application services
│
├── infrastructure/           # Infrastructure layer
│   ├── config/              # Infrastructure configuration
│   ├── external/            # External service integrations
│   ├── persistence/         # Data persistence
│   │   ├── converter/       # Data converters
│   │   ├── entity/          # JPA entities
│   │   ├── mapper/          # MyBatis mappers
│   │   ├── po/              # Persistent objects
│   │   └── repository/      # Repository implementations
│   ├── security/            # Security implementations
│   ├── util/                # Infrastructure utilities
│   └── task/                # Task handlers
│       └── handler/
│
└── interfaces/              # Interface layer (API endpoints)
    └── src/main/
        ├── java/com.huanzhen.fileflexmanager.interfaces/
        │   ├── api/
        │   │   ├── advice/         # Global exception handlers
        │   │   ├── assembler/      # API assemblers
        │   │   └── controller/     # REST controllers
        │   ├── config/             # Interface configuration
        │   ├── controller/         # Legacy controllers
        │   ├── convert/            # Converters
        │   ├── converter/          # Additional converters
        │   ├── exception/          # Custom exceptions
        │   ├── facade/             # Facade patterns
        │   └── model/
        │       ├── dto/            # DTOs
        │       ├── req/            # Request models
        │       ├── resp/           # Response models
        │       └── vo/             # View objects
        └── resources/
            ├── data/               # Static data
            ├── db.migration/       # Database migrations
            ├── static/             # Static resources
            └── application*.yml    # Configuration files
```

### Frontend Structure

```
frontend/
├── src/
│   ├── api/              # API service layer
│   ├── assets/           # Static assets
│   ├── components/       # Reusable Vue components
│   ├── router/           # Vue Router configuration
│   ├── stores/           # Pinia stores (state management)
│   ├── utils/            # Utility functions
│   └── views/            # Page components
├── public/               # Public static files
├── dist/                 # Build output
└── config/
    ├── env/              # Environment configurations
    ├── typescript/       # TypeScript configurations
    └── build/            # Build configurations
```

---

## Architecture Principles

### 1. Domain-Driven Design (DDD)

**Layer Dependencies:**
```
interfaces → application → domain ← infrastructure
```

**Rules:**
- ✅ Strict layer boundaries - each layer only depends on the domain layer and the layer directly below it
- ✅ Domain logic isolation - business logic stays in the domain layer
- ✅ Domain models are technology-agnostic
- ❌ Never let infrastructure concerns leak into domain layer

### 2. Object Mapping with MapStruct

**Mapping Strategy:**
- **Interface Layer**: `DTO ↔ Domain Model` (using converters in `interfaces/converter/`)
- **Infrastructure Layer**: `Domain Model ↔ PO/Entity` (using converters in `infrastructure/persistence/converter/`)

**Rules:**
- ✅ Always use MapStruct for object conversions
- ✅ Define mapping interfaces with `@Mapper` annotation
- ✅ Use compile-time generation (no reflection)
- ✅ Add custom mapping rules when needed
- ❌ Never manually map objects in service layer

### 3. Data Model Layers

- **DTO (Data Transfer Object)**: Used in API layer for client communication
- **Domain Model**: Core business entities in domain layer
- **PO (Persistent Object) / Entity**: Database entities in infrastructure layer

**Rules:**
- Each layer uses its own model type
- Always convert between layers using MapStruct
- Never expose PO/Entity directly to API clients

---

## Coding Standards

### Backend Standards

#### 1. Exception Handling
- ✅ Use global exception handler (`@ControllerAdvice`)
- ✅ Define custom exceptions in `interfaces/exception/`
- ✅ Return standardized `BaseResponse` for all API responses
- ❌ Never catch and swallow exceptions without logging

#### 2. API Responses
- ✅ All API endpoints return `BaseResponse<T>`
- ✅ Include proper HTTP status codes
- ✅ Provide meaningful error messages

#### 3. Domain Events
- ✅ Use domain events for cross-aggregate communication
- ✅ Define events in `domain/event/`
- ✅ Handle events in `application/event/`

#### 4. Data Access
- ✅ Use Repository pattern
- ✅ Define repository interfaces in `domain/repository/`
- ✅ Implement repositories in `infrastructure/persistence/repository/`
- ✅ Use MyBatis Plus for database operations

### Frontend Standards

#### 1. API Integration
- ✅ Use Vue 3 Composition API
- ✅ Define API calls in `src/api/`
- ✅ Use unified request handler from `config.ts`
- ✅ Handle `BaseResponse` format from backend

#### 2. TypeScript
- ✅ Strictly typed - no `any` types
- ✅ Define interfaces for all data models
- ✅ Use type inference where possible

#### 3. Import Paths
- ✅ Always use path aliases (e.g., `@/components/`)
- ❌ Never use relative imports like `../../components/`

#### 4. Component Structure
- ✅ Use Vant components for UI consistency
- ✅ Mobile-first responsive design
- ✅ Composition API with `<script setup>`

---

## Testing Standards

### Unit Tests

**Framework:** JUnit 5

**Naming Convention:**
- Test classes: `*Test.java`
- Test methods: camelCase (e.g., `shouldReturnUserWhenIdExists()`)

**Location:**
```
src/test/java/{package}/
```

**Structure:**
```java
class UserServiceTest {

    @BeforeEach
    void setUp() {
        // Setup test fixtures
    }

    @Test
    void shouldReturnUserWhenIdExists() {
        // Test implementation
    }

    @Test
    void shouldThrowExceptionWhenIdNotFound() {
        // Test implementation
    }
}
```

**Coverage Requirements:**
- ✅ Core business logic must have tests
- ✅ Mock external dependencies
- ✅ Test both success and failure scenarios
- ✅ Use `@MockBean` for Spring components

### Integration Tests

**Location:**
```
backend/interfaces/src/test/java/
```

**Naming Convention:**
- Test classes: `*IntegrationTest.java`
- Configuration: `TestConfig.java`

**Rules:**
- ✅ Use `@SpringBootTest` for full context
- ✅ Test API endpoints end-to-end
- ✅ Use test database (H2 in-memory)

---

## Development Environment

- ✅ Docker support for deployment
- ✅ Gradle for build automation
- ✅ Git for version control
- ✅ Linux target environment

---

## Important Constraints

1. **Mobile-First**: Always prioritize mobile experience in frontend development
2. **DDD Boundaries**: Respect layer boundaries strictly
3. **Type Safety**: Use MapStruct and TypeScript for compile-time safety
4. **Embedded Database**: Use H2 or SQLite (no external database required)
5. **Docker Deployment**: Application must be containerized
6. **Self-Contained**: Application should run standalone on Linux NAS

---

## When Modifying Code

1. **Read before write**: Always read existing code to understand patterns
2. **Follow existing patterns**: Match the coding style and architecture
3. **Update all layers**: When adding features, update DTO, Domain, and PO layers
4. **Add mappings**: Create MapStruct converters for new models
5. **Write tests**: Add unit tests for new business logic
6. **Use BaseResponse**: All API endpoints must return standardized responses
7. **Mobile-first**: Test responsive design on mobile viewport
