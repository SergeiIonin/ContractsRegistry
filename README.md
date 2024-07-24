# ContractsRegistrator...4s
![latest version](https://img.shields.io/badge/version-0.1.0-orange)
![scala version](https://img.shields.io/badge/scala-3-red)

This project plays a role of onboarding protobuf-based contracts used by the microservices.
Contracts are registered via `Kafka` and `Schema Registry` in an async fashion and for each incoming contract the PR to that repository is created.
Microservices contracts are governed via this service leveraging `Schema Registry` built-in features (creating, deleting, versioning) making 
contracts handing clean and transparent.
Thus onboarding of the new contract consists of 2 steps:
  - Request via REST API to `ContractsRegistrator`
  - Approve of the PR to `ContractsRegistrator` `proto` module
