# Contracts Registry
![latest version](https://img.shields.io/badge/version-0.6.0-orange)
![scala version](https://img.shields.io/badge/scala-3-red)

This project provides lifecycle management for `Protobuf`-based contracts in a software project.

Example usage: management of onboarding and deletion of contracts used between microservices within an organization.

## REST API

After deploying this project, users are empowered with the following capabilities:
 - **Create new contracts:** `POST /contracts`
 - **Fetch contracts by their `subject` and `version`*:** `GET /contracts/{subject}/versions/{version}` 
 - **Fetch all `subjects`:** `GET /contracts/subjects`
 - **Fetch all `versions` for `subject`:** `GET /contracts/{subject}/versions` 
 - **Fetch the latest version of the contract for the `subject`:** `GET /contracts/{subject}/latest`
 - **Delete the contract for `subject` and `version`:** `DELETE /contracts/{subject}/versions/{version}`
 - **Delete all contracts for the `subject`:** `DELETE /contracts/{subject}`

[*] The term "subject" refers to the `name` of the contract. Both `subject` and `version` are borrowed from [`Schema Registry`](https://docs.confluent.io/platform/current/schema-registry/index.html), which plays a key role in this project.

Creating or deleting a contract triggers a request that generates a Pull Request (PR) in the GitHub repository. This repository must be configured during the project setup.

To access the repository, developer should provide `owner`, `repo`, `branch`, and `token` with the relevant capabilities.

Next, the PR will resemble the following:

**Title**: `Add contract foo_3`, where `foo` is the `subject` and `3` is the `version`

**From**:  `add-foo-3`

**To**: `configured_branch` (`main` or `master`)

### Key Features

- **Accurate Validation**: Contracts are precisely validated and governed by `Schema Registry`.
- **Versioning**: Contracts for the same `subject` are automatically versioned also by `Schema Registry`.
- **GitHub Integration**: Users submit changes in one request and not approved contracts shall not pass.
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
After submitting the request, the user should approve the PR in the relevant repository as usual. And that's it, the contract `getUser_1` is ready for use!

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
