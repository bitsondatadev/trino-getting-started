# Delta Lake example

This examples showcases the Delta Lake connector. It uses MinIO for
S3-compatible objects storage for Delta Lake and a Hive Metastore Service backed
by a MariaDB instance.

The `delta.properties` catalog is used by Trino to enable querying.

Watch [Trino Community Broadcast episode 34](https://trino.io/episodes/33.html)
for a full demo, and check the show notes for SQL sample queries.