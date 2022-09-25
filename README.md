# embulk-input-http_json

[![main](https://github.com/civitaspo/embulk-input-http_json/actions/workflows/main.yml/badge.svg)](https://github.com/civitaspo/embulk-input-http_json/actions/workflows/main.yml)

An Embulk plugin to ingest json records from REST API with transformation by [`jq`](https://github.com/eiiches/jackson-jq).

## Overview

* **Plugin type**: input
* **Resume supported**: yes
* **Cleanup supported**: yes
* **Guess supported**: no

## Configuration

- **scheme**: URI Scheme for the endpoint (string, default: `"https"`, allows: `"https"`, `"http"`)
- **host**: Hostname or IP address of the endpoint (string, required)
- **port**: Port number of the endpoint (integer, optional, allows: `0-65535`)
- **path**: Path of the endpoint (string, optional)
- **headers**: HTTP Headers (array of map, optional, allows: 1 element can contains 1 key-value.)
- **method**: HTTP Method (string, default: `"GET"`, allows: `"GET"`, `"POST"`, `"PUT"`, `"PATCH"`, `"DELETE"`, `"GET"`, `"HEAD"`, `"OPTIONS"`, `"TRACE"`, `"CONNECT"`)
- **params**: HTTP Request params. This is merged with params for pagenation when the `pager` option is specified. (array of map, optional, allows: 1 element can contains 1 key-value.)
- **body**: HTTP Request body. (string, optional)
- **content_type**: HTTP Request Content-Type. (string, default: `"application/json"`)
- **success_condition**: jq filter to check whether the response is succeeded or not. You can use [`jq`](https://github.com/eiiches/jackson-jq) to query for the status code and the response body. (string, `".status_code_class == 200"`)
- **transformer**: jq filter to transform the api response json. (string, `"[.response_body]"`)
- **extract_transformed_json_array**: If true, the plugin extracts the transformed json array, and ingest them as records. (boolean, default: `true`)
- **pager**: (the following options are acceptable, default: `{}`)
  - **initial_params**: Additional HTTP Request params that is used the first request. (array of map, optional, allows: 1 element can contains 1 key-value.)
  - **next_params**: Additional HTTP Request params that is used the subsequent requests. The value is treated as a [`jq`](https://github.com/eiiches/jackson-jq) filter to transform the prior response. (array of map, optional, allows: 1 element can contains 1 key-value.)
  - **while**: jq filter to check whether the pagination is required or not. You can use [`jq`](https://github.com/eiiches/jackson-jq) to query for the status code and the response body. (string, `"false"`)
- **retry**: (the following options are acceptable, default: `{}`)
  - **condition**: jq filter to check whether the response is retryable or not. This condition will be used when it is determined that the response is not succeeded by `success_condition_jq`. You can use [`jq`](https://github.com/eiiches/jackson-jq) to query for the status code and the response body. (string, `"true"`)
  - **max_retries**: Maximum retries. (integer, default: `7`)
  - **initial_interval_millis**: Initial retry interval in milliseconds. (integer, default: `1000`)
  - **max_interval_millis**: Maximum retries interval in milliseconds. (integer, default: `60000`)
- **show_request_body_on_error**: Show request body on error. (boolean, default: `true`)
- **default_timezone**: Default timezone. (string, default: `"UTC"`)
- **default_timestamp_format**: Default timestamp format. (string, default: `"%Y-%m-%d %H:%M:%S %z"`)
- **default_date**: Default date. (string, default: `"1970-01-01"`)

### About the [`jq`](https://github.com/eiiches/jackson-jq) filter

The following options accept the [`jq`](https://github.com/eiiches/jackson-jq) filter to transform the api response json.

- **success_condition**
- **transformer**
- **pager/next_params**
- **retry/condition**

All of the [`jq`](https://github.com/eiiches/jackson-jq) filters transform json that has the same format as the following.

```json
{
  "request_params": [
    {"name": "foo", "value": "bar"}
  ],
  "status_code": 201,
  "status_code_class": 200,
  "response_body": {
    "foo": "bar",
    "results": [
      {"id": 1, "name": "foo"},
      {"id": 2, "name": "bar"}
    ]
  }
}
```

The response of api is stored as the `"response_body"` field, so please note that the [`jq`](https://github.com/eiiches/jackson-jq) filter definition must start with `.response_body` in order to perform jq transformations on the API response results.

## Example

```yaml
in:
  type: http_json
  scheme: http
  host: localhost
  port: 8080
  path: /example
  method: GET
  transformer: '.response_body.integerValues'
  success_condition_jq: '.status_code_class == 200'
out:
  type: stdout
```

## Development

### Run an example

Firstly, you need to start the mock server.

```shell
$ ./example/run-mock-server.sh
```

then, you run the example.

```shell
$ ./gradlew gem
$ embulk run -Ibuild/gemContents/lib -X min_output_tasks=1 example/config.yml
```

The requested records are shown on the mock server console.

### Run tests

```shell
$ ./gradlew test
```

### Build

```
$ ./gradlew gem  # -t to watch change of files and rebuild continuously
```

### Update dependencies locks

```shell
$ ./gradlew dependencies --write-locks
```

### Run the formatter

```shell
## Just check the format violations
$ ./gradlew spotlessCheck

## Fix the all format violations
$ ./gradlew spotlessApply
```

### Release a new gem

A new tag is pushed, then a new gem will be released. See [the Github Action CI Setting](./.github/workflows/main.yml).

## CHANGELOG

See. [Github Releases](https://github.com/civitaspo/embulk-input-http_json/releases)

## License

[MIT LICENSE](./LICENSE.txt)
