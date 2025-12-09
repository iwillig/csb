(ns csb.ai.schemas
  "Malli schemas for runtime validation of AI service types.
  
  These schemas validate:
  - Request structures (InvokeRequest)
  - Response structures (InvokeResponse)
  - Streaming events (StreamEvent)
  - Model information (ModelInfo)
  - HTTP requests/responses
  - OpenAPI specifications"
  (:require [malli.core :as m]
            [malli.util :as mu]))

;; ============================================================================
;; Message Schemas
;; ============================================================================

(def Message
  "Schema for a single message in a conversation."
  [:map
   [:role {:description "Role of the message sender"}
    [:enum :user :assistant :system]]
   [:content {:description "Text content of the message"}
    string?]
   [:name {:optional true
           :description "Optional name for the message sender"}
    string?]])

(def Messages
  "Schema for a vector of messages."
  [:vector {:description "Sequence of conversation messages"}
   Message])

;; ============================================================================
;; Request Schemas
;; ============================================================================

(def InvokeRequest
  "Schema for AI model invocation requests."
  [:map
   [:model {:description "Model identifier (e.g., 'claude-3-5-sonnet-20241022')"}
    string?]
   [:messages {:description "Conversation messages"}
    Messages]
   [:max-tokens {:description "Maximum tokens to generate"}
    [:and int? [:> 0] [:<= 4096]]]
   [:temperature {:optional true
                  :description "Sampling temperature (0.0 to 1.0)"}
    [:and number? [:>= 0.0] [:<= 1.0]]]
   [:top-p {:optional true
            :description "Nucleus sampling parameter"}
    [:and number? [:>= 0.0] [:<= 1.0]]]
   [:stop-sequences {:optional true
                     :description "Sequences that stop generation"}
    [:vector string?]]
   [:system {:optional true
             :description "System prompt"}
    string?]])

;; ============================================================================
;; Response Schemas
;; ============================================================================

(def ContentBlock
  "Schema for a content block in a response."
  [:map
   [:type {:description "Content block type"}
    [:enum :text]]
   [:text {:description "Text content"}
    string?]])

(def Usage
  "Schema for token usage information."
  [:map
   [:input-tokens {:description "Number of input tokens"}
    [:and int? [:>= 0]]]
   [:output-tokens {:description "Number of output tokens"}
    [:and int? [:>= 0]]]
   [:cache-read-tokens {:optional true
                        :description "Tokens read from cache"}
    [:and int? [:>= 0]]]
   [:cache-write-tokens {:optional true
                         :description "Tokens written to cache"}
    [:and int? [:>= 0]]]])

(def InvokeResponse
  "Schema for AI model invocation responses."
  [:map
   [:content {:description "Response content blocks"}
    [:vector ContentBlock]]
   [:model {:description "Model that generated the response"}
    string?]
   [:usage {:description "Token usage information"}
    Usage]
   [:stop-reason {:description "Why generation stopped"}
    [:enum :end-turn :max-tokens :stop-sequence]]
   [:id {:optional true
         :description "Unique response identifier"}
    string?]])

;; ============================================================================
;; Streaming Schemas
;; ============================================================================

(def StreamDelta
  "Schema for a streaming delta."
  [:map
   [:type {:description "Delta type"}
    [:enum :text-delta]]
   [:text {:description "Delta text"}
    string?]])

(def StreamEvent
  "Schema for server-sent streaming events."
  [:map
   [:type {:description "Event type"}
    [:enum :message-start :content-block-start :content-block-delta
     :content-block-stop :message-delta :message-stop :ping :error]]
   [:delta {:optional true
            :description "Delta information for content updates"}
    StreamDelta]
   [:content-block {:optional true
                    :description "Content block for start events"}
    ContentBlock]
   [:usage {:optional true
            :description "Token usage information"}
    Usage]
   [:message {:optional true
              :description "Complete message for message-start"}
    InvokeResponse]
   [:error {:optional true
            :description "Error information for error events"}
    [:map
     [:type string?]
     [:message string?]]]])

;; ============================================================================
;; Model Information Schemas
;; ============================================================================

(def ModelCapabilities
  "Schema for model capabilities."
  [:set {:description "Set of capability keywords"}
   [:enum :text :vision :function-calling :json-mode :streaming]])

