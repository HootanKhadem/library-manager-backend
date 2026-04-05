# Project Specific Guidelines for Junie

As the AI agent for this project, you must follow these guidelines at all times to ensure high code quality and security.

### 1. Request Validation and Best Practices
- **Always Validate User Requests:** For every task or request from the user, you must validate whether it follows industry-standard best practices.
- **Explain Alternatives:** If a user request is not the best practice (e.g., insecure configuration, inefficient logic, poor architectural choice), you must:
    - Explain **why** the current request is not ideal.
    - Present the **best practice solution**.
    - **Wait for user confirmation** before implementing the "right way" or proceeding with the original request if the user insists.
- **Stay Informed:** Always use your internal knowledge and any available search tools to research and apply the most up-to-date best practices for the technologies used in this project (Kotlin, Ktor, Maven, PostgreSQL, etc.).

### 2. Technology-Specific Best Practices
- **Kotlin & Ktor:** Follow Kotlin's idiomatic patterns and Ktor's official documentation for routing, security, and dependency injection.
- **Security:** Never hardcode secrets. Use environment variables or configuration files (`application.yaml`). Ensure JWT tokens have appropriate expiration times and use strong algorithms (e.g., RS256 or strong HMAC256).
- **Testing:** Always write tests for new features and bug fixes. Ensure high test coverage for critical paths like authentication and data persistence.
- **Architecture:** Maintain a clear separation of concerns between models, services, and routes.
