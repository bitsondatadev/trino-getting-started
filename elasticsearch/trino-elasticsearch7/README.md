# Running Elasticsearch and using the Elasticsearch Connector

## Introduction 
This tutorial will cover running the Elasticsearch connector

## Goals
In this tutorial, you will:
 1. Learn how to run a CTAS (CREATE TABLE AS) statement in Trino.
 2. Run a query against the underlying system.
 
## Steps

### Running Services

First, you want to start the services. Make sure that you are in the 
`trino-getting-started/mongo/trino-elasticsearch7` directory. Now run the following
command:

```
docker-compose up -d
```

You should expect to see the following output (you may also have to download
the Docker images before you see the "done" message):

```
Creating network "trino-elasticsearch7_trino-network" with driver "bridge"
Creating trino-mongo_mongodb_1           ... done
Creating trino-mongo_trino-coordinator_1 ... done

```

### Open Trino CLI

Once this is complete, you can log into the Trino coordinator node. We will
do this by using the [`exec`](https://docs.docker.com/engine/reference/commandline/exec/)
command and run the `trino` CLI executable as the command we run on that
container. Notice the container id is `trino-mongo_trino-coordinator_1` so the
command you will run is:

```
docker container exec -it trino-mongo_trino-coordinator_1 trino
```

When you start this step, you should see the `trino` cursor once the startup
is complete. It should look like this when it is done:
```
trino>
```
 
The first step to is to run a CTAS (CREATE TABLE AS) query that pushes data from
one of the TPC connectors into the `mongo` catalog. The TPC connector generates 
data on the fly so that we can run simple tests like this.

First, run a command to show the catalogs to see the `tpch` and `mongo` catalogs
since these are what we will use in the CTAS query.

```
SHOW CATALOGS;
```

You should see that the `mongo` catalog in the ouptut. Next let's look at the
schemas under the `mongo` catalog.

```
SHOW SCHEMAS in mongo;
```

Which will output:

```
|Schema            |
|------------------|
|admin             |
|config            |
|information_schema|
|local             |

```

Note: There are two meanings we just used when saying the word "schema".
There is the table schema that defines columns of a table, then there is the
SCHEMA that I intentionally put in all caps that signifies the SCHEMA in the
containment hierarchy used by Trino. Trino defines a CATALOG which contains
multiple SCHEMAS, which contain multiple TABLES. 


### Querying Trino

One thing to note is that the MongoDB connector does not support creating 
SCHEMAS at the time of writing. For instance if we try to run the command:

```
CREATE SCHEMA mongo.tiny;
```

You will get the following exception thrown.

```
Caused by: io.trino.spi.TrinoException: This connector does not support creating schemas
	at io.trino.spi.connector.ConnectorMetadata.createSchema(ConnectorMetadata.java:250)
	at io.trino.metadata.MetadataManager.createSchema(MetadataManager.java:651)
	at io.trino.execution.CreateSchemaTask.internalExecute(CreateSchemaTask.java:105)
	at io.trino.execution.CreateSchemaTask.execute(CreateSchemaTask.java:72)
	at io.trino.execution.CreateSchemaTask.execute(CreateSchemaTask.java:47)
	at io.trino.execution.DataDefinitionExecution.start(DataDefinitionExecution.java:170)
	at io.trino.execution.SqlQueryManager.createQuery(SqlQueryManager.java:237)
	at io.trino.dispatcher.LocalDispatchQuery.lambda$startExecution$7(LocalDispatchQuery.java:143)
	at io.trino.$gen.Trino_355____20210512_134350_2.run(Unknown Source)
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1128)
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:628)
	at java.base/java.lang.Thread.run(Thread.java:834)

```

For simplicity, lets just use one of the existing Mongo databases (i.e. Trino
SCHEMA) that we listed previously. For this exercise we will use the `local` 
SCHEMA. Using this MongoDB database, we now can create our first collection 
(i.e. Trino TABLE).

Optional: To view your queries run, log into the 
[Trino UI](http://localhost:8080) and log in using any username (it doesn't
 matter since no security is set up).

Move the customer data from the tiny generated tpch data into MongoDB uing a 
CTAS query. Run the following query and if you like, watch it running on the 
Trino UI:

```
CREATE TABLE mongo.local.customer
AS SELECT * FROM tpch.tiny.customer;
```

Now there is a table under MongoDB, you can query this data by running the
following.

```
SELECT custkey, name, nationkey, phone 
FROM mongo.local.customer LIMIT 5;
```

The results should look like this:
```
|custkey|name              |nationkey|phone          |
|-------|------------------|---------|---------------|
|751    |Customer#000000751|0        |10-658-550-2257|
|752    |Customer#000000752|8        |18-924-993-6038|
|753    |Customer#000000753|17       |27-817-126-3646|
|754    |Customer#000000754|0        |10-646-595-5871|
|755    |Customer#000000755|16       |26-395-247-2207|
```

So now you have a basic working Trino and MongoDB cluster up and running. From
here you can read more about the 
[Trino MongoDB Connector](https://trino.io/docs/current/connector/mongodb.html) 
to learn more about the capabilities and limitations of this connector.

See trademark and other [legal notices](https://trino.io/legal.html).
