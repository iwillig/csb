(ns csb.ai.railway
  "Railway-Oriented Programming utilities for AI service.
  
  Provides validation functions that bridge Malli schemas to Failjure Results.
  All validation functions return Result<T> - either the validated value or a Failure."
  (:require [failjure.core :as f]
            [malli.core :as m]
            [malli.error :as me]
            [csb.ai.schemas :as schemas]))

;; ============================================================================
;; Core Railway Validation
;; ============================================================================

(defn validate-with-schema
  "Validate data against a Malli schema, returning Result<T>.
  
  If validation succeeds, returns data unchanged.
  If validation fails, returns Failure with humanized error message.
  
  Args:
    schema - Malli schema to validate against
    data   - Data to validate
  
  Returns:
    Result<T> - Either the validated data or a Failure
  
  Example:
    (validate-with-schema schemas/InvokeRequest request)
    => request (if valid)
    => #Failure{:message \"Validation failed: ...\"} (if invalid)"
  [schema data]
  (if (m/validate schema data)
    data
    (f/fail "Validation failed: %s"
            (me/humanize (m/explain schema data)))))

;; ============================================================================
;; Request Validation
;; ============================================================================

(defn validate-invoke-request
  "Validate an InvokeRequest, returning Result<InvokeRequest>.
  
  Checks:
  - Required fields present (model, messages, max-tokens)
  - Field types correct
  - Value constraints (max-tokens > 0, temperature in range, etc.)
  
  Args:
    request - Map representing an invoke request
  
  Returns:
    Result<InvokeRequest> - Validated request or Failure"
  [request]
  (validate-with-schema schemas/InvokeRequest request))

(defn validate-message
  "Validate a single message, returning Result<Message>.
  
  Args:
    message - Map representing a message
  
  Returns:
    Result<Message> - Validated message or Failure"
  [message]
  (validate-with-schema schemas/Message message))

(defn validate-messages
  "Validate a vector of messages, returning Result<Messages>.
  
  Args:
    messages - Vector of message maps
  
  Returns:
    Result<Messages> - Validated messages or Failure"
  [messages]
  (validate-with-schema schemas/Messages messages))

;; ============================================================================
;; Response Validation
;; ============================================================================

(defn validate-invoke-response
  "Validate an InvokeResponse, returning Result<InvokeResponse>.
  
  Checks:
  - Required fields present (content, model, usage, stop-reason)
  - Field types correct
  - Content blocks valid
  - Usage counts non-negative
  
  Args:
    response - Map representing an invoke response
  
  Returns:
    Result<InvokeResponse> - Validated response or Failure"
  [response]
  (validate-with-schema schemas/InvokeResponse response))

(defn validate-content-block
  "Validate a content block, returning Result<ContentBlock>.
  
  Args:
    content-block - Map representing a content block
  
  Returns:
    Result<ContentBlock> - Validated content block or Failure"
  [content-block]
  (validate-with-schema schemas/ContentBlock content-block))

(defn validate-usage
  "Validate usage information, returning Result<Usage>.
  
  Args:
    usage - Map representing token usage
  
  Returns:
    Result<Usage> - Validated usage or Failure"
  [usage]
  (validate-with-schema schemas/Usage usage))

;; ============================================================================
;; Streaming Validation
;; ============================================================================

(defn validate-stream-event
  "Validate a streaming event, returning Result<StreamEvent>.
  
  Checks:
  - Event type is valid
  - Optional fields (delta, content-block, usage) valid if present
  - Error events have required error information
  
  Args:
    event - Map representing a stream event
  
  Returns:
    Result<StreamEvent> - Validated event or Failure"
  [event]
  (validate-with-schema schemas/StreamEvent event))

;; ============================================================================
;; Model Information Validation
;; ============================================================================

(defn validate-model-info
  "Validate model information, returning Result<ModelInfo>.
  
  Checks:
  - Required fields present (id, name, capabilities)
  - Capabilities is a valid set
  - Optional fields valid if present
  
  Args:
    model-info - Map representing model information
  
  Returns:
    Result<ModelInfo> - Validated model info or Failure"
  [model-info]
  (validate-with-schema schemas/ModelInfo model-info))

