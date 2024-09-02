# Contracts Registry
![latest version](https://img.shields.io/badge/version-0.6.0-orange)
![scala version](https://img.shields.io/badge/scala-3-red)

This project provides lifecycle management for `Protobuf`-based contracts in a software project.

One of the main usages is management of onboarding and deletion of contracts used between microservices within an organization.

Handling of such contracts used across the platform may consist of the following stages:
1) Validate the contracts syntactically and semantically
2) Versioning of the contracts (which interleaves with semantical validation)
3) Submit contracts through the pull request to a contracts repository (where `.proto` files are hosted and probalby compiled) for human control

The most intuitive approach of leveraging just a standalone contracts repository has a few drawbacks:

1) There's no protection from breaking semantical validity of the contract, e.g. the contract with the following diff is syntactically correct:
```protobuf
"syntax = "proto3";
 - package users;
 + package users_new;
message GetUser {
  string id = 1
  string domain = 2
}
```
however, semantically such contract should be a different one. It's not a new version of the current contract since the `package` is updated.

2) Provisioning of the semantical validity requires additional efforts.

3) Lack of the navigation for a specific contract name (such as fetch all versions, fetch last version etc)

This projects is intended to solve these problems together with intuitive REST API and lifecycle management.

### REST API

After deploying this project, users are empowered with the following capabilities:
 - **Create new contracts:** `POST /contracts`
 - **Fetch contracts by their `subject` and `version`*:** `GET /contracts/{subject}/versions/{version}` 
 - **Fetch all `subjects`:** `GET /contracts/subjects`
 - **Fetch all `versions` for `subject`:** `GET /contracts/{subject}/versions` 
 - **Fetch the latest version of the contract for the `subject`:** `GET /contracts/{subject}/latest`
 - **Delete the contract for `subject` and `version`:** `DELETE /contracts/{subject}/versions/{version}`
 - **Delete all contracts for the `subject`:** `DELETE /contracts/{subject}`

[*] The term "subject" refers to the `name` of the contract. Both `subject` and `version` are borrowed from [`Schema Registry`](https://docs.confluent.io/platform/current/schema-registry/index.html), which plays a key role in this project.

### GitHub Lifecycle

Creating or deleting a contract triggers a request that generates a Pull Request (PR) in the GitHub repository. 

The PR will resemble the following:

**Title**: `Add contract foo_3` or `Delete contract foo_3`, where `foo` is the `subject` and `3` is the `version`

**From**:  `add-foo-3`

**To**: `configured_branch` (`main` or `master`)

### Key Features

- **Accurate Validation**: Contracts are precisely validated and governed by `Schema Registry`.
- **Versioning**: Contracts for the same `subject` are automatically versioned also by `Schema Registry`.
- **GitHub Integration**: Users submit changes in one request and not approved contracts will not pass.
- **Fine Granulation**: Users can create, view and delete contracts per version.
 
### Example of the contract creation:

```shell
curl --location 'http://myawesometech.com:8080/contracts' \
--header 'Content-Type: application/json' \
--data '{
    "subject": "getUser",
    "schemaType": "PROTOBUF",
    "schema": "syntax = \"proto3\";\npackage users;\n\nmessage GetUser {\n  string id = 1;\n  string domain = 2\n}\n"
}'
```
After submitting the request, user should approve the PR in the relevant repository as usual. And that's it, the contract `getUser_1` is ready for use!

Note that `"schema"` is validated by the `Schema Registry` and the following request will result in 409 (because `package` in the `schema` is altered illegally):

```shell
curl --location 'http://myawesometech.com:8080/contracts' \
--header 'Content-Type: application/json' \
--data '{
    "subject": "getUser",
    "schemaType": "PROTOBUF",
    "schema": "syntax = \"proto3\";\npackage users_new;\n\nmessage GetUser {\n  string id = 1;\n  string domain = 2\n}\n"
}'
```
Similarly, requests with syntactically invalid schema will be rejected.

New valid requests for the same `subject` will create new contract with incremented version `getUser_2`:

```shell
curl --location 'http://myawesometech.com:8080/contracts' \
--header 'Content-Type: application/json' \
--data '{
    "subject": "getUser",
    "schemaType": "PROTOBUF",
    "schema": "syntax = \"proto3\";\npackage users_new;\n\nmessage GetUser {\n  string id = 1;\n  string domain = 2\n; bool isAdmin = 3\n}\n"
}'
```

### Set up
The project consists of 2 modules: `rest-api` and `reader`.
#### reader
This module handles `Kafka` events for creating and deleting contracts.
```shell
GITHUB_BASE_BRANCH=<main/master>;
GITHUB_REPO=<REPO_WHERE_CONTRACTS_RESIDE>;
GITHUB_PATH=<PATH_TO_THE_CONTRACTS_IN_REPO>;
GITHUB_OWNER=<OWNER_OF_THE_REPO_WHERE_CONTRACTS_RESIDE>;
GITHUB_TOKEN=<GITHUB_TOKEN_WITH_ENOUGH_WRITE_CAPABILITIES>
```
additionally provide environment variables to match the following config for `kafka`:
```hocon
kafka {
  contracts-deleted-topic = ${?KAFKA_CONTRACTS_DELETED_TOPIC}
  contracts-deleted-topic = events_contracts_deleted
  contracts-created-topic = ${?KAFKA_CONTRACTS_CREATED_TOPIC}
  contracts-created-topic = events_contracts_created
  consumer-props {
    bootstrap-servers = ${?KAFKA_BOOTSTRAP_SERVERS}
    bootstrap-servers = ["localhost:9092"]
    group-id = ${?KAFKA_GROUP_ID}
    group-id = contracts-registrator-reader
    auto-offset-reset = latest
  }
}
```
### rest-api
Provide environment variables to match the following configs for `kafka-producer`:
```hocon
kafka-producer {
  contracts-deleted-topic = ${?KAFKA_CONTRACTS_DELETED_TOPIC}
  contracts-deleted-topic = events_contracts_deleted
  contracts-created-topic = ${?KAFKA_CONTRACTS_CREATED_TOPIC}
  contracts-created-topic = events_contracts_created
  bootstrap-servers = ${?KAFKA_BOOTSTRAP_SERVERS}
  bootstrap-servers = ["localhost:9092"]
}
```
`postgres`:
```hocon
postgres {
  host = ${?POSTGRES_HOST}
  host = localhost
  port = ${?POSTGRES_PORT}
  port = 5432
  user = ${?POSTGRES_USER}
  user = postgres
  password = ${?POSTGRES_PASSWORD}
  password = postgres
  database = ${?POSTGRES_DATABASE}
  database = contracts_registry
}
```
`schema-registry`:
```hocon
schema-registry {
    host = ${?SCHEMA_REGISTRY_HOST}
    host = "http://localhost"
    port = ${?SCHEMA_REGISTRY_PORT}
    port = 8081
}
```
and http `host` and `port` itselves:
```hocon
rest-api {
    host = ${?RESTAPI_HOST}
    host = "http://localhost"
    port = ${?RESTAPI_PORT}
    port = 8080
}
```
