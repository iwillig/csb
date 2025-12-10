(ns csb.ai.types
  "Core type definitions for AI service abstraction.
  
  Uses Typed Clojure for static type checking and documentation.
  All fallible operations return Result<T> = T | Failure."
  (:require
   [failjure.core :as f]
   [typed.clojure :as t]))

;; ============================================================================
;; Railway Result Type
;; ============================================================================

(comment
  "Result type represents operations that can fail.
  
  A Result<T> is either:
  - The success value of type T
  - A Failure value containing error information
  
  This makes failure handling explicit in type signatures."
  
  (t/defalias Result
    (t/All [a]
      (t/U a failjure.core.Failure))))

;; ============================================================================
;; Message Types
;; ============================================================================

(comment
  "Message represents a single message in a conversation."
  
  (t/defalias Message
    (t/HMap :mandatory {:role t/Keyword
                        :content t/Str}
            :optional {:name t/Str})))

(comment
  "Messages is a vector of Message objects."
  
  (t/defalias Messages
    (t/Vec Message)))

;; ============================================================================
;; Request Types
;; ============================================================================

(comment
  "InvokeRequest represents a request to invoke an AI model."
  
  (t/defalias InvokeRequest
    (t/HMap :mandatory {:model t/Str
                        :messages Messages
                        :max-tokens t/Int}
            :optional {:temperature (t/U t/Num nil)
                       :top-p (t/U t/Num nil)
                       :stop-sequences (t/U (t/Vec t/Str) nil)
                       :system (t/U t/Str nil)})))

(comment
  "ValidatedRequest is a Result containing a validated InvokeRequest."
  
  (t/defalias ValidatedRequest
    (Result InvokeRequest)))

;; ============================================================================
;; Response Types
;; ============================================================================

(comment
  "ContentBlock represents a single content block in a response."
  
  (t/defalias ContentBlock
    (t/HMap :mandatory {:type t/Keyword
                        :text t/Str})))

(comment
  "Usage tracks token consumption for a request."
  
  (t/defalias Usage
    (t/HMap :mandatory {:input-tokens t/Int
                        :output-tokens t/Int}
            :optional {:cache-read-tokens t/Int
                       :cache-write-tokens t/Int})))

(comment
  "InvokeResponse represents the response from an AI model invocation."
  
  (t/defalias InvokeResponse
    (t/HMap :mandatory {:content (t/Vec ContentBlock)
                        :model t/Str
                        :usage Usage
                        :stop-reason t/Keyword}
            :optional {:id t/Str})))

(comment
  "ValidatedResponse is a Result containing a validated InvokeResponse."
  
  (t/defalias ValidatedResponse
    (Result InvokeResponse)))

;; ============================================================================
;; Streaming Types
;; ============================================================================

(comment
  "StreamEvent represents a single event in a streaming response."
  
  (t/defalias StreamEvent
    (t/HMap :mandatory {:type t/Keyword}
            :optional {:delta (t/U (t/HMap :mandatory {:type t/Keyword
                                                       :text t/Str}) 
                                   nil)
                       :content-block (t/U ContentBlock nil)
                       :usage (t/U Usage nil)
                       :message (t/U InvokeResponse nil)})))

(comment
  "StreamCallback is a function that handles stream events.
  Returns true to continue streaming, false to stop."
  
  (t/defalias StreamCallback
    [StreamEvent :-> t/Bool]))

(comment
  "StreamState tracks the state of a streaming operation."
  
  (t/defalias StreamState
    (t/HMap :mandatory {:completed t/Bool
                        :stopped-by-callback t/Bool}
            :optional {:accumulated-text t/Str
                       :final-usage (t/U Usage nil)
                       :error (t/U failjure.core.Failure nil)})))

(comment
  "ValidatedStreamEvent is a Result containing a validated StreamEvent."
  
  (t/defalias ValidatedStreamEvent
    (Result StreamEvent)))

;; ============================================================================
;; Model Information Types
;; ============================================================================

(comment
  "ModelCapabilities describes what a model can do."
  
  (t/defalias ModelCapabilities
    (t/Set t/Keyword)))

(comment
  "ModelInfo describes a single AI model."
  
  (t/defalias ModelInfo
    (t/HMap :mandatory {:id t/Str
                        :name t/Str
                        :capabilities ModelCapabilities}
            :optional {:max-tokens (t/U t/Int nil)
                       :supports-streaming (t/U t/Bool nil)
                       :description (t/U t/Str nil)})))

(comment
  "ModelList is a vector of ModelInfo objects."
  
  (t/defalias ModelList
    (t/Vec ModelInfo)))

(comment
  "ValidatedModelList is a Result containing a validated ModelList."
  
  (t/defalias ValidatedModelList
    (Result ModelList)))

;; ============================================================================
;; HTTP Types
;; ============================================================================

(comment
  "HTTPMethod represents HTTP request methods."
  
  (t/defalias HTTPMethod
    (t/U ':get ':post ':put ':delete ':patch)))

(comment
  "HTTPHeaders is a map of header names to values."
  
  (t/defalias HTTPHeaders
    (t/Map t/Str t/Str)))

(comment
  "HTTPRequest represents an HTTP request."
  
  (t/defalias HTTPRequest
    (t/HMap :mandatory {:url t/Str
                        :method HTTPMethod}
            :optional {:headers (t/U HTTPHeaders nil)
                       :body (t/U t/Str nil)
                       :query-params (t/U (t/Map t/Str t/Str) nil)
                       :timeout (t/U t/Int nil)})))

(comment
  "HTTPResponse represents an HTTP response."
  
  (t/defalias HTTPResponse
    (t/HMap :mandatory {:status t/Int
                        :body t/Str}
            :optional {:headers (t/U HTTPHeaders nil)})))

(comment
  "ValidatedHTTPResponse is a Result containing a validated HTTPResponse."
  
  (t/defalias ValidatedHTTPResponse
    (Result HTTPResponse)))

;; ============================================================================
;; OpenAPI Types
;; ============================================================================

(comment
  "OpenAPISpec represents a parsed OpenAPI specification."
  
  (t/defalias OpenAPISpec
    (t/HMap :mandatory {:openapi t/Str
                        :info (t/Map t/Keyword t/Any)
                        :paths (t/Map t/Str t/Any)}
            :optional {:servers (t/U (t/Vec t/Any) nil)
                       :components (t/U (t/Map t/Keyword t/Any) nil)})))

(comment
  "OperationSpec represents a single API operation from OpenAPI."
  
  (t/defalias OperationSpec
    (t/HMap :mandatory {:operationId t/Str
                        :path t/Str
                        :method HTTPMethod}
            :optional {:requestBody (t/U (t/Map t/Keyword t/Any) nil)
                       :responses (t/U (t/Map t/Keyword t/Any) nil)
                       :parameters (t/U (t/Vec t/Any) nil)})))

;; ============================================================================
;; Provider Configuration Types
;; ============================================================================

(comment
  "ProviderConfig represents configuration for an AI provider."
  
  (t/defalias ProviderConfig
    (t/HMap :mandatory {:api-key t/Str
                        :base-url t/Str}
            :optional {:timeout (t/U t/Int nil)
                       :max-retries (t/U t/Int nil)
                       :openapi-spec-url (t/U t/Str nil)})))

(comment
  "ValidatedProviderConfig is a Result containing validated config."
  
  (t/defalias ValidatedProviderConfig
    (Result ProviderConfig)))
