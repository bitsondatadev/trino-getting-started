# Trino Getting Started

Welcome to the [Trino](https://trino.io/) getting started tutorial repository. 
This is a home for a set of preconfigured [Docker Compose](https://docs.docker.com/compose/) 
environments that are used to set up simple environments and showcase basic 
configurations in isolation to get new and existing users started on all the 
different containers we have, and more importantly learn and have fun with 
Trino.

## Trino History

Trino is a distributed SQL query engine designed to query large data sets
distributed over one or more heterogeneous data sources. Check out some of
the [use cases](https://trino.io/docs/current/overview/use-cases.html) to
understand what Trino is and is not.  We also have a rascally little bunny
mascot named 
[Commander Bun Bun](https://twitter.com/trinodb/status/1357416368543588356) 🐇.

Trino was [originally called Presto, and was a seperate fork that renamed
itself to PrestoSQL](https://en.wikipedia.org/wiki/Trino_(SQL_query_engine)#History). 
It shares the first 7 years of development with [PrestoDB
](https://en.wikipedia.org/wiki/Presto_(SQL_query_engine)). Trino resulted from
a [disagreement in the governance](https://trino.io/blog/2022/08/02/leaving-facebook-meta-best-for-trino)
leading the four original creators of Presto and the majority of its contributors
to using Trino.
 
## Prerequisites

In order to use this repository you need to have [Docker](https://www.docker.com/why-docker) installed to run your service [containers](https://www.docker.com/why-docker). Check if you have Docker installed by running `docker --version`. If Docker isn't found, please refer to the [install insructions](https://docs.docker.com/engine/install/) and install Docker before trying to run these tutorials. If you're on mac or windows, you will just need to install docker desktop. If you're on a linux distribution, you will just need to install the docker engine.

## Layout

The first level of directories in this repo are generally organized by [connector](https://trino.io/docs/current/connector.html). Concepts such as security, clients, or basics will have their own directory as well. The second level of directories contain the actual environment and tutorial directories themselves. In order to run the environment, you need to be in one of these directories that have a docker-compose.yml file in it. The second level of directories contain the actual environment and tutorial directories themselves. In order to run the environment, you need to be in one of these directories that have a docker-compose.yml file in it.


> [!IMPORTANT]  
> The layout of this repository [has been updated](https://github.com/bitsondatadev/trino-getting-started/issues/53) to minimize the scope of this repository to 

### Community Tutorials

The `community-tutorials/` directory contains a list of tutorials that have little to no testing and may contain outdated versions, bugs, or missing container images. Feel free to open up an issue if you would like to adopt one of these tutorials on your own repository or website and we will link them in the following list. Otherwise, feel free to provide Pull requests for any functionality in these tutorials, or submit a new one. [See more information here](https://github.com/bitsondatadev/trino-getting-started/issues/53)

| Tutorial | Description |
| --- | --- |
| [Trino on Backblaze b2](https://github.com/backblaze-b2-samples/trino-getting-started-b2) | These tutorials by @metadaddy demonstrate how to use Trino with Backblaze b2 as an S3 object storage with connectors like Hive and Iceberg. |

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
