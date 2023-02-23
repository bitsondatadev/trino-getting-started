# Trino Getting Started

Welcome to the [Trino](https://trino.io/) getting started tutorial repository. 
This is a home for a set of preconfigured [Docker Compose](https://docs.docker.com/compose/) 
environments that are used to set up simple environments and showcase basic 
configurations in isolation to get new and existing users started on all the 
different containers we have, and more importantly learn and have fun with 
Trino.

If you're entirely new to Trino, you're not alone. Trino is a distributed SQL 
query engine designed to query large data sets distributed over one or more 
heterogeneous data sources. Check out some of our [use cases](https://trino.io/docs/current/overview/use-cases.html) 
to understand what Trino is and is not.  We also have a rascally little bunny 
mascot named 
[Commander Bun Bun](https://twitter.com/trinodb/status/1357416368543588356) 🐇.

Another thing to know about Trino is that it was originally called Presto® SQL 
and it shares the first 7 years of code with the well known Presto® DB. Trino
houses the four original creators of Presto® and the majority code contributors
of both Presto® and Trino. You can read more about why this split happened 
[in the Trino rebrand blog](https://trino.io/blog/2020/12/27/announcing-trino.html).

## Prerequisites

In order to use this repository you need to have [Docker](https://www.docker.com/why-docker) installed to run your service [containers](https://www.docker.com/why-docker). Check if you have Docker installed by running `docker --version`. If Docker isn't found, please refer to the [install insructions](https://docs.docker.com/engine/install/) and install Docker before trying to run these tutorials. If you're on mac or windows, you will just need to install docker desktop. If you're on a linux distribution, you will just need to install the docker engine.

## Layout

The first level of directories in this repo are generally organized by [connector](https://trino.io/docs/current/connector.html). Concepts such as security, clients, or basics will have their own directory as well. The second level of directories contain the actual environment and tutorial directories themselves. In order to run the environment, you need to be in one of these directories that have a docker-compose.yml file in it. The second level of directories contain the actual environment and tutorial directories themselves. In order to run the environment, you need to be in one of these directories that have a docker-compose.yml file in it.

## Helpful Docker commands

### Start Service

`docker compose up -d`

### Stop Service

`docker compose stop`

### Clean Service

[cleans images, containers, and network](shttps://docs.docker.com/config/pruning/)

`docker system prune --all --force`

[cleans volumes](shttps://docs.docker.com/config/pruning/)

`docker volume prune --force`

### Show Service Images 

`docker images`

### Login to Container

`docker container exec -it <container_id> /bin/bash`

### Show Service Logs

`docker logs <container_id>`

### List Services

`docker container ls`

### List Service Process information

`docker compose ps`

See trademark and other [legal notices](https://trino.io/legal.html).