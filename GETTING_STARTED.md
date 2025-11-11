# Getting Started

This guide is for developers working in the app component (where the SDK is used) rather than the internal-core (framework internals). It explains how to set up a local environment, choose between ActiveMQ and the internal in-memory queue, define and wire agents via `agent.yml`, implement handlers, trigger flows, and control chaining or termination of agent flows.

---

## 1) Prerequisites

- Java 21+
- Gradle (or use the included Gradle wrapper)
- Docker (optional, recommended for running ActiveMQ and the full stack)

---

## 2) Configuration Overview

- App-specific overrides: `app/src/main/resources/application.yml`
  - Activates the "internal" profile and sets application-level overrides
- Framework defaults: `internal-core/src/main/resources/application-internal.yml`
  - Messaging provider (ActiveMQ or local), queues, server port, metrics, logging, etc.
- Agent configuration file: `qodo.agent.configFile`
  - Default: `classpath:agent.yml`
  - Override via env var: `QODO_AGENT_CONFIG_FILE=file:/absolute/path/to/agent.yml`

---

## 3) Quick Start: Local Development (Internal In-Memory Queue)

The fastest way to develop is using the internal in-memory queue implementation.

Environment:
- `MESSAGING_PROVIDER=local`
- Optional local queue tuning (defaults are robust for dev):
  - `LOCAL_QUEUE_CAPACITY=1000`
  - `LOCAL_CONSUMER_THREADS=1` (preserves ordering)
  - `LOCAL_RETRY_ATTEMPTS=3`, `LOCAL_RETRY_DELAY_MS=1000`, `LOCAL_MAX_RETRY_DELAY_MS=30000`
  - `LOCAL_POLL_TIMEOUT_SECONDS=5`, `LOCAL_EXPONENTIAL_BACKOFF=true`
- Optionally set `JMS_HEALTH_ENABLED=false` when not using JMS

Run:
- `./gradlew bootRun`

Health endpoint (default port 8080):
- http://localhost:8080/actuator/health

---

## 4) Robust Setup: ActiveMQ Broker (Production-like)

Use the Docker Compose stack to run Apache ActiveMQ plus the app and monitoring tools.

Start with Docker Compose:
- `cd docker`
- `docker compose up --build`

What it runs:
- ActiveMQ broker
  - Ports: 61616 (JMS), 8161 (web console)
  - Console: http://localhost:8161 (defaults to admin/admin via compose env)
- App container
  - Default exposed port: 8081 (mapped to container 8081)
  - Environment automatically set for ActiveMQ:
    - `MESSAGING_PROVIDER=activemq`
    - `MESSAGING_ACTIVEMQ_BROKER_URL=tcp://activemq:61616`
    - `MESSAGING_ACTIVEMQ_USERNAME=qodo`, `MESSAGING_ACTIVEMQ_PASSWORD=qodo`
    - `JMS_HEALTH_ENABLED=true`
- Monitoring (optional): Prometheus (9090), Grafana (3000)

App health in compose:
- http://localhost:8081/actuator/health

---

## 5) Switching Queue Backends

Choose the queue backend via `messaging.provider` or `MESSAGING_PROVIDER` env:

- ActiveMQ (robust):
  - `MESSAGING_PROVIDER=activemq`
  - Provide `MESSAGING_ACTIVEMQ_BROKER_URL`, credentials
  - `JMS_HEALTH_ENABLED=true`

- Local in-memory queue (simple/fast):
  - `MESSAGING_PROVIDER=local`
  - Optionally disable JMS health: `JMS_HEALTH_ENABLED=false`
  - Tune local queue behavior with `LOCAL_*` settings

Queue/topic names (overridable):
- `messaging.queue.event` (default: `event`)
- `messaging.queue.response` (default: `response`)
- `messaging.queue.audit` (default: `audit`)

Override via:
- `MESSAGING_EVENT_TOPIC`, `MESSAGING_RESPONSE_TOPIC`, `MESSAGING_DEFAULT_TOPIC`

---

## 6) Defining Agents via agent.yml

Where `agent.yml` lives:
- Default: `classpath:agent.yml`
- Override path:
  - `export QODO_AGENT_CONFIG_FILE=file:/absolute/path/to/agent.yml`

Agent YAML structure (high-level):
- `version`, `system_prompt`
- `commands`: map of commandName → configuration
  - `description`, `instructions`, `model`
  - `mcpServers` (optional): defines MCP processes (command, args, env)
  - `tools`, `execution_strategy`
  - `output_schema` (JSON schema string)
  - `exit_expression`: typically `success` (boolean)

How commands map to handlers:
- The command name in `agent.yml` must map to a Spring bean named `{command}-handler`.
- Example:
  - A command `jira_agent` requires a bean named `jira_agent-handler`.
  - In code: `@Service("jira_agent-handler")`.

MCP client initialization:
- MCP servers are read from `agent.yml` and initialized at runtime for each command.

---

## 7) Implementing Handlers in the App

Base interface and base class:
- `Handler` (internal-core):
  - `void handle(CommandSession commandSession, List<TaskResponse> allTaskResponses)`
- `BaseHandler` (recommended):
  - You extend `BaseHandler` to leverage parsing, success routing, and publishing.
  - Constructor: `(MessagePublisher messagePublisher, ObjectMapper objectMapper)`
  - Implement:
    - `public String type()` → the next step when success=true (or `end_node` to end)
    - `public Map<String,Object> handle(Map<String,Object> map)` → mutate/enrich payload before re-enqueue

Bean naming:
- Annotate your handler with `@Service("{command}-handler")` and `@Scope("prototype")`.

Examples in app:
- `JiraAgentHandler` → `@Service("jira_agent-handler")` → `type()` returns `"coding_agent"` to chain next.
- `CodingAgentHandler` → `@Service("coding_agent-handler")` → `type()` returns `EndFlowCleanup.TYPE` (`"end_node"`) to terminate.

Routing on success vs failure:
- `BaseHandler` consolidates structured LLM output and checks `success` from the agent’s `exit_expression`.
  - If `success=true` → `map["type"] = this.type()` (your handler controls next step)
  - If `success=false` or schema parse fails → routes to `end_node` or `incomplete-service` depending on context
- The final map is serialized and published to the response queue via `MessagePublisher.publishResponse(...)`.

Terminating services:
- `EndFlowCleanup` is provided as `@Service("end_node-service")` and acts as the default end node.

---

## 8) Starting Agents from Events

Typical triggers:
- Webhooks (e.g., Jira, Snyk) handled by app controllers under `app/.../controllers`.
  - Example: `JiraWebhookController` validates and transforms webhooks into internal messages.
- Direct queue messages (advanced):
  - Publish a message to the `event` topic with a proper `CommandSession` payload.
- CommandTriggers (from core) include `webhooks`, `cron`, and `file_watch` (see `internal-core/.../api` records). If you model these in `agent.yml`, ensure your app layer emits the appropriate messages.

Local test with webhooks:
- Start the app (local queue or ActiveMQ)
- POST a test webhook payload to the app’s exposed webhook endpoint
- The app enqueues a `CommandSession` and the agent defined in `agent.yml` executes

---

## 9) Flow Control: Chain Next Agent or End

- To chain to another agent: return the next command name from your handler’s `type()` when `success=true`.
- To end the flow: return `EndFlowCleanup.TYPE` (i.e., `"end_node"`) from `type()` or ensure `success=false`.
- For incomplete/validation failure paths, the framework can route to `"incomplete-service"`.

Concrete examples:
- `jira_agent` → `JiraAgentHandler.type()` returns `"coding_agent"` when success=true
- `coding_agent` → `CodingAgentHandler.type()` returns `"end_node"` to terminate

---

## 10) Running and Verifying

Local queue mode:
- `export MESSAGING_PROVIDER=local`
- `export QODO_AGENT_CONFIG_FILE=classpath:agent.yml`
- `./gradlew bootRun`
- Health: http://localhost:8080/actuator/health

ActiveMQ mode (compose):
- `cd docker`
- `docker compose up --build`
- App health: http://localhost:8081/actuator/health
- ActiveMQ console: http://localhost:8161 (admin/admin)

Trigger flows:
- Send a webhook to the app controller (e.g., Jira) to start `jira_agent`.

---

## 11) Environment Variables Reference (Selected)

General:
- `QODO_AGENT_CONFIG_FILE` (default: `classpath:agent.yml`)
- `COMMAND_BASE_URL`, `WEBSOCKET_TOKEN`, `GITHUB_API_TOKEN` (if you use related functionality)

Messaging:
- `MESSAGING_PROVIDER=[activemq|local]`
- `MESSAGING_DEFAULT_TOPIC` (audit), `MESSAGING_EVENT_TOPIC` (event), `MESSAGING_RESPONSE_TOPIC` (response)
- ActiveMQ:
  - `MESSAGING_ACTIVEMQ_BROKER_URL`, `MESSAGING_ACTIVEMQ_USERNAME`, `MESSAGING_ACTIVEMQ_PASSWORD`
  - `JMS_HEALTH_ENABLED=true`
