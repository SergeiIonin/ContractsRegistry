# ContractsRegistry...4s
![latest version](https://img.shields.io/badge/version-0.6.0-orange)
![scala version](https://img.shields.io/badge/scala-3-red)

This project plays a role of lifecycle management of protobuf-based contracts used on some software project. E.g. it could handle onboarding and deleting of the contracts used between microservices within the organization. `Avro` and `Json` contracts should be supported as well.

Upon deploying of this project, users are empowered with the following REST API capabilities:
 - create new contracts: `POST /contracts`
 - fetch contracts by their `subject` and `version`*: `GET /contracts/{subject}/versions/{version}` 
 - fetch all `subjects`: `GET /contracts/subjects`
 - fetch all `versions` for `subject`: `GET /contracts/{subject}/versions` 
 - fetch the latest version of the contract for the `subject`: `GET /contracts/{subject}/latest`
 - delete the contract for `subject` and `version`: `DELETE /contracts/{subject}/versions/{version}`
 - delete all contracts for the `subject` `DELETE /contracts/{subject}`

* `subject` is essentially a name of the contract. The terms `subject` and `version` are borrowed from [`Schema Registry`](https://docs.confluent.io/platform/current/schema-registry/index.html), which plays a key role in this project.

Create and delete of the contract basically mean the request to do so: each action leads to `Pull Request` to the github repository, which should be configured at the project setup stage.

To access the repository, developer should provide `owner`, `repo`, `branch`, and `token` with the relevant capabilities.

Next the PR will resemble the following:

**Title**: `Add contract foo_3`, where `foo` is the `subject` and `3` is the `version`

**From**:  `add-foo-3`

**To**: `configured_branch` (`main` or `master`)
