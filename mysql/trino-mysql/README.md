# Running MySQL and configuring the MySQL Connector

## Introduction 
This tutorial will cover running the MySQL connector

## Goals
In this tutorial, you will:
 1. Learn how to run a CTAS (CREATE TABLE AS) statement in Trino.
 2. Run a query against the underlying system.
 
## Steps

### Running Services

First, you want to start the services. Make sure that you are in the 
`trino-getting-started/mysql/trino-mysql` directory. Now run the following
command:

```
docker-compose up -d
```

You should expect to see the following output (you may also have to download
the Docker images before you see the "done" message):

```
Creating network "trino-mysql_trino-network" with driver "bridge"
Creating network "trino-mysql_default" with the default driver
Creating volume "trino-mysql_mysql-storage" with default driver
Creating trino-mysql_trino-coordinator_1 ... done
Creating trino-mysql_mysql_1             ... done
```

### Open Trino CLI

Once this is complete, you can log into the Trino coordinator node. We will
do this by using the [`exec`](https://docs.docker.com/engine/reference/commandline/exec/)
command and run the `trino` CLI executable as the command we run on that
container. Notice the container id is `trino-mysql_trino-coordinator_1` so the
command you will run is:

```
docker container exec -it trino-mysql_trino-coordinator_1 trino
```

When you start this step, you should see the `trino` cursor once the startup
is complete. It should look like this when it is done:
```
trino>
```


To insert data, run a CTAS (CREATE TABLE AS) query that pushes data from one of
the TPC connectors into the hive catalog that points to MySQL. The TPC
connectors generate data on the fly so that we can run simple tests like this.

First, run a command to show the catalogs to see the `tpch` and `mysql` catalogs
since these are what we will use in the CTAS query.

```
SHOW CATALOGS;
```

You should see that the mysql catalog is available. 

### Querying Trino

If you are familiar with MySQL, you are likely to know that MySQL supports a 
two-tiered containment hierarchy, though you may have never known it was called
that. This containment hierarchy refers to databases and tables. The first tier
of the hierarchy are the tables, while the second tier consists of databases. A
database contains multiple tables and therefore two tables can have the same 
name provided they live under a different database. Since Trino has to connect
to multiple databases, it supports a three-tiered containment hierarchy. Rather
than call the second tier as databases, Trino refers to this tier as schemas. So
a database in MySQL is equivalent to a schema in Trino. The third tier that 
allows Trino to distinguish between multiple underlying data sources is called a
catalog. In our case, since the file we provide trino is called 
`etc/catalog/mysql.properties` it automatically names the catalog `mysql` without
the `.properties` file type. 

There was a database created in MySQL on creation of the container by setting the
`MYSQL_DATABASE` envoironment variable to `tiny`. Therefore you should be able
to run the following command and see that there is a `tiny` database, or schema 
as it's referred to by Trino.

```
SHOW SCHEMAS in mysql;
```

Now that we know the name of the schema that will hold our table, we now can create our first table.

Optional: To view your queries run, log into the
[Trino UI](http://localhost:8080) and log in using any username (it doesn't
 matter since no security is set up).

Move the customer data from the tiny generated tpch data into MySQL using a CTAS
query. Run the following query and if you like, watch it running on the Trino UI:

```
CREATE TABLE mysql.tiny.customer
AS SELECT * FROM tpch.tiny.customer;
```

Now there is a table under MySQL, you can query this data by checking the
following.
```
SELECT * FROM mysql.tiny.customer LIMIT 5;
```

The results should look like this:
```
trino> SELECT * FROM mysql.tiny.customer LIMIT 5;
 custkey |        name        |                address                 | nationkey |      phone      | acctbal | mktsegment |
---------+--------------------+----------------------------------------+-----------+-----------------+---------+------------+---------------------------
    1126 | Customer#000001126 | 8J bzLWboPqySAWPgHrl4IK4roBvb          |         8 | 18-898-994-6389 | 3905.97 | AUTOMOBILE | se carefully asymptotes. u
    1127 | Customer#000001127 | nq1w3VhKie4I3ZquEIZuz1 5CWn            |        10 | 20-830-875-6204 | 8631.35 | AUTOMOBILE | endencies. express instruc
    1128 | Customer#000001128 | 72XUL0qb4,NLmfyrtzyJlR0eP              |         0 | 10-392-200-8982 | 8123.99 | BUILDING   | odolites according to the
    1129 | Customer#000001129 | OMEqYv,hhyBAObDjIkoPL03BvuSRw02AuDPVoe |         8 | 18-313-585-9420 | 6020.02 | HOUSEHOLD  | pades affix realms. pendin
    1130 | Customer#000001130 | 60zzrBpFXjvHzyv0WObH3h8LhYbOaRID58e    |        22 | 32-503-721-8203 | 9519.36 | HOUSEHOLD  | s requests nag silently ca
(5 rows)
```

So now you have a basic working Trino and MySQL instance up and running. From
here you can read more about the 
[Trino MySQL Connector](https://trino.io/docs/current/connector/mysql.html) 
to learn more about the capabilities and limitations of this connector.

See trademark and other [legal notices](https://trino.io/legal.html).
