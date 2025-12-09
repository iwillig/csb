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

## Important: Shell Command Usage in OpenCode

**All shell commands in OpenCode require a `description` parameter.** This is mandatory for the bash tool.

Example:
```shell
clj-nrepl-eval -p 7889 "*ns*"
# DESCRIPTION: Check current namespace
```

When using the bash tool programmatically, always include the description:
```
bash(command="clj-nrepl-eval -p 7889 '(type-check)'", description="Type check the CSB project")
```

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

## clojure-lsp

Clojure Language Server Protocol (LSP) command-line tool for code analysis, formatting, and refactoring.

### Common Options

All commands support these options:

- `--project-root PATH` - Specify the project root path (default: current directory)
- `--namespace NS` - Target specific namespace(s), can be specified multiple times
- `--filenames FILENAMES` - Target specific files (comma or double-colon separated)
- `--ns-exclude-regex REGEX` - Exclude namespaces matching regex
- `--dry` - Make no changes, only report what would be done
- `--raw` - Print only essential data (minimal output)
- `--verbose` - Use stdout for logs
- `--settings SETTINGS` - Provide settings as EDN (see https://clojure-lsp.io/settings)

### diagnostics - Find Code Issues

Analyze the project and report all warnings, errors, and linting issues.

```shell
## Check entire project
clojure-lsp diagnostics --project-root .

## Check with minimal output
clojure-lsp diagnostics --raw --project-root .

## Check specific namespace
clojure-lsp diagnostics --namespace csb.db --project-root .

## Check specific files
clojure-lsp diagnostics --filenames src/csb/db.clj,src/csb/parser.clj --project-root .

## Exclude test namespaces
clojure-lsp diagnostics --ns-exclude-regex ".*-test$" --project-root .
```

**Output format:**
```
src/csb/db/file_change.clj:126:7: info: [clojure-lsp/unused-public-var] Unused public var 'csb.db.file-change/create-file-change'
```

**Use when:**
- After making code changes to check for issues
- Before committing to catch unused vars, wrong arities, etc.
- To find all project-wide problems

### clean-ns - Organize Namespace Forms

Organize ns forms by removing unused requires/refers/imports and sorting alphabetically.

```shell
## Clean all namespaces (dry run)
clojure-lsp clean-ns --dry --project-root .

## Clean specific namespace
clojure-lsp clean-ns --namespace csb.db --project-root .

## Clean all namespaces (apply changes)
clojure-lsp clean-ns --project-root .

## Clean multiple namespaces
clojure-lsp clean-ns --namespace csb.db --namespace csb.parser --project-root .

## Clean excluding tests
clojure-lsp clean-ns --ns-exclude-regex ".*-test$" --project-root .
```

**What it does:**
- Removes unused `:require`, `:refer`, and `:import` entries
- Sorts requires alphabetically
- Organizes ns form consistently

**Use when:**
- After refactoring to remove unused dependencies
- Before committing to ensure clean namespace declarations
- To standardize namespace formatting across project

**Tip:** Always run with `--dry` first to preview changes

### format - Format Code

Format Clojure code using cljfmt rules.

```shell
## Format entire project (dry run)
clojure-lsp format --dry --project-root .

## Format specific file
clojure-lsp format --filenames src/csb/db.clj --project-root .

## Format specific namespace
clojure-lsp format --namespace csb.db --project-root .

## Format and apply changes
clojure-lsp format --project-root .

## Format with custom settings
clojure-lsp format --settings '{:cljfmt {:indents {my-macro [[:inner 0]]}}}' --project-root .
```

**Output (dry run):**
```diff
--- a/src/csb/db.clj
+++ b/src/csb/db.clj
@@ -113,10 +113,10 @@
       (ragtime-repl/rollback config))))
 
 (t/ann ^:no-check execute-many
-        (t/All [a] [SQLiteConnection (t/Map t/Keyword t/Any) :-> (t/Seqable a)]))
+       (t/All [a] [SQLiteConnection (t/Map t/Keyword t/Any) :-> (t/Seqable a)]))
```

**Use when:**
- Before committing to ensure consistent code style
- After manual edits that may have inconsistent indentation
- To standardize formatting across the project

### rename - Rename Symbols and Namespaces

Safely rename symbols or namespaces and update all references across the project.

```shell
## Rename a function
clojure-lsp rename --from csb.db/old-function --to csb.db/new-function --project-root .

## Rename a namespace (also renames file)
clojure-lsp rename --from csb.old-namespace --to csb.new-namespace --project-root .

## Dry run to see what would change
clojure-lsp rename --from csb.db/old-function --to csb.db/new-function --dry --project-root .
```

**What it does:**
- Renames the symbol or namespace
- Updates all references across the project
- For namespace renames, also moves/renames the source file

**Use when:**
- Refactoring function or variable names
- Restructuring namespace organization
- Need to ensure all references are updated consistently

**Important:** Always use `--dry` first to review changes

### dump - Export Project Data

Export comprehensive project data including classpath, source paths, dependency graph, and analysis data.

```shell
## Dump all project data
clojure-lsp dump --project-root .

## Dump with raw output
clojure-lsp dump --raw --project-root .

## Dump with specific output options
clojure-lsp dump --output '{:canonical-paths true}' --project-root .
```

**Output includes:**
- Source paths and classpaths
- Dependency graph
- clj-kondo analysis data
- Project structure

**Use when:**
- Debugging project configuration
- Analyzing project structure
- Building tooling that needs project metadata
- Understanding dependency relationships

### Integration with Development Workflow

```shell
## Step 1: Check for issues before starting
clojure-lsp diagnostics --raw --project-root .

## Step 2: Make code changes using edit tools
# ... make changes ...

## Step 3: Clean up namespace declarations
clojure-lsp clean-ns --dry --namespace csb.db --project-root .
clojure-lsp clean-ns --namespace csb.db --project-root .

## Step 4: Format code
clojure-lsp format --dry --namespace csb.db --project-root .
clojure-lsp format --namespace csb.db --project-root .

## Step 5: Check for new issues
clojure-lsp diagnostics --raw --namespace csb.db --project-root .

## Step 6: Run tests (using clj-nrepl-eval)
clj-nrepl-eval -p 7889 "(k/run 'csb.db-test)"
```

### Best Practices

1. **Always use `--dry` first** - Preview changes before applying them
2. **Use `--raw` for scripts** - Minimal output is easier to parse
3. **Target specific namespaces** - Faster than whole-project operations
4. **Check diagnostics regularly** - Catch issues early
5. **Clean before formatting** - Remove unused code before formatting
6. **Use with version control** - Easy to review changes in git diff

### Common Error Patterns

**Analysis failures:**
- Ensure deps.edn or project.clj is valid
- Check that all dependencies are available
- Verify project-root points to correct directory

**Format/clean-ns issues:**
- Some code may not be auto-fixable
- Review dry-run output before applying
- Custom cljfmt rules may conflict

**Performance:**
- First run analyzes entire project (slow)
- Subsequent runs use cache (faster)
- Target specific namespaces for quick iterations

## Development flow

LLM Agents should follow a similar pattern

1. Work with user to understand the user request
2. Review the existing code with the read llm tool
3. Come up with a plan to implement the user request, share plan with user.
4. If plan is approved, use the edit LLM tool to make changes to the code
5. Use `clojure-lsp diagnostics` to check for code issues
6. Use `clojure-lsp clean-ns` to organize namespace declarations
7. Use `clojure-lsp format` to format code consistently
8. Use the `Type-Check` command to type check `src/csb`
9. Use the `Test` Command to check changes

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
