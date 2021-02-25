The steps in this demo are adapted from the Amundsen [installation page](https://www.amundsen.io/amundsen/installation/)
and  Clone this repository and navigate to the `trino-getting-started/amundsen` 
directory. For this demo you will need at least 3GB of memory allocated to your 
Docker application. 

```
git clone git@github.com:bitsondatadev/trino-getting-started.git

cd amundsen

docker-compose up -d
```

Once all the services are running clone the Amundsen repository in a seperate
terminal and navigate to the databuilder folder and install all the dependencies:

```
git clone --recursive https://github.com/amundsen-io/amundsen.git
cd databuilder
python3 -m venv venv
source venv/bin/activate
pip3 install --upgrade pip
pip3 install -r requirements.txt
python3 setup.py install
```
Navigate to MinIO at <http://localhost:9000> to create the `tiny` bucket for the
schema in Trino to map to. In Trino, create a schema and a couple tables in the 
existing `minio` catalog:

```
CREATE SCHEMA minio.tiny
WITH (location = 's3a://tiny/');

CREATE TABLE minio.tiny.customer
WITH (
  format = 'ORC',
  external_location = 's3a://tiny/customer/'
) 
AS SELECT * FROM tpch.tiny.customer;

CREATE TABLE minio.tiny.orders
WITH (
  format = 'ORC',
  external_location = 's3a://tiny/orders/'
) 
AS SELECT * FROM tpch.tiny.orders;
```

Navigate back to the `trino-getting-started/amundsen` directory in the same 
python virtual environment you just opened. 

```
cd trino-getting-started/amundsen
python3 assets/scripts/sample_trino_data_loader.py
```

View UI at <http://localhost:5000> and try to search test, it should return the
tables we just created. 

You can verify dummy data has been ingested into Neo4j by visiting <http://localhost:7474/browser/>,
log in as `neo4j` with the `test` password and run 
`MATCH (n:Table) RETURN n LIMIT 25` in the query box. You should see few tables.

If you have any issues, look at some of the [troubleshooting steps](https://www.amundsen.io/amundsen/installation/#troubleshooting)
in the Amundsen installation page.