- Local:
  - `LOCAL_QUEUE_CAPACITY`, `LOCAL_CONSUMER_THREADS`, `LOCAL_RETRY_*`, `LOCAL_POLL_TIMEOUT_SECONDS`, `LOCAL_EXPONENTIAL_BACKOFF`

---

## 12) Troubleshooting

- Agent not recognized:
  - Ensure `QODO_AGENT_CONFIG_FILE` points to your `agent.yml`
  - Confirm `agent.yml` command names match handler bean names (`{command}-handler`)
- Local queue not running:
  - Verify `MESSAGING_PROVIDER=local` and internal-core local queue beans are active
  - Check actuator health and logs
- ActiveMQ issues:
  - Confirm broker is healthy: http://localhost:8161
  - Verify connection URL/credentials in environment
- Structured output parse errors:
  - Validate the agent’s `output_schema` and ensure the LLM returns a JSON object matching it
  - Ensure `exit_expression` produces a boolean `success`
- Flow chaining unexpected:
  - Log what your handler’s `type()` returns
  - Verify `success` is true; otherwise BaseHandler will not use `type()` and will route to termination/incomplete

---

## 13) Code Pointers

- Handlers (app):
  - `app/src/main/java/ai/qodo/command/app/handlers/JiraAgentHandler.java`
  - `app/src/main/java/ai/qodo/command/app/handlers/CodingAgentHandler.java`
- Core contracts and flow helpers:
  - `internal-core/.../api/Handler.java`
  - `internal-core/.../service/BaseHandler.java`
  - `internal-core/.../service/MessageService.java`
  - `internal-core/.../service/EndFlowCleanup.java`
  - `internal-core/.../service/MessagePublisher.java`
- Queue implementations:
  - `internal-core/.../service/ActiveMQMessagePublisher` and `ActiveMQMessageConsumer`
  - `internal-core/.../queue/LocalMessagePublisher`, `LocalMessageConsumer`, `LocalMessagingConfig`
- Configurations:
  - `app/src/main/resources/application.yml`
  - `internal-core/src/main/resources/application-internal.yml`
- Docker and example agent:
  - `docker/docker-compose.yml`
  - `docker/agent.yml`

---

## 14) Minimal Implementation Checklist

1) Add your command to `agent.yml` under `commands`
2) Create a handler in `app`:
   - `@Service("{command}-handler")`
   - Extend `BaseHandler`
   - Implement `type()` (next step or `"end_node"`) and `handle(Map)`
3) Choose queue backend:
   - Local: `MESSAGING_PROVIDER=local` → `./gradlew bootRun`
   - ActiveMQ: `docker compose up --build`
4) Trigger via webhook or direct message
5) Verify logs and actuator health

You now have a full path to configure, run, and iterate on agent workflows from the app component using either a robust broker (ActiveMQ) or the internal in-memory queue.

---

## 15) Testing Webhooks Locally

Use these examples to exercise webhook controllers and validate end-to-end flow into your agents.

- Snyk webhook example:
  - Default local port: 8080; Docker Compose default: 8081
  - Replace payload paths and headers as needed for your environment

```
# Local default port
curl -X POST http://localhost:8080/api/webhooks/snyk \
  -H "Content-Type: application/json" \
  -H "X-Snyk-Event: project.snapshot" \
  -H "X-Snyk-Timestamp: 2024-01-01T00:00:00Z" \
  -d @test-payload.json

# Docker Compose default port
curl -X POST http://localhost:8081/api/webhooks/snyk \
  -H "Content-Type: application/json" \
  -H "X-Snyk-Event: project.snapshot" \
  -H "X-Snyk-Timestamp: 2024-01-01T00:00:00Z" \
  -d @test-payload.json
```

- Jira webhook example:
```
# Local default port
curl -X POST http://localhost:8080/api/webhooks/jira/PROJ-123 \
  -H "Content-Type: application/json" \
  -d @jira-payload.json

# Docker Compose default port
curl -X POST http://localhost:8081/api/webhooks/jira/PROJ-123 \
  -H "Content-Type: application/json" \
  -d @jira-payload.json
```

---

## 16) MCP Request Timeout Guidance

Long-running tools (builds, code analysis, large file ops) may require a longer MCP request timeout.

- Default (internal-core): `qodo.mcp.request-timeout-seconds` → 300 in Docker Compose; otherwise framework default may apply.
- Configure via environment variable (recommended):

```
export QODO_MCP_REQUEST_TIMEOUT_SECONDS=300  # 5 minutes
```

- Or via `application.yml` override:

```
qodo:
  mcp:
    request-timeout-seconds: 300
```

Typical values:
- Fast operations: 30–60s
- Code analysis: 90–120s
- Build operations (gradle/maven): 300–600s

Monitor timeouts via logs and adjust as necessary.
