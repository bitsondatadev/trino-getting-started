To load the data, we're going to use a simple batch import, but you can also 
[insert the data in a stream](https://docs.pinot.apache.org/basics/data-import/upsert)
using [Kafka](https://kafka.apache.org/).

Let's start up the Pinot cluster along with the required Zookeeper and Kafka
broker. Clone this repository and navigate
```
git clone git@github.com:bitsondatadev/trino-getting-started.git

cd pinot/trino-pinot

docker-compose up -d
```

**NOTE: There is an issue where zookeeper isn't completely ready before other
containers start to load. You will likely need to run `docker-compose up -d`
a second time until I get the fix in.**

To do batch insert, we will stage a csv file to read the data in. Create a 
directory underneath a temp folder locally and then submit this to Pinot.

```
mkdir -p /tmp/pinot-quick-start/rawdata

echo "studentID,firstName,lastName,gender,subject,score,timestampInEpoch
200,Lucy,Smith,Female,Maths,3.8,1570863600000
200,Lucy,Smith,Female,English,3.5,1571036400000
201,Bob,King,Male,Maths,3.2,1571900400000
202,Nick,Young,Male,Physics,3.6,1572418800000" > /tmp/pinot-quick-start/rawdata/transcript.csv
```

In order for Pinot to understand the csv data, we must provide it a 
[schema](https://docs.pinot.apache.org/configuration-reference/schema).

```
echo "{
    \"schemaName\": \"transcript\",
    \"dimensionFieldSpecs\": [
      {
        \"name\": \"studentID\",
        \"dataType\": \"INT\"
      },
      {
        \"name\": \"firstName\",
        \"dataType\": \"STRING\"
      },
      {
        \"name\": \"lastName\",
        \"dataType\": \"STRING\"
      },
      {
        \"name\": \"gender\",
        \"dataType\": \"STRING\"
      },
      {
        \"name\": \"subject\",
        \"dataType\": \"STRING\"
      }
    ],
    \"metricFieldSpecs\": [
      {
        \"name\": \"score\",
        \"dataType\": \"FLOAT\"
      }
    ],
    \"dateTimeFieldSpecs\": [{
      \"name\": \"timestampInEpoch\",
      \"dataType\": \"LONG\",
      \"format\" : \"1:MILLISECONDS:EPOCH\",
      \"granularity\": \"1:MILLISECONDS\"
    }]
}" > /tmp/pinot-quick-start/transcript-schema.json
```

Now we are almost ready to create the [table](https://docs.pinot.apache.org/basics/components/table). 
Instead of adding table configurations as part of the SQL command, Pinot enables
you to store table configurations as a file. This is a nice option that
decouples the DDL which makes for simpler scripting in batch setups.

```
echo "{
    \"tableName\": \"transcript\",
    \"segmentsConfig\" : {
      \"timeColumnName\": \"timestampInEpoch\",
      \"timeType\": \"MILLISECONDS\",
      \"replication\" : \"1\",
      \"schemaName\" : \"transcript\"
    },
    \"tableIndexConfig\" : {
      \"invertedIndexColumns\" : [],
      \"loadMode\"  : \"MMAP\"
    },
    \"tenants\" : {
      \"broker\":\"DefaultTenant\",
      \"server\":\"DefaultTenant\"
    },
    \"tableType\":\"OFFLINE\",
    \"metadata\": {}
}" > /tmp/pinot-quick-start/transcript-table-offline.json
```

Once you've created these 3 files and verify that docker containers are running,
we can now run the `Add Table` command 

```
docker run --rm -ti \
    --network=trino-pinot_trino-network \
    -v /tmp/pinot-quick-start:/tmp/pinot-quick-start \
    --name pinot-batch-table-creation \
    apachepinot/pinot:latest AddTable \
    -schemaFile /tmp/pinot-quick-start/transcript-schema.json \
    -tableConfigFile /tmp/pinot-quick-start/transcript-table-offline.json \
    -controllerHost pinot-controller \
    -controllerPort 9000 -exec
```

Now, we've created a table, we can actually see our table exists in the 
[Pinot web ui](http://localhost:9000/#/tables). Now we want to actually want to insert data 
using a batch job specification. 

```
echo "executionFrameworkSpec:
  name: 'standalone'
  segmentGenerationJobRunnerClassName: 'org.apache.pinot.plugin.ingestion.batch.standalone.SegmentGenerationJobRunner'
  segmentTarPushJobRunnerClassName: 'org.apache.pinot.plugin.ingestion.batch.standalone.SegmentTarPushJobRunner'
  segmentUriPushJobRunnerClassName: 'org.apache.pinot.plugin.ingestion.batch.standalone.SegmentUriPushJobRunner'
jobType: SegmentCreationAndTarPush
inputDirURI: '/tmp/pinot-quick-start/rawdata/'
includeFileNamePattern: 'glob:**/*.csv'
outputDirURI: '/tmp/pinot-quick-start/segments/'
overwriteOutput: true
pinotFSSpecs:
  - scheme: file
    className: org.apache.pinot.spi.filesystem.LocalPinotFS
recordReaderSpec:
  dataFormat: 'csv'
  className: 'org.apache.pinot.plugin.inputformat.csv.CSVRecordReader'
  configClassName: 'org.apache.pinot.plugin.inputformat.csv.CSVRecordReaderConfig'
tableSpec:
  tableName: 'transcript'
  schemaURI: 'http://pinot-controller:9000/tables/transcript/schema'
  tableConfigURI: 'http://pinot-controller:9000/tables/transcript'
pinotClusterSpecs:
  - controllerURI: 'http://pinot-controller:9000'" > /tmp/pinot-quick-start/docker-job-spec.yml
```
Now run this batch job by running the `LaunchDataIngestionJob` task.
```
docker run --rm -ti \
    --network=trino-pinot_trino-network \
    -v /tmp/pinot-quick-start:/tmp/pinot-quick-start \
    --name pinot-data-ingestion-job \
    apachepinot/pinot:latest LaunchDataIngestionJob \
    -jobSpecFile /tmp/pinot-quick-start/docker-job-spec.yml
```


This demo was modified from the tutorials available on the Pinot website:
 - <https://docs.pinot.apache.org/basics/getting-started/pushing-your-data-to-pinot>
 - <https://docs.pinot.apache.org/basics/getting-started/running-pinot-in-docker>
