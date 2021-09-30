# Trino on LakeFS using the Hive connector over MinIO file storage

## Introduction 
[LakeFS](https://lakefs.io/) is a layer around object storage like MinIO, that allows you to have 
git-like syntax to move beteween different states of your data. This example extends
the Hive examples by showcasing copying of data into different branches. For more resources,
check out the [Trino Community Broadcast](https://www.youtube.com/watch?v=OzlO1cxYpIY) that
covers Trino + LakeFS.

If you are new to Trino or Presto®, I recommend that you check out the following
blog to get a sense for the Hive connector architecture. [A gentle
 introduction to the Hive connector](https://trino.io/blog/2020/10/20/intro-to-hive-connector.html)

## Goals
In this tutorial, you will:
 1. Learn how to run a CTAS (CREATE TABLE AS) statement in Trino.
 2. Learn the roles of the Trino runtime, metastore, and storage.
 3. Dive into the relational database that contains the Hive model and metadata
    that is stored in the Hive metstore service.
 
## Steps

### Running Services

Start up the LakeFS instance and the required PostgreSQL instance along 
with the typical Trino containers used with the Hive connector. Make sure you
are in `trino-getting-started/lakefs/trino-lakefs-minio/` directory and run 
the following command:

```
docker-compose up -d
```

You should expect to see the following output (you may also have to download
the Docker images before you see the "done" message):

```
Creating network "trino-lakefs-minio_trino-network" with driver "bridge"
Creating volume "trino-lakefs-minio_minio-data" with local driver
Creating postgres                               ... done
Creating trino-lakefs-minio_mariadb_1           ... done
Creating minio                                  ... done
Creating trino-lakefs-minio_trino-coordinator_1 ... done
Creating minio-setup                            ... done
Creating lakefs                                 ... done
Creating trino-lakefs-minio_hive-metastore_1    ... done
Creating lakefs-setup                           ... done
```

### create directories and tables using LakeFS.
Once this is done, you can navigate to the following locations to verify that
everything started correctly. 

1. Navigate to <http://localhost:8000> to open the LakeFS user interface.
2. Log in with Access Key, `AKIAIOSFODNN7EXAMPLE`, and Secret Access Key, 
`wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY`.
3. Verify that the `example` repository exists in the UI and open it.
4. The repository under branch `main` found under `example/main/` should be 
empty.

Once you have verified the repository exists, let's go ahead and create a schema
under the Trino Hive catalog called `minio` that was pointing to `minio` but is
now wrapped by LakeFS to add the git like layer around the file storage. 

Name the schema `tiny` as that is the schema we will copy from the TPCH data 
set. Notice the `location` property of the schema. It now has a namespace that 
is prefixed before the actual `tiny/` table directory. The prefix contains the 
repository name, then the branch name. All together this follows the pattern of 
`<protocol>://<repository>/<branch>/<schema>/`.

```
CREATE SCHEMA minio.tiny
WITH (location = 's3a://example/main/tiny');
```

Now, create two tables, `customer` and  `orders` by setting `external_location`
using the same namespace used in the schema and adding the table name. The data
will pull from the `tiny` TPCH data set.

```
CREATE TABLE minio.tiny.customer
WITH (
  format = 'ORC',
  external_location = 's3a://example/main/tiny/customer/'
) 
AS SELECT * FROM tpch.tiny.customer;

CREATE TABLE minio.tiny.orders
WITH (
  format = 'ORC',
  external_location = 's3a://example/main/tiny/orders/'
) 
AS SELECT * FROM tpch.tiny.orders;

```

Verify that you can see the table directories in LakeFS once they exist.
<http://localhost:8000/repositories/example/objects?ref=main&path=tiny%2F>

Run a query on these two tables using the standard table pointing to the `main`
branch.

```
SELECT ORDERKEY, ORDERDATE, SHIPPRIORITY
FROM minio.tiny.customer c, minio.tiny.orders o
WHERE MKTSEGMENT = 'BUILDING' AND c.CUSTKEY = o.CUSTKEY AND
ORDERDATE < date'1995-03-15'
GROUP BY ORDERKEY, ORDERDATE, SHIPPRIORITY
ORDER BY ORDERDATE;
```

Open the [LakeFS UI again](http://localhost:8000/repositories/example/objects?ref=main) 
and click on the **Unversioned Changes** tab. Click **Commit Changes**. Type a 
commit message on the popup and click **Commit Changes**.

Once the changes are commited on branch `main`, click on the **Branches** tab.
Click **Create Branch**. Name a new branch `sandbox` that branches off of the
`main` branch. Now click **Create**.

Although there is a branch that exists called `sandbox`, this only exists 
logically and we need to make Trino aware by adding another schema and tables 
that point to the new branch. Do this by making a new schema called 
`tiny_sandbox` and changing the `location` property to point to the `sandbox`
branch instead of the `main` branch.

```
CREATE SCHEMA minio.tiny_sandbox
WITH (location = 's3a://example/sandbox/tiny');
```

Once the `tiny_sandbox` schema exists, we'll need to copy the table definitions
of the `customer` and `orders` table from the original tables created. We got
the schema for free by copying it directly from the TPCH data using the CTAS 
statement. We don't want to use CTAS in this case as it not only copies the 
table definition, but also the data. This duplication of data is unnecessary and
is what creating a branch in LakeFS avoids. We want to just copy the table
definition using the `SHOW CREATE TABLE` statement.

```
SHOW CREATE TABLE minio.tiny.customer;
SHOW CREATE TABLE minio.tiny.orders;
```
Take the output and update the schema to `tiny_sandbox` and `external_location`
to point to `sandbox` for both tables.

```
CREATE TABLE minio.tiny_sandbox.customer (
   custkey bigint,
   name varchar(25),
   address varchar(40),
   nationkey bigint,
   phone varchar(15),
   acctbal double,
   mktsegment varchar(10),
   comment varchar(117)
)
WITH (
   external_location = 's3a://example/sandbox/tiny/customer',
   format = 'ORC'
);

CREATE TABLE minio.tiny_sandbox.orders (
   orderkey bigint,
   custkey bigint,
   orderstatus varchar(1),
   totalprice double,
   orderdate date,
   orderpriority varchar(15),
   clerk varchar(15),
   shippriority integer,
   comment varchar(79)
)
WITH (
   external_location = 's3a://example/sandbox/tiny/orders',
   format = 'ORC'
);
```

Once these table definitions exist, go ahead and run the same query as before,
but update using the `tiny_sandbox` schema instead of the `tiny` schema.

```
SELECT ORDERKEY, ORDERDATE, SHIPPRIORITY
FROM minio.tiny_sandbox.customer c, minio.tiny_sandbox.orders o
WHERE MKTSEGMENT = 'BUILDING' AND c.CUSTKEY = o.CUSTKEY AND
ORDERDATE < date'1995-03-15'
ORDER BY ORDERDATE;
```

One last bit of functionality we want to test is the merging capabilities. To
do this, create a table called `lineitem` in the `sandbox` branch using a CTAS
statement.

```
CREATE TABLE minio.tiny_sandbox.lineitem
WITH (
  format = 'ORC',
  external_location = 's3a://example/sandbox/tiny/lineitem/'
) 
AS SELECT * FROM tpch.tiny.lineitem;
```

Verify that you can see three table directories in LakeFS including `lineitem` 
in the `sandbox` branch.
<http://localhost:8000/repositories/example/objects?ref=sandbox&path=tiny%2F>

Verify that you do not see `lineitem` in the table directories in LakeFS in the 
`main` branch.
<http://localhost:8000/repositories/example/objects?ref=main&path=tiny%2F>

You can also verify this by running queries against `lineitem` in the tables
pointing to the `sandbox` branch that should fail on the tables pointing to the
`main` branch.

To merge the new table `lineitem` to show up in the main branch, first commit 
the new change to `sandbox` by again going to **Unversioned Changes** tab. 
Click **Commit Changes**. Type a commit message on the popup and click 
**Commit Changes**.

Once the `lineitem` add is committed, click on the **Compare** tab. Set the
base branch to `main` and the compared to branch to `sandbox`. You should see
the addition of a line item show up in the diff view. Click **Merge** and click
**Yes**.

Once this is merged you should see the table data show up in LakeFS. Verify that
you can see `lineitem` in the table directories in LakeFS in the `main` branch.
<http://localhost:8000/repositories/example/objects?ref=main&path=tiny%2F>

As before, we won't be able to query this data from Trino until we run the
`SHOW CREATE TABLE` from the `tiny_sandbox` schema and use the output to create
the table in the `tiny` schema that is pointing to `main`. 

See trademark and other [legal notices](https://trino.io/legal.html).
