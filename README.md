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

For beginners, I recommend using [Docker](https://www.docker.com/why-docker) to run your service [containers](https://www.docker.com/why-docker). I do not use the `version` tag for compose files and therefore don't provide support for [legacy compose versions](https://docs.docker.com/reference/compose-file/legacy-versions/). Make sure you have the correct Docker Compose version by having one of the following installed.


 * [Docker Desktop](https://docs.docker.com/desktop/) >= [4.1.0](https://docs.docker.com/desktop/release-notes/#410)(latest recommended)
 
 or

 * Docker Engine and Docker Compose [with compose version >= 1.27.0](https://docs.docker.com/reference/compose-file/legacy-versions/).


## Tutorial Layout

Tutorials are organized by a concept, most commonly a [connector](https://trino.io/docs/current/connector.html). Concepts such as security, clients, or basics should use their own directory as well. There may also be another directory that contains the actual environment and tutorial directories themselves. When running a tutorial command, ensure you're in the correct directory and that it contains a `docker-compose.yml` file in it. 

> [!IMPORTANT]  
> The layout of this repository [has migrated](https://github.com/bitsondatadev/trino-getting-started/issues/53) most concept tutorials to the `community-tutorials/` directory or adopted into other repositories to minimize the scope of this repository to the most common Trino use cases.

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
