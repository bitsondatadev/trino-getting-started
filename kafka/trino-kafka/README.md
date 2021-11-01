# Running Kafka and using the Kafka Connector

## Introduction 
This tutorial will cover running the Kafka connector

## Goals
In this tutorial, you will:
 1. Learn how to run a CTAS (CREATE TABLE AS) statement in Trino.
 2. Run a query against the underlying system.
 
## Steps

### Running Services

First, you want to start the services. Make sure that you are in the 
`trino-getting-started/kafka/trino-kafka` directory. Now run the following
command:

```
docker-compose up -d
```

You should expect to see the following output (you may also have to download
the Docker images before you see the "done" message):

```
Creating network "trino-kafka_trino-network" with driver "bridge"
Creating trino-kafka_trino-coordinator_1 ... done
Creating kafka-manager                   ... done
Creating zookeeper                       ... done
Creating trino-kafka_kafka_1             ... done
```
### Loading Kafka Data

The first step to is to load data from `kafka-tpch` tool that loads TPCH
data into kafka topics. This tool generates data on the fly so that we can run 
simple tests like this.

```
docker run --rm -ti \
    --network=trino-kafka_trino-network \
    --name kafka-load-tpch-data \
    openjdk:11 \
    sh -c '
    curl -o kafka-tpch https://repo1.maven.org/maven2/de/softwareforge/kafka_tpch_0811/1.0/kafka_tpch_0811-1.0.sh && \
    chmod 755 kafka-tpch && \
    ./kafka-tpch load --brokers host.docker.internal:9092 --prefix tpch. --tpch-type tiny'
```

You should expect to see roughly the following output:
```
2021-05-12T21:41:48.183+0000     INFO   main    io.airlift.log.Logging  Logging to stderr
2021-05-12T21:41:48.198+0000     INFO   main    de.softwareforge.kafka.LoadCommand      Processing tables: [customer, orders, lineitem, part, partsupp, supplier, nation, region]
2021-05-12T21:41:48.477+0000     INFO   pool-1-thread-1 de.softwareforge.kafka.LoadCommand      Loading table 'customer' into topic 'tpch.customer'...
2021-05-12T21:41:48.477+0000     INFO   pool-1-thread-2 de.softwareforge.kafka.LoadCommand      Loading table 'orders' into topic 'tpch.orders'...
2021-05-12T21:41:48.478+0000     INFO   pool-1-thread-4 de.softwareforge.kafka.LoadCommand      Loading table 'part' into topic 'tpch.part'...
2021-05-12T21:41:48.479+0000     INFO   pool-1-thread-6 de.softwareforge.kafka.LoadCommand      Loading table 'supplier' into topic 'tpch.supplier'...
2021-05-12T21:41:48.479+0000     INFO   pool-1-thread-7 de.softwareforge.kafka.LoadCommand      Loading table 'nation' into topic 'tpch.nation'...
2021-05-12T21:41:48.485+0000     INFO   pool-1-thread-3 de.softwareforge.kafka.LoadCommand      Loading table 'lineitem' into topic 'tpch.lineitem'...
2021-05-12T21:41:48.485+0000     INFO   pool-1-thread-5 de.softwareforge.kafka.LoadCommand      Loading table 'partsupp' into topic 'tpch.partsupp'...
2021-05-12T21:41:48.489+0000     INFO   pool-1-thread-8 de.softwareforge.kafka.LoadCommand      Loading table 'region' into topic 'tpch.region'...
2021-05-12T21:41:51.147+0000     INFO   pool-1-thread-8 de.softwareforge.kafka.LoadCommand      Generated 5 rows for table 'region'.
2021-05-12T21:41:51.242+0000     INFO   pool-1-thread-6 de.softwareforge.kafka.LoadCommand      Generated 100 rows for table 'supplier'.
2021-05-12T21:41:51.363+0000     INFO   pool-1-thread-7 de.softwareforge.kafka.LoadCommand      Generated 25 rows for table 'nation'.
2021-05-12T21:41:51.900+0000     INFO   pool-1-thread-1 de.softwareforge.kafka.LoadCommand      Generated 1500 rows for table 'customer'.
2021-05-12T21:41:52.081+0000     INFO   pool-1-thread-4 de.softwareforge.kafka.LoadCommand      Generated 2000 rows for table 'part'.
2021-05-12T21:41:52.526+0000     INFO   pool-1-thread-5 de.softwareforge.kafka.LoadCommand      Generated 8000 rows for table 'partsupp'.
2021-05-12T21:41:53.325+0000     INFO   pool-1-thread-2 de.softwareforge.kafka.LoadCommand      Generated 15000 rows for table 'orders'.
2021-05-12T21:41:54.975+0000     INFO   pool-1-thread-3 de.softwareforge.kafka.LoadCommand      Generated 60175 rows for table 'lineitem'.
```

### Open Trino CLI

Once this is complete, you can log into the Trino coordinator node. We will
do this by using the [`exec`](https://docs.docker.com/engine/reference/commandline/exec/)
command and run the `trino` CLI executable as the command we run on that
container. Notice the container id is `trino-kafka_trino-coordinator_1` so the
command you will run is:

```
docker container exec -it trino-kafka_trino-coordinator_1 trino
```

When you start this step, you should see the `trino` cursor once the startup
is complete. It should look like this when it is done:
```
trino>
```

First, run a command to show the catalogs to see the `kafka` catalog.

```
SHOW CATALOGS;
```

You should see that the `kafka` catalog in the ouptut. Next let's look at the
schemas under the `kafka` catalog.

```
SHOW SCHEMAS in kafka;
```

Which will output:

```
|Schema            |
|------------------|
|information_schema|
|tpch              |
```

Note: There are two meanings we just used when saying the word "schema".
There is the table schema that defines columns of a table, then there is the
SCHEMA that I intentionally put in all caps that signifies the SCHEMA in the
containment hierarchy used by Trino. Trino defines a CATALOG which contains
multiple SCHEMAS, which contain multiple TABLES. 

### Querying Trino

Optional: To view your queries run, log into the 
[Trino UI](http://localhost:8080) and log in using any username (it doesn't
 matter since no security is set up).

Now there is a table under Kafka, you can query this data by running the
following.

```
SELECT custkey, name, nationkey, phone 
FROM kafka.tpch.customer LIMIT 5;

```

The results should look like this:
```
|custkey|name              |nationkey|phone          |
|-------|------------------|---------|---------------|
|1      |Customer#000000001|15       |25-989-741-2988|
|2      |Customer#000000002|13       |23-768-687-3665|
|3      |Customer#000000003|1        |11-719-748-3364|
|4      |Customer#000000004|4        |14-128-190-5944|
|5      |Customer#000000005|3        |13-750-942-6364|
```

So now you have a basic working Trino and Kafka cluster up and running. From
here you can read more about the 
[Trino Kafka Connector](https://trino.io/docs/current/connector/kafka.html) 
to learn more about the capabilities and limitations of this connector.

See trademark and other [legal notices](https://trino.io/legal.html).
