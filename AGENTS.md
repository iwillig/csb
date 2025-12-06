# CSB Agents

This document describes the LLM agents for the Clojure Software Builder (CSB) project.

## Overview

The Clojure Software Builder (CSB) is a sophisticated development tool designed to enhance productivity when working with LLM agents and the Clojure programming language. It leverages Clojure's strengths to create a powerful development environment with:

- **Typed Clojure** for static verification
- **SQLite** for primary storage with JSON and full-text capabilities
- **Datastar** for the primary UI framework
- **Hiccup** for HTML generation
- **HTTP-Kit** for HTTP server capabilities
- **rewrite-clj** for Clojure parsing

## Agent Capabilities

### Core Development Agent

The primary agent for this project is designed to assist with:

1. **Clojure Development**: 
   - Writing, reviewing, and refactoring Clojure code
   - Leveraging Typed Clojure for type safety
   - Working with idiomatic Clojure patterns

2. **Database Management**:
   - Utilizing SQLite with the established migration system
   - Working with the `resources/migrations` directory
   - Understanding and extending the database schema (currently including `001-initial-schema.edn`)

3. **Project Navigation**:
   - Familiar with the REPL-driven development workflow
   - Understanding the dev.clj file structure with `refresh`, `lint`, `migration`, and `type-check` functions
   - Aware of project structure including `src`, `test`, `dev`, and `resources` directories

4. **Tool Integration**:
   - Working with clojure-lsp for code intelligence
   - Using clj-kondo for linting
   - Leveraging typed Clojure for static verification
   - Utilizing Ragtime for database migrations

### Development Workflow

The project embraces a REPL-driven development approach with the following key workflows:

- **Refresh**: `dev/refresh` - Reloads Clojure namespaces for iterative development
- **Lint**: `dev/lint` - Uses clj-kondo to lint the codebase
- **Type Check**: `dev/type-check` - Uses Typed Clojure to verify type correctness
- **Migrate**: `dev/migrate` - Migrates the database using the established migration system

### Database Schema

The project includes a comprehensive database schema defined in `resources/migrations/001-initial-schema.edn` which includes:

- Core entities: project, plan, task, file, file_content
- Reference tables: plan_state for tracking plan lifecycle
- Relationship tables: plan_skills for associating skills with plans
- Indexes for performance optimization
- Full-text search (FTS5) tables and triggers
- Update timestamp triggers for automatic timestamp maintenance

### Technology Stack Integration

The agent should be familiar with these technologies and their integration in the project:

1. **Typed Clojure**: Used for static type checking in the `src` directory
2. **SQLite**: Primary storage with JSON and full-text search capabilities
3. **Datastar**: Used for UI development
4. **Hiccup**: For generating HTML
5. **HTTP-Kit**: For serving HTTP requests
6. **rewrite-clj**: For parsing Clojure code

### Migration System

The project uses Ragtime for database migrations. The migration system:

- Loads migrations from `resources/migrations/`
- Currently includes one migration: `001-initial-schema.edn`
- Supports both up and down migrations
- Can be managed via the `dev/migrate` function in dev.clj

## Agent Instructions

When working with this project, the agent should:

1. **Follow REPL-driven development**: Test code in the REPL before committing to files
2. **Respect the project structure**: Work within the established directories and conventions
3. **Understand the database schema**: Be familiar with the current schema and how to extend it
4. **Use proper migration practices**: When making database changes, create new migration files
5. **Leverage existing tooling**: Use the dev.clj functions for common operations
6. **Maintain type safety**: Ensure code adheres to Typed Clojure specifications
7. **Consider performance**: Be mindful of indexing and query optimization

## Available Commands

The agent can work with these development commands through the REPL:

- `dev/refresh` - Reloads Clojure namespaces
- `dev/lint` - Lints the project using clj-kondo
- `dev/type-check` - Performs type checking with Typed Clojure
- `dev/migrate` - Applies database migrations

## Key Files and Directories

- `src/` - Main source code directory
- `test/` - Test files
- `dev/` - Development utilities and REPL helpers
- `resources/migrations/` - Database migration files
- `readme.md` - Project documentation
- `deps.edn` - Project dependencies
- `dev/dev.clj` - Development utilities

## Testing Strategy

The project uses `clojure.test` for unit and integration testing. The agent should be prepared to:

- Write unit tests for new functionality
- Understand integration testing patterns
- Use the existing test framework
- Consider browser-based exports via etapoin (mentioned in readme)

This agent is designed to work seamlessly with the existing Clojure ecosystem and development patterns of the CSB project.