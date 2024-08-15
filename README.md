# ContractsRegistry...4s (WIP)
![latest version](https://img.shields.io/badge/version-0.1.0-orange)
![scala version](https://img.shields.io/badge/scala-3-red)

This project plays a role of onboarding protobuf-based contracts used by the microservices of some platform.

Contracts are registered via REST API which dispatches requests to `Schema Registry`, next each incoming contract is processed as `Kafka` record asynchronously. Finally, for each incoming contract the service creates a PR to the repository defined in the config.

To access the repository, developer should provide `owner`, `repo`, `branch`, and `token` with the relevant capabilities.

Microservices contracts are governed via this service leveraging `Schema Registry` built-in features (creating, deleting, versioning) making 
contracts handing clean and transparent.

Thus, onboarding of the new contract consists of 2 steps:
  - Request via REST API to `ContractsRegistry`
  - Approve of the PR to `owner:repo:branch` with the new contract `<contract_name>_v<contract_version>.proto` (e.g. `foo_v1.proto`)

Note: it's implied that the repository holding the contracts is a standalone repository where `.proto` files are hosted (and maybe also compiled). 

It's also possible to delete contract version (`foo_v1.proto`) or the all contracts for the prefix (e.g. all contracts starting with `foo`).
