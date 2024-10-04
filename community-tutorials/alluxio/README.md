# Using Alluxio caching via the Iceberg connector over MinIO file storage

## Introduction 
If you're new to Alluxio it's a system that sits between data driven applications, 
such as Trino and Apache Spark, and various persistent storage systems, such as 
Amazon S3, Google Cloud Storage, HDFS, Ceph, and Minio. Alluxio unifies the data 
stored in these different storage systems, presenting unified client APIs and a 
global namespace to its upper layer data driven applications.
There was recently a [Trino Community Broadcast that discussed Alluxio and Trino in more detail](https://trino.io/episodes/43.html)

### Video Tutorial

[![Watch the video](youtube.png)](https://youtu.be/yaxPEWRpEzc)

=======

## Goals
In this tutorial, you will:
 1. Learn how to configure Alluxio to point to S3 storage like MinIO.
 2. Learn how to query Alluxio with query write-through from Trino.
 
## Resources
* [Configuring Trino and Alluxio](https://docs.alluxio.io/os/user/edge/en/compute/Trino.html)
* [Configuring MinIO and Alluxio](https://docs.alluxio.io/os/user/2.1/en/ufs/Minio.html)
* [Alluxio configuration settings](https://docs.alluxio.io/os/user/edge/en/operation/Configuration.html)
* [Alluxio property list](https://docs.alluxio.io/os/user/stable/en/reference/Properties-List.html)
* [Alluxio CLI commands](https://docs.alluxio.io/os/user/stable/en/operation/User-CLI.html)
* [Hive and Alluxio configurations](https://docs.alluxio.io/os/user/2.1/en/compute/Hive.html)
* [Running Alluxio on Docker](https://docs.alluxio.io/os/user/edge/en/deploy/Running-Alluxio-On-Docker.html)

## Steps

This project will run Alluxio on seperate servers while the recommendation is to run 
Alluxio on the same nodes where Trino runs. This means that all the configuration for
Alluxio will exist on the servers where Alluxio runs and Trino's configuration is
unaffected. The advantage of running Alluxio externally is that it won't contend for
resources with Trino. The disadvantage is that you will require moving files across the
network whenever reading data from Alluxio. It is critical with performance that Trino
and Alluxio are on the same network.

### Trino configuration

Trino is configured identically to a standard Iceberg configuration. Since Alluxio is
running external to Trino, the only configuration needed is at query time and not at
startup.

### Alluxio configuration

The configuration for Alluxio can all be set using the 
[`alluxio-site.properties`](https://docs.alluxio.io/os/user/edge/en/operation/Configuration.html#alluxio-siteproperties-files-recommended)
file. To keep all configurations colocated on the `docker-compose.yml`, we are setting them using
Java properties via the `ALLUXIO_JAVA_OPTS` environment variable. This tutorial also refers to the master
node as the leader and the workers as followers.

#### Leader configurations


```
alluxio.master.mount.table.root.ufs=s3://alluxio/ 
```

The leader exposes ports `19998` and `19999`, the latter being the port for the web UI.


#### Follower configurations
    
```
alluxio.worker.ramdisk.size=1G
alluxio.worker.hostname=alluxio-follower
```

The follower exposes ports `29999` and `30000`, and sets up a shared memory used by Alluxio
to store data. This is set to `1G` via the `shm_size` property and is referenced from the 
`alluxio.worker.ramdisk.size` property.

#### Shared configurations between leader and follower

```
alluxio.master.hostname=alluxio-leader

# Minio configs
alluxio.underfs.s3.endpoint=http://minio:9000
alluxio.underfs.s3.disable.dns.buckets=true
alluxio.underfs.s3.inherit.acl=false
aws.accessKeyId=minio
aws.secretKey=minio123

# Demo-only configs
alluxio.security.authorization.permission.enabled=false
```
The `alluxio.master.hostname` needs to be on all nodes, leaders and followers. The majority
of shared configs points Alluxio to the `underfs` which is MinIO in this case.
`alluxio.security.authorization.permission.enabled` is set to false to keep the docker setup
simple, this is not recommended to do in a production or CI/CD environment.

### Running Services

First, you want to start the services. Make sure that you are in the 
`trino-getting-started/iceberg/trino-alluxio-iceberg-minio` directory. Now run the following
command:

```
docker compose up -d
```

You should expect to see the following output. Docker may also have to download
the Docker images before you see the "Created/Started" messages, so there could
be extra output:

```
[+] Running 10/10
 ⠿ Network trino-alluxio-iceberg-minio_trino-network          Created                                                                                                                                                                                                      0.0s
 ⠿ Volume "trino-alluxio-iceberg-minio_minio-data"            Created                                                                                                                                                                                                      0.0s
 ⠿ Container trino-alluxio-iceberg-minio-mariadb-1            Started                                                                                                                                                                                                      0.6s
 ⠿ Container trino-alluxio-iceberg-minio-trino-coordinator-1  Started                                                                                                                                                                                                      0.7s
 ⠿ Container trino-alluxio-iceberg-minio-alluxio-leader-1     Started                                                                                                                                                                                                      0.9s
 ⠿ Container minio                                            Started                                                                                                                                                                                                      0.8s
 ⠿ Container trino-alluxio-iceberg-minio-alluxio-follower-1   Started                                                                                                                                                                                                      1.5s
 ⠿ Container mc                                               Started                                                                                                                                                                                                      1.4s
 ⠿ Container trino-alluxio-iceberg-minio-hive-metastore-1     Started
```

### Open Trino CLI

Once this is complete, you can log into the Trino coordinator node. We will
do this by using the [`exec`](https://docs.docker.com/engine/reference/commandline/exec/)
command and run the `trino` CLI executable as the command we run on that
container. Notice the container id is `trino-alluxio-iceberg-minio-trino-coordinator-1` so the
command you will run is:

```
<<<<<<< HEAD
docker container exec -it trino-alluxio-iceberg-minio-trino-coordinator-1 trino
=======
docker container exec -it trino-minio_trino-coordinator_1 trino
>>>>>>> alluxio
```

When you start this step, you should see the `trino` cursor once the startup
is complete. It should look like this when it is done:
```
trino>
```
 
To best understand how this configuration works, let's create an Iceberg table using a CTAS 
(CREATE TABLE AS) query that pushes data from one of the TPC connectors into Iceberg that 
points to MinIO. The TPC connectors generate data on the fly so that we can run simple tests
like this.

First, run a command to show the catalogs to see the `tpch` and `iceberg` catalogs
since these are what we will use in the CTAS query.

```
SHOW CATALOGS;
```

You should see that the iceberg catalog is registered. 

### MinIO buckets and Trino Schemas

Upon startup, the following command is executed on an intiailization container that includes
the `mc` CLI for MinIO. This creates a bucket in MinIO called `/alluxio` which gives us a 
location to write our data to and we can tell Trino where to find it.

```
/bin/sh -c "
until (/usr/bin/mc config host add minio http://minio:9000 minio minio123) do echo '...waiting...' && sleep 1; done;
/usr/bin/mc rm -r --force minio/alluxio;
/usr/bin/mc mb minio/alluxio;
/usr/bin/mc policy set public minio/alluxio;
exit 0;
"
```

Note: This bucket will act as the mount point for Alluxio. So the schema directory
`alluxio://lakehouse/` in Alluxio will map to `s3://alluxio/lakehouse/`.

### Querying Trino

Let's move to creating our `SCHEMA` that points us to the bucket in MinIO and 
then run our CTAS query. Back in the terminal create the `iceberg.lakehouse` `SCHEMA`.
This will be the first call to the metastore to save the location of the schema location
in the Alluxio namespace. Notice, we will need to specify the hostname `alluxio-leader`
and port `19998` since we did not set Alluxio as the default filesystem. Take this
into consideration if you want Alluxio caching to be the default usage and
transparent to users managing DDL statements.

```
CREATE SCHEMA iceberg.lakehouse
WITH (location = 'alluxio://alluxio-leader:19998/lakehouse/');
```

Now that we have a SCHEMA that references the bucket where we store our tables 
in Alluxio which syncs to MinIO, we now can create our first table.

Optional: To view your queries run, log into the 
[Trino UI](http://localhost:8080) and log in using any username (it doesn't
 matter since no security is set up).

Move the customer data from the tiny generated tpch data into MinIO uing a CTAS
query. Run the following query and if you like, watch it running on the Trino UI:

```
CREATE TABLE iceberg.lakehouse.customer
WITH (
  format = 'ORC',
  location = 'alluxio://alluxio-leader:19998/lakehouse/customer/'
) 
AS SELECT * FROM tpch.tiny.customer;
```

Go to the [Alluxio UI](http://localhost:19999/) and the [MinIO UI](http://localhost:9000),
and browse the Alluxio and MinIO files and you will now see a `lakehouse` directory that
contains a `customer` directory that contains the data written by Trino to Alluxio and 
Alluxio writing it to MinIO.
 
Now there is a table under Alluxio and MinIO, you can query this data by checking the
following.
```
SELECT * FROM iceberg.lakehouse.customer LIMIT 10;
```

How are we sure that Trino is actually reading from Alluxio and not MinIO? Let's delete
the data in MinIO and run the query again just to be sure. Once you delete this data,
you should still see data return.

### Stopping Services

Once you complete this tutorial, the resources used for this excercise can be released
by runnning the following command:

```
docker compose down
```

See trademark and other [legal notices](https://trino.io/legal.html).
