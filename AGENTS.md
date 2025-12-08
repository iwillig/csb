# CSB Agents

This document describes the LLM agents for the Clojure Software
Builder (CSB) project.

## Overview

- **Typed Clojure** for static verification
- **SQLite** for primary storage with JSON and full-text capabilities
- **Datastar** for the primary UI framework
- **Hiccup** for HTML generation
- **HTTP-Kit** for HTTP server capabilities
- **rewrite-clj** for Clojure parsing


### cljn-nrepl-eval

Runs clojure code

```shell
## Get command help
clj-nrepl-eval --help

## Discover ports
clj-nrepl-eval --discover-ports

### Run Clojure expression
clj-nrepl-eval -p 7889 "*ns*"
```

**Evaluate code:**

`clj-nrepl-eval -p <port> "<clojure-code>"`

With timeout (milliseconds)

`clj-nrepl-eval -p <port> --timeout 5000 "<clojure-code>"`

## clj-paren-repair

Attempts to fix parenedithis errors automatically

```shell
## Get Help
clj-paren-repair --help

## File a namespace
clj-paren-repair src/csb/db.clj
```

## Development flow

LLM Agents should follow a similar pattern

1. Work with user to understand the user request
2. Review the existing code with the read llm tool
3. Come up with a plan to implement the user request, share plan with user.
4. If plan is approved, use the edit LLM tool to make changes to the code
5. Use the `Lint` command to check changes
6.
7. Use the `Type-Check` command to type check `src/csb`
8. Use the `Test` Command to check changes

### Check current namespace


```shell
clj-nrepl-eval -p 7889 "*ns*"

clj-nrepl-eval -p 7889 "(fast-dev)"
```

## Lint changes

If not in dev namespace `clj-nrepl-eval -p 7889 "(fast-dev)"`

```shell
clj-nrepl-eval -p 7889 "(lint)"
```

## Type check

Uses typed-clojure to check the `src` for type correctness.

If not in dev namespace `clj-nrepl-eval -p 7889 "(fast-dev)"`

```shell
clj-nrepl-eval -p 7889 "(type-check)"
```


## Test

Uses Kaocha on the repl to run tests.

If not in dev namespace `clj-nrepl-eval -p 7889 "(fast-dev)"`


```shell
## Run all the tests in the project
clj-nrepl-eval -p 7889 "(k/run-all)"
## Run a Speific Test
clj-nrepl-eval -p 7889 "(k/run 'csb.db.conversation-test)"
## Run
clj-nrepl-eval -p 7889 "(k/run 'csb.db.conversation-test/test-create-conversation-with-required-fields)"
```
