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