(defn validate-model-list
  "Validate a list of models, returning Result<ModelList>.
  
  Args:
    models - Vector of model info maps
  
  Returns:
    Result<ModelList> - Validated model list or Failure"
  [models]
  (validate-with-schema schemas/ModelList models))

;; ============================================================================
;; HTTP Validation
;; ============================================================================

(defn validate-http-request
  "Validate an HTTP request, returning Result<HTTPRequest>.
  
  Checks:
  - URL and method present
  - Method is valid HTTP method
  - Optional fields valid if present
  
  Args:
    http-request - Map representing an HTTP request
  
  Returns:
    Result<HTTPRequest> - Validated request or Failure"
  [http-request]
  (validate-with-schema schemas/HTTPRequest http-request))

(defn validate-http-response
  "Validate an HTTP response, returning Result<HTTPResponse>.
  
  Checks:
  - Status code in valid range (100-599)
  - Body is string
  - Headers valid if present
  
  Args:
    http-response - Map representing an HTTP response
  
  Returns:
    Result<HTTPResponse> - Validated response or Failure"
  [http-response]
  (validate-with-schema schemas/HTTPResponse http-response))

;; ============================================================================
;; OpenAPI Validation
;; ============================================================================

(defn validate-openapi-spec
  "Validate an OpenAPI specification, returning Result<OpenAPISpec>.
  
  Checks:
  - OpenAPI version present
  - Info and paths present
  - Structure matches OpenAPI format
  
  Args:
    spec - Map representing an OpenAPI specification
  
  Returns:
    Result<OpenAPISpec> - Validated spec or Failure"
  [spec]
  (validate-with-schema schemas/OpenAPISpec spec))

;; ============================================================================
;; Provider Configuration Validation
;; ============================================================================

(defn validate-provider-config
  "Validate provider configuration, returning Result<ProviderConfig>.
  
  Checks:
  - api-key present and non-empty
  - base-url present and valid URL
  - Optional timeout and max-retries in valid ranges
  
  Args:
    config - Map representing provider configuration
  
  Returns:
    Result<ProviderConfig> - Validated config or Failure"
  [config]
  (validate-with-schema schemas/ProviderConfig config))

;; ============================================================================
;; Collection Operations
;; ============================================================================

(defn collect-results
  "Collect multiple Results into a single Result.
  
  If all Results are successful, returns a vector of values.
  If any Result is a Failure, returns the first Failure (short-circuit).
  
  This is useful for operations like list-models where you want to validate
  multiple items but stop on the first error.
  
  Args:
    results - Sequence of Result values
  
  Returns:
    Result<Vec<T>> - Vector of success values or first Failure
  
  Example:
    (collect-results [(validate-model model1)
                      (validate-model model2)
                      (validate-model model3)])
    => [model1 model2 model3] (if all valid)
    => #Failure{...} (if any invalid)"
  [results]
  (reduce
    (fn [acc result]
      (if (f/failed? result)
        (reduced result)  ; Short-circuit on first failure
        (conj acc result)))
    []
    results))

;; ============================================================================
;; Conditional Validation
;; ============================================================================

(defn validate-if-present
  "Validate a value if it's present (not nil), otherwise return nil.
  
  Useful for optional fields that need validation only when provided.
  
  Args:
    schema - Malli schema to validate against
    value  - Value to validate (may be nil)
  
  Returns:
    Result<T | nil> - Validated value, nil, or Failure
  
  Example:
    (validate-if-present schemas/HTTPHeaders headers)
    => headers (if present and valid)
    => nil (if not present)
    => #Failure{...} (if present but invalid)"
  [schema value]
  (if (nil? value)
    nil
    (validate-with-schema schema value)))

;; ============================================================================
;; Validation Helpers
;; ============================================================================

(defn validation-error?
  "Check if a failure is specifically a validation error.
  
  Args:
    failure - A Failure value
  
  Returns:
    Boolean - True if failure message starts with 'Validation failed'"
  [failure]
  (and (f/failed? failure)
       (-> failure f/message (.startsWith "Validation failed"))))

(defn extract-validation-errors
  "Extract the validation error details from a validation failure.
  
  Args:
    failure - A validation Failure
  
  Returns:
    String - The validation error details (the part after 'Validation failed: ')"
  [failure]
  (when (validation-error? failure)
    (-> failure
        f/message
        (subs (count "Validation failed: ")))))
