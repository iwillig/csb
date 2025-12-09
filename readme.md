# Clojure Software Builder

This tool is designed to help you get the most out of working with LLM
agents and the clojure programming language.

## Techonology

- Clojure: Core techonology
- TypedClojure: Static verification of Clojure
- SQLite: Primary Storage. JSON and Full text storage
- Datastar: Primary UI
- Hiccup: Generate HTML
- HTTP-Kit: HTTP Server
- rewrite-clj: CLojure parser

## Data Model

### Schema Overview

```mermaid
erDiagram
    %% Core Entities
    project ||--o{ plan : "has"
    project ||--o{ file : "contains"
    project ||--o{ conversation : "has"
    
    plan_state ||--o{ plan : "defines state"
    
    plan ||--o{ task : "contains"
    plan ||--o{ file_change : "proposes"
    plan ||--o{ error : "tracks"
    plan ||--o{ plan_outcome : "produces"
    plan }o--o{ plan_skill : "uses"
    
    task ||--o{ task : "has subtasks"
    
    file ||--o{ file_content : "has versions"
    file ||--o{ file_change : "modified by"
    
    file_change ||--o{ file_change_change : "has line changes"
    file_change ||--o{ file_change_ast : "has AST changes"
    file_change ||--o{ file_change_application : "tracks application"
    
    conversation ||--o{ message : "contains"
    conversation }o--o{ plan_skill : "references"
    conversation }o--o{ plan : "discusses"
    conversation }o--o{ task : "mentions"
    conversation }o--o{ file_change : "proposes"
    
    %% Entity Definitions
    project {
        INTEGER id PK
        TEXT name
        TEXT description
        TEXT path UK
        TEXT created_at
        TEXT updated_at
    }
    
    plan_state {
        TEXT id PK "created|researched|file-change-created|..."
    }
    
    plan {
        INTEGER id PK
        INTEGER project_id FK
        TEXT name
        TEXT context "FTS indexed"
        TEXT plan_state_id FK
        TEXT created_at
        TEXT updated_at
    }
    
    task {
        INTEGER id PK
        INTEGER plan_id FK
        INTEGER parent_id FK "self-reference"
        TEXT name
        TEXT context "FTS indexed"
        BOOLEAN completed
        TEXT created_at
        TEXT updated_at
    }
    
    plan_skill {
        INTEGER id PK
        TEXT name
        TEXT description
        TEXT content "FTS indexed"
        TEXT created_at
        TEXT updated_at
    }
    
    file {
        INTEGER id PK
        INTEGER project_id FK
        TEXT path
        TEXT summary
        TEXT created_at
        TEXT updated_at
    }
    
    file_content {
        INTEGER id PK
        INTEGER file_id FK
        TEXT content
        TEXT compact_ast "JSON"
        TEXT parsed_at
        TEXT created_at
        TEXT updated_at
    }
    
    file_change {
        INTEGER id PK
        INTEGER plan_id FK
        INTEGER file_id FK
        TEXT created_at
        TEXT updated_at
    }
    
    file_change_change {
        INTEGER id PK
        INTEGER file_change_id FK
        TEXT change_type "addition|removal|update"
        INTEGER line_start
        INTEGER line_end
        TEXT change_content
        TEXT created_at
        TEXT updated_at
    }
    
    file_change_ast {
        INTEGER id PK
        INTEGER file_change_id FK
        TEXT node_path
        TEXT node_tag
        TEXT node_string
        TEXT node_compact_ast "JSON"
        TEXT change_type "addition|removal|update"
        TEXT created_at
        TEXT updated_at
    }
    
    file_change_application {
        INTEGER id PK
        INTEGER file_change_id FK
        INTEGER plan_id FK
        TEXT status "pending|applied|failed"
        TEXT result_message
        TEXT applied_at
        TEXT created_at
        TEXT updated_at
    }
    
    error {
        INTEGER id PK
        INTEGER plan_id FK
        TEXT message "FTS indexed"
        TEXT stack_trace
        TEXT created_at
        TEXT updated_at
    }
    
    plan_outcome {
        INTEGER id PK
        INTEGER plan_id FK
        TEXT outcome
        TEXT created_at
        TEXT updated_at
    }
    
    conversation {
        INTEGER id PK
        INTEGER project_id FK
        TEXT name
        TEXT created_at
        TEXT updated_at
    }
    
    message {
        INTEGER id PK
        INTEGER conversation_id FK
        TEXT role "user|assistant"
        TEXT content "FTS indexed"
        TEXT created_at
        TEXT updated_at
    }
```

### Key Features

- **Foreign Keys**: Enabled globally with CASCADE deletes
- **Full-Text Search (FTS5)**: On plan.context, task.context, plan_skill.content, error.message, message.content
- **Timestamps**: Automatic created_at/updated_at on all tables
- **Indexes**: Comprehensive indexing on all foreign keys and frequently queried columns
- **Constraints**: CHECK constraints on enums (role, change_type, status)
- **Versioning**: file_content supports multiple versions per file


## Core features

Command line tool

- skill
- plan
- task
- file
- file-change

## Development

This is a REPL driven project. Most of the important development
operations are exposed in in the dev.clj.

- dev/refresh: Reloads the clojure namespaces
- dev/lint: Uses clj-kondo to lint the namespaces
- dev/type-check: Uses typed Clojure to type check the code in `src`
- dev/migrate: Migrates the database and generates a test.db file.

## Testing

- clojure.test for unit and integrational style tests
- Use etapoin for browswer based exportations of the UI.
