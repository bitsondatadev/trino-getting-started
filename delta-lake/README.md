# Delta Lake example

This examples showcases the Delta Lake connector. It uses MinIO for
S3-compatible objects storage for Delta Lake and a Hive Metastore Service backed
by a MariaDB instance.

The following files are included:

* `docker-conmpose.yml`, configuration file for docker-compose to start all
  needed containers.
* `conf/metastore-site.xml`, configuration file for the Hive Metastore Service
* `etc/` - various configuration files for Trino
* `etc/catalog/delta.properties`, configuration file for the catalog in Trino
  that allows querying the Delta Lake storage.

Watch [Trino Community Broadcast episode 34](https://trino.io/episodes/34.html)
for a full demo, and check the show notes for SQL sample queries.

## Step by step

Start the Docker containers:

```
docker-compose up -d
```

Create a bucket in MinIO with the following steps:

1. Open a browser to http://<host>:9001, if running on your local machine
   [http://localhost:9001](http://localhost:9001)
2. Log in with username `minio` and password `minio123`, see `docker-compose.yml`
3. Click on **Create Bucket** near the top right.
4. Enter `claudiustestbucket` in the **Bucket Name** field. If you use a
   different name make sure to use that as the location when you use
   `CREATE SCHEMA`.
5. Click on **Create Bucket** in the lower right to confirm.

Connect with your SQL client of choice. The following steps are applicable with
[DBeaver](https://dbeaver.io/download).

1. Download and install DBeaver.
2. Select **Database->New Database Connection** from the menu.
3. Select the Trino driver and click **next**.
4. This demo is set up without authentication.
5. Put anything in the **Username** field and leave the **Password** field
   blank.
6. Open the SQL console and run the command to create a schema.

```
CREATE SCHEMA delta.myschema
WITH (location=’s3a://claudiustestbucket/myschema');
```
Remember to change the location if you created the bucket under a different name.

From here you can follow along with the demo, or try your own SQL queries.
