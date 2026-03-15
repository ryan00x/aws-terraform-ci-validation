# Contributing to Floci

Thank you for your interest in contributing! Floci is a community-driven project and all contributions are welcome.

## Ways to Contribute

- **Bug reports** — open an issue with a minimal reproduction
- **Feature requests** — open an issue describing the AWS behavior you need
- **Pull requests** — bug fixes, new service implementations, or improvements
- **Compatibility tests** — add cases to `../floci-compatibility-tests`

## Getting Started

### Prerequisites

- Java 25+
- Maven 3.9+
- Docker (for integration tests that spin up Lambda/RDS/ElastiCache)

### Build & Run

```bash
git clone https://github.com/hectorvent/floci.git
cd floci
mvn quarkus:dev        # hot reload on port 4566
```

### Run Tests

```bash
mvn test                                          # all tests
mvn test -Dtest=SsmIntegrationTest                # single class
mvn test -Dtest=SsmIntegrationTest#putParameter   # single method
```

## Commit Message Format

This project uses [Conventional Commits](https://www.conventionalcommits.org/) — semantic-release reads these to generate the changelog and version bumps automatically.

| Prefix | When to use | Version bump |
|--------|-------------|--------------|
| `feat:` | New AWS API action or service | minor |
| `fix:` | Bug fix or AWS compatibility correction | patch |
| `perf:` | Performance improvement | patch |
| `docs:` | Documentation only | none |
| `chore:` | Build, CI, dependencies | none |
| `BREAKING CHANGE:` | Footer or `!` suffix — incompatible change | major |

**Examples:**

```
feat: add SQS SendMessageBatch action
fix: correct DynamoDB QueryFilter comparison operators
feat!: change default storage mode to persistent
```

## Architecture

See [CLAUDE.md](CLAUDE.md) for a detailed description of the three-layer architecture (Controller → Service → Storage), the AWS wire protocol mapping, and conventions for adding new services.

## Adding a New AWS Service

1. Create a package under `src/main/java/.../services/<service>/`
2. Add a Controller (follow the correct protocol — Query, JSON 1.1, REST JSON, or REST XML)
3. Add a Service (`@ApplicationScoped`) and model POJOs
4. Register in `ServiceRegistry`
5. Add config entries in `EmulatorConfig.java` and `application.yml`
6. Add integration tests in `*IntegrationTest.java`

Always implement the **real AWS wire protocol** — never invent custom endpoints. The AWS SDK must work against Floci without modification.

## Pull Request Guidelines

- Keep PRs focused — one feature or fix per PR
- Add or update tests for any changed behavior
- Ensure `mvn test` passes before opening the PR
- Reference any related issues in the PR description

## Reporting Security Issues

Please do **not** open public issues for security vulnerabilities. Report them privately by emailing the maintainer or using [GitHub private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability).