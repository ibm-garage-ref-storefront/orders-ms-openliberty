###### orders-ms-openliberty

# Microservice Apps Integration with MariaDB Database and enabling OpenID Connect protection for APIs

*This project is part of the 'IBM Cloud Native Reference Architecture' suite, available at
https://cloudnativereference.dev/*

## Table of Contents

* [Introduction](#introduction)
    + [APIs](#apis)
* [Pre-requisites](#pre-requisites)
* [Implementation Details](#implementation-details)
* [Running the application on Docker](#running-the-application-on-docker)
    + [Get the Orders application](#get-the-inventory-application)
    + [Run the MariaDB Docker Container](#run-the-mysql-docker-container)
    + [Run the Auth Docker Container](#run-the-auth-docker-container)
    + [Set up custom keystore](#set-up-custom-keystore)
    + [Run the Orders application](#run-the-inventory-application)
    + [Validating the application](#validating-the-application)
    + [Exiting the application](#exiting-the-application)
* [Conclusion](#conclusion)

## Introduction

This project will demonstrate how to deploy a Openliberty Microprofile application with a MariaDB database onto a Kubernetes Cluster. This application provides basic operations of saving and querying orders from a relational database as part of the Orders service of Storefront.

![Application Architecture](static/orders.png?raw=true)

Here is an overview of the project's features:
- Based on [MicroProfile](https://microprofile.io/).
- Uses [`MariaDB`](https://mariadb.org/) as the orders database.
- Uses [`Docker`](https://docs.docker.com/) to package application binary and its dependencies.
- When retrieving orders using the OAuth 2.0 protected APIs, return only orders belonging to the user identity encoded in the user_name claim in the JWT payload. For more details on how identity is propagated, refer [Auth](https://github.com/ibm-garage-ref-storefront/auth-ms-openliberty) Microservice.

### APIs

The Orders Microservice REST API is OAuth 2.0 protected. These APIs identifies and validates the caller using signed JWT tokens.

* `GET /micro/orders`

  Returns all orders. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim. A JSON object array is returned consisting of only orders created by the orders ID.

* `GET /micro/orders/{id}`

  Return order by ID. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim. If the id of the order is owned by the orders passed in the header, it is returned as a JSON object in the response; otherwise `HTTP 401` is returned.

* `POST /micro/orders`

  Create an order. The caller of this API must pass a valid OAuth token. The OAuth token is a JWT with the orders ID of the caller encoded in the `user_name` claim. The Order object must be passed as JSON object in the request body with the following format.

  ```
    {
      "itemId": "item_id",
      "count": "number_of_items_in_order",
    }
  ```

  On success, you will see a success message something like below.

  ```
    {
      "count": number_of_items_in_order,
      "customerId": "foo",
      "date": "yyyy-mm-ddTHH:MM:SS[UTC]",
      "id": "order_id",
      "itemId": "item_id"
    }
  ```

## Pre-requisites:

* [Appsody](https://appsody.dev/)
    + [Installing on MacOS](https://appsody.dev/docs/installing/macos)
    + [Installing on Windows](https://appsody.dev/docs/installing/windows)
    + [Installing on RHEL](https://appsody.dev/docs/installing/rhel)
    + [Installing on Ubuntu](https://appsody.dev/docs/installing/ubuntu)
For more details on installation, check [this](https://appsody.dev/docs/installing/installing-appsody/) out.

* Docker Desktop
    + [Docker for Mac](https://docs.docker.com/docker-for-mac/)
    + [Docker for Windows](https://docs.docker.com/docker-for-windows/)

## Implementation Details

We created a new openliberty project using appsody as follows.

```
appsody repo add kabanero https://github.com/kabanero-io/kabanero-stack-hub/releases/download/0.6.5/kabanero-stack-hub-index.yaml

appsody init kabanero/java-openliberty
```

## Running the application on Docker

### Get the Orders application

- Clone orders repository:

```bash
git clone https://github.com/ibm-garage-ref-storefront/orders-ms-openliberty.git
cd orders-ms-openliberty
```

### Run the MariaDB Docker Container

Run the below command to get MariaDB running via a Docker container.

```bash
# Start a MariaDB Container with a database user, a password, and create a new database
docker run --name ordersmysql \
    -e MYSQL_ROOT_PASSWORD=admin123 \
    -e MYSQL_USER=dbuser \
    -e MYSQL_PASSWORD=password \
    -e MYSQL_DATABASE=ordersdb \
    -p 3307:3306 \
    -d mariadb
```

If it is successfully deployed, you will see something like below.

```
$ docker ps
CONTAINER ID        IMAGE                           COMMAND                  CREATED             STATUS              PORTS                                                                    NAMES
5762edb59a86        mariadb                         "docker-entrypoint.s…"   6 days ago          Up 6 days           0.0.0.0:3307->3306/tcp  
```

- Create the orders table.

Firstly ssh into the MySQL container.

```
docker exec -it ordersmysql bash
```

- Now, run the below command for table creation.

```
mysql -udbuser -ppassword
```

- This will take you to something like below.

```
root@5762edb59a86:/# mysql -udbuser -ppassword
Welcome to the MariaDB monitor.  Commands end with ; or \g.
Your MariaDB connection id is 22
Server version: 10.5.5-MariaDB-1:10.5.5+maria~focal mariadb.org binary distribution

Copyright (c) 2000, 2018, Oracle, MariaDB Corporation Ab and others.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

MariaDB [(none)]>
```

- Go to `scripts > mysql_data.sql`. Copy the contents from [mysql_data.sql](./scripts/mysql_data.sql) and paste the contents in the console.

- You can exit from the console using `exit`.

```      
MariaDB [(none)]> exit
Bye
```

* To come out of the container, enter `exit`.

```
root@5762edb59a86:/# exit
exit
```

### Run the Auth Docker Container

- Open a new terminal.

- Clone the below repository.

```bash
git clone https://github.com/ibm-garage-ref-storefront/auth-ms-openliberty.git
cd auth-ms-openliberty
```

- Run the below command.

```
appsody build
```

This builds the docker image.

- Now, run the auth service as follows.

```
docker run -it --name auth-ms-openliberty -p 9081:9080 -p 9444:9443 -e jwksIssuer="https://localhost:9444/oidc/endpoint/OP" -d dev.local/auth-ms-openliberty
```

If it is successfully deployed, you will see something like below.

```
$ docker ps
CONTAINER ID        IMAGE                           COMMAND                  CREATED             STATUS              PORTS                                                                    NAMES
05ee16bff4a4        dev.local/auth-ms-openliberty   "/opt/ol/helpers/run…"   4 hours ago         Up 4 hours          0.0.0.0:9081->9080/tcp, 0.0.0.0:9444->9443/tcp                           auth-ms-openliberty
5762edb59a86        mariadb                         "docker-entrypoint.s…"   6 days ago          Up 6 days           0.0.0.0:3307->3306/tcp  
```

### Set up custom keystore

- Use the certs generated previously in the Auth service. For details on how to generate them, click [here](https://github.com/ibm-garage-ref-storefront/auth-ms-openliberty#set-up-custom-keystore).

- Grab the `BCKeyStoreFile.p12`, `client.cer`, and `truststore.p12` and place them under `src/main/liberty/config/resources/security`

### Run the Orders application

- Before running the application, make sure you set up the custom keystore properly.

- Set up Jaegar for opentracing. This is optional. If you want to enable distributed tracing your application, run this step.

```
 docker run -d --name jaeger   -e COLLECTOR_ZIPKIN_HTTP_PORT=9411   -p 5775:5775/udp   -p 6831:6831/udp   -p 6832:6832/udp   -p 5778:5778   -p 16686:16686   -p 14268:14268   -p 9411:9411   jaegertracing/all-in-one:1.11
```

- To run the orders application, run the below command.

```
appsody run --docker-options "-e jdbcURL=jdbc:mysql://<docker_host>:3307/ordersdb?useSSL=true -e dbuser=<database user name> -e dbpassword=<database password> -e jwksIssuer="https://localhost:9444/oidc/endpoint/OP" -e JAEGER_SERVICE_NAME=catalog-ms-openliberty -e JAEGER_AGENT_HOST=host.docker.internal -e JAEGER_AGENT_PORT=6831 -e JAEGER_REPORTER_LOG_SPANS=true -e JAEGER_REPORTER_FLUSH_INTERVAL=2000 -e JAEGER_SAMPLER_TYPE=const -e JAEGER_SAMPLER_PARAM=1"
```

For instance <docker_host>, it will be `host.docker.internal`, <database user name> will be `dbuser` and <database password> will be `password`.

If not running Jaegar, run the below command.

```
appsody run --docker-options "-e jdbcURL=jdbc:mysql://<docker_host>:3307/ordersdb?useSSL=true -e dbuser=<database user name> -e dbpassword=<database password> -e jwksIssuer="https://localhost:9444/oidc/endpoint/OP" "
```

For instance <docker_host>, it will be `host.docker.internal`, <database user name> will be `dbuser` and <database password> will be `password`.

- If it is successfully running, you will see something like below.

```
[Container] [INFO] CWWKM2010I: Searching for CWWKZ0003I.*orders-ms-openliberty in /opt/ol/wlp/usr/servers/defaultServer/logs/messages.log. This search will timeout after 30 seconds.
[Container] [INFO] [AUDIT   ] CWWKT0016I: Web application available (default_host): http://3395123e7b35:9080/
[Container] [INFO] [AUDIT   ] CWWKZ0003I: The application orders-ms-openliberty updated in 1.773 seconds.
[Container] [INFO] [AUDIT   ] CWWKT0017I: Web application removed (default_host): http://3395123e7b35:9080/
[Container] [INFO] [AUDIT   ] CWWKZ0009I: The application orders-ms-openliberty has stopped successfully.
[Container] [INFO] CWWKM2015I: Match number: 8 is [10/20/20 14:37:11:934 UTC] 00000470 com.ibm.ws.app.manager.AppMessageHelper                      A CWWKZ0003I: The application orders-ms-openliberty updated in 1.773 seconds..
[Container] [INFO] Running integration tests...
[Container] [INFO]
[Container] [INFO] -------------------------------------------------------
[Container] [INFO]  T E S T S
[Container] [INFO] -------------------------------------------------------
[Container] [INFO] [AUDIT   ] CWWKT0016I: Web application available (default_host): http://3395123e7b35:9080/
[Container] [INFO] [AUDIT   ] CWWKZ0003I: The application orders-ms-openliberty updated in 1.766 seconds.
[Container] [INFO] Running it.dev.appsody.orders.EndpointTest
[Container] [INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.776 s - in it.dev.appsody.orders.EndpointTest
[Container] [INFO] Running it.dev.appsody.orders.HealthEndpointTest
[Container] [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.218 s - in it.dev.appsody.orders.HealthEndpointTest
[Container] [INFO]
[Container] [INFO] Results:
[Container] [INFO]
[Container] [INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
[Container] [INFO]
[Container] [INFO] Integration tests finished.
```

- You can also verify it as follows.

```
$ docker ps
CONTAINER ID        IMAGE                           COMMAND                  CREATED             STATUS              PORTS                                                                    NAMES
3395123e7b35        kabanero/java-openliberty:0.2   "/.appsody/appsody-c…"   2 hours ago         Up 2 hours          0.0.0.0:7777->7777/tcp, 0.0.0.0:9080->9080/tcp, 0.0.0.0:9443->9443/tcp   orders-ms-openliberty
05ee16bff4a4        dev.local/auth-ms-openliberty   "/opt/ol/helpers/run…"   4 hours ago         Up 4 hours          0.0.0.0:9081->9080/tcp, 0.0.0.0:9444->9443/tcp                           auth-ms-openliberty
5762edb59a86        mariadb                         "docker-entrypoint.s…"   6 days ago          Up 6 days           0.0.0.0:3307->3306/tcp  
```

### Validating the application

- Now generate a JWT Token with `openid` scope, which will let you create/get/delete orders.

To do so, run the commands below:

```
curl -k -d "grant_type=password&client_id=bluecomputeweb&client_secret=bluecomputewebs3cret&username=<user>&password=<password>&scope=openid" https://localhost:9444/oidc/endpoint/OP/token
```

If it is successful, you will see something like below.

```
$ curl -k -d "grant_type=password&client_id=bluecomputeweb&client_secret=bluecomputewebs3cret&username=foo&password=bar&scope=openid" https://localhost:9444/oidc/endpoint/OP/token
{"access_token":"eyJraWQiOiJ0aDVYaWg2Z0NQLV83d2pyN2FtOF8yVURnbGxQc28xT1pjNlpMcmhJdmZJIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmb28iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic2NvcGUiOlsib3BlbmlkIl0sImp0aSI6Ill1ekEzMkVwcnJKcmcySEMiLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo5NDQ0L29pZGMvZW5kcG9pbnQvT1AiLCJleHAiOjE2MDMyMTk1ODAsImlhdCI6MTYwMzIxMjM4MCwiZ3JvdXBzIjpbIlVzZXJzIl0sInVwbiI6ImZvbyJ9.ioBKZpIZ6x0c00v0bgwyxRWoSIXf-LXFY0kjQHOT9zrbXzZte3JLpx1J1FkPvYntxZOmxzIGRIiNuMUlRveOD1DIZkjJcsMyijQt2NCx9FWxPN3bUOHhG3F7MCJxCR7tANNMa3DfbQceXmLeP2n4KgLdYoV0SNQ7k5OUZNhuMg5KShledehOeGfrfT8VbXM0gBKC8RyH5wmQLUQj_ykzkKJ4y2czwmUq4NKsVqZoFFdYaFvKFOuxAXSPiIKDWlMViU2L8599wyNi4IHX5LOfLu3vDh-SLVkFebR1SU_82qUwsD6qUcDzJwwrqQE1o5DLc4k7Olo2ZfbdAJGk56TjfA","token_type":"Bearer","expires_in":7200,"scope":"openid","refresh_token":"eN5kHBxwVmZcyGRScE2WLDIwqOUTn50tiwVRkgOwoaBnRcHiSc"}
```      

#### Validation

Now, you can validate the application as follows.

* Create an Order

Run the following to create an order for the user. Be sure to use the JWT retrieved from the previous step in place of `<jwt token>`.

```
curl -k -X POST   --url https://localhost:9443/micro/orders   --header "Content-Type: application/json"   --header "Authorization: Bearer <jwt token>" -d '{"itemId":13401, "count":1}'
```

If successfully created, you will see something like below.

```
$ curl -k -X POST   --url https://localhost:9443/micro/orders   --header "Content-Type: application/json"   --header "Authorization: Bearer eyJraWQiOiJ0aDVYaWg2Z0NQLV83d2pyN2FtOF8yVURnbGxQc28xT1pjNlpMcmhJdmZJIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmb28iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic2NvcGUiOlsib3BlbmlkIl0sImp0aSI6Ill1ekEzMkVwcnJKcmcySEMiLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo5NDQ0L29pZGMvZW5kcG9pbnQvT1AiLCJleHAiOjE2MDMyMTk1ODAsImlhdCI6MTYwMzIxMjM4MCwiZ3JvdXBzIjpbIlVzZXJzIl0sInVwbiI6ImZvbyJ9.ioBKZpIZ6x0c00v0bgwyxRWoSIXf-LXFY0kjQHOT9zrbXzZte3JLpx1J1FkPvYntxZOmxzIGRIiNuMUlRveOD1DIZkjJcsMyijQt2NCx9FWxPN3bUOHhG3F7MCJxCR7tANNMa3DfbQceXmLeP2n4KgLdYoV0SNQ7k5OUZNhuMg5KShledehOeGfrfT8VbXM0gBKC8RyH5wmQLUQj_ykzkKJ4y2czwmUq4NKsVqZoFFdYaFvKFOuxAXSPiIKDWlMViU2L8599wyNi4IHX5LOfLu3vDh-SLVkFebR1SU_82qUwsD6qUcDzJwwrqQE1o5DLc4k7Olo2ZfbdAJGk56TjfA" -d '{"itemId":13401, "count":1}'
{"count":1,"customerId":"foo","date":"2020-10-20T14:58:06.675Z[UTC]","id":"d07d2d9b-fd02-40b7-bce7-1fe6340403e8","itemId":13401}
```

* Get all Orders

Run the following to retrieve all orders for the given customerId. Be sure to use the JWT retrieved from the previous step in place of `<jwt token>`.

```
curl -k --request GET   --url https://localhost:9443/micro/orders   --header 'Authorization: Bearer <jwt token>'   --header 'Content-Type: application/json'
```

If it is running successfully, you will see something like below.

```
$ curl -k --request GET   --url https://localhost:9443/micro/orders   --header 'Authorization: Bearer eyJraWQiOiJ0aDVYaWg2Z0NQLV83d2pyN2FtOF8yVURnbGxQc28xT1pjNlpMcmhJdmZJIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJmb28iLCJ0b2tlbl90eXBlIjoiQmVhcmVyIiwic2NvcGUiOlsib3BlbmlkIl0sImp0aSI6IlFqVFZUWE1KNk9USHlqTnIiLCJpc3MiOiJodHRwczovL2xvY2FsaG9zdDo5NDQ0L29pZGMvZW5kcG9pbnQvT1AiLCJleHAiOjE2MDMyMDUzMjUsImlhdCI6MTYwMzE5ODEyNSwiZ3JvdXBzIjpbIlVzZXJzIl0sInVwbiI6ImZvbyJ9.k4mCqokWHFQMmyZF7Fu4iXMHdq77k6IDcgwH5Pm1UidIWz_Xi05c-vSgKkmLUR6xkbDnd5-9YslxrRotYNupMiHlc4lG0M3KYwFJrwVjphMQ8bXYmreILjQQqEj4GN-cJzR83N5Kt5y7y3ScBsdrlGeOO2Z-AUcSyzZBHVKNedwJElZo8idzLRaqldCYnSz6tr9krlED70XgRH9f4ea2hjtWh2VRZgcg2H1b1Z35U-LuqnUWO4uTrzOgerz0hBeDibcNOXZVMAH-KOxuyhGwEddUfaWeF3fIwkRW8NJtHiPI4wdL4OjbGwx_D4NtaZwodVtD-4PdC651JDtuwuaa-Q'   --header 'Content-Type: application/json'
[{"count":1,"customerId":"foo","date":"2020-10-20T12:06:09Z[UTC]","id":"bb8c0512-f03a-46b1-b2c2-59813bdb49ce","itemId":13401}]
```

If enabled Jaegar, access it at http://localhost:16686/ and point the service to `orders-ms-openliberty` to access the traces.

![Jaegar UI](./static/jaegar_ui_orders.png)

![Jaegar traces](./static/jaegar_traces.png)

Also openapi is enabled.

    - http://localhost:9080/openapi/ui/
    - http://localhost:9080/index.html
    - http://localhost:9080/health
    - http://localhost:9080/openapi

![Openapi UI](./static/openapi-ui.png)

### Exiting the application

To exit the application, just press `Ctrl+C`.

It shows you something like below.

```
[Container] [INFO] ------------------------------------------------------------------------
[Container] [INFO] BUILD SUCCESS
[Container] [INFO] ------------------------------------------------------------------------
[Container] [INFO] Total time:  20:35 min
[Container] [INFO] Finished at: 2020-05-07T09:17:39Z
[Container] [INFO] ------------------------------------------------------------------------
Closing down development environment.
```

## Conclusion

You have successfully deployed and tested the Orders Microservice and a MariaDB database in local Docker Containers using Appsody.

To see the Orders application working in a more complex microservices use case, checkout our Microservice Reference Architecture Application [here](https://cloudnativereference.dev/).