(def ModelInfo
  "Schema for AI model information."
  [:map
   [:id {:description "Unique model identifier"}
    string?]
   [:name {:description "Human-readable model name"}
    string?]
   [:capabilities {:description "Model capabilities"}
    ModelCapabilities]
   [:max-tokens {:optional true
                 :description "Maximum context window"}
    [:and int? [:> 0]]]
   [:supports-streaming {:optional true
                         :description "Whether model supports streaming"}
    boolean?]
   [:description {:optional true
                  :description "Model description"}
    string?]])

(def ModelList
  "Schema for a list of models."
  [:vector {:description "List of available models"}
   ModelInfo])

;; ============================================================================
;; HTTP Schemas
;; ============================================================================

(def HTTPMethod
  "Schema for HTTP methods."
  [:enum :get :post :put :delete :patch :head :options])

(def HTTPHeaders
  "Schema for HTTP headers."
  [:map-of {:description "HTTP header name-value pairs"}
   string? string?])

(def HTTPRequest
  "Schema for HTTP requests."
  [:map
   [:url {:description "Request URL"}
    string?]
   [:method {:description "HTTP method"}
    HTTPMethod]
   [:headers {:optional true
              :description "Request headers"}
    HTTPHeaders]
   [:body {:optional true
           :description "Request body"}
    string?]
   [:query-params {:optional true
                   :description "URL query parameters"}
    [:map-of string? string?]]
   [:timeout {:optional true
              :description "Request timeout in milliseconds"}
    [:and int? [:> 0]]]])

(def HTTPResponse
  "Schema for HTTP responses."
  [:map
   [:status {:description "HTTP status code"}
    [:and int? [:>= 100] [:< 600]]]
   [:body {:description "Response body"}
    string?]
   [:headers {:optional true
              :description "Response headers"}
    HTTPHeaders]])

;; ============================================================================
;; OpenAPI Schemas
;; ============================================================================

(def OpenAPIInfo
  "Schema for OpenAPI info object."
  [:map
   [:title string?]
   [:version string?]
   [:description {:optional true} string?]])

(def OpenAPIServer
  "Schema for OpenAPI server object."
  [:map
   [:url string?]
   [:description {:optional true} string?]])

(def OpenAPISpec
  "Schema for OpenAPI specification (simplified)."
  [:map
   [:openapi {:description "OpenAPI version"}
    string?]
   [:info {:description "API information"}
    OpenAPIInfo]
   [:paths {:description "API paths"}
    [:map-of string? any?]]
   [:servers {:optional true
              :description "API servers"}
    [:vector OpenAPIServer]]
   [:components {:optional true
                 :description "Reusable components"}
    [:map-of keyword? any?]]])

;; ============================================================================
;; Provider Configuration Schemas
;; ============================================================================

(def ProviderConfig
  "Schema for AI provider configuration."
  [:map
   [:api-key {:description "API authentication key"}
    [:and string? [:min-length 1]]]
   [:base-url {:description "Base URL for API requests"}
    [:and string? [:re #"^https?://.*"]]]
   [:timeout {:optional true
              :description "Default request timeout in milliseconds"}
    [:and int? [:> 0] [:<= 300000]]]
   [:max-retries {:optional true
                  :description "Maximum retry attempts for failed requests"}
    [:and int? [:>= 0] [:<= 10]]]
   [:openapi-spec-url {:optional true
                       :description "URL to OpenAPI specification"}
    string?]])

;; ============================================================================
;; Schema Registry
;; ============================================================================

(def schemas
  "Registry of all schemas for easy lookup."
  {:message Message
   :messages Messages
   :invoke-request InvokeRequest
   :content-block ContentBlock
   :usage Usage
   :invoke-response InvokeResponse
   :stream-delta StreamDelta
   :stream-event StreamEvent
   :model-capabilities ModelCapabilities
   :model-info ModelInfo
   :model-list ModelList
   :http-method HTTPMethod
   :http-headers HTTPHeaders
   :http-request HTTPRequest
   :http-response HTTPResponse
   :openapi-info OpenAPIInfo
   :openapi-server OpenAPIServer
   :openapi-spec OpenAPISpec
   :provider-config ProviderConfig})

(defn get-schema
  "Get a schema by keyword identifier.
  
  Example:
    (get-schema :invoke-request)
    => InvokeRequest schema"
  [schema-id]
  (get schemas schema-id))
