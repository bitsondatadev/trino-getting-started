Let's start up the Druid cluster along with the required Zookeeper and 
PostgreSQL instance. Clone this repository and navigate to the `trino-druid`
directory.
```
git clone git@github.com:bitsondatadev/trino-getting-started.git

cd druid/trino-druid

docker-compose up -d
```

To do batch insert, we can navigate to the Druid Web UI once it has finished
starting up at <http://localhost:8888>. Once that is done, click the "Load data"
button, choose, "Example data", and follow the prompts to create the native
batch ingestion spec. Once the spec is created, run the job and ingest the data.
More information can be found here: <https://druid.apache.org/docs/latest/tutorials/index.html>

Once Druid completes the task, open up a Trino connection and validate that
the `druid` catalog exists.

```
docker exec -it trino-druid_trino-coordinator_1 trino

trino> SHOW CATALOGS;

 Catalog 
---------
 druid   
 system  
 tpcds   
 tpch    
(4 rows)

```

Now show the tables under the `druid.druid` schema.

```
trino> SHOW TABLES IN druid.druid;
   Table   
-----------
 wikipedia 
(1 row)

```

Run a `SHOW CREATE TABLE`  to see the column definitions.

```
trino> SHOW CREATE TABLE druid.druid.wikipedia;
             Create Table             
--------------------------------------
 CREATE TABLE druid.druid.wikipedia ( 
    __time timestamp(3) NOT NULL,     
    added bigint NOT NULL,            
    channel varchar,                  
    cityname varchar,                 
    comment varchar,                  
    commentlength bigint NOT NULL,    
    countryisocode varchar,           
    countryname varchar,              
    deleted bigint NOT NULL,          
    delta bigint NOT NULL,            
    deltabucket bigint NOT NULL,      
    diffurl varchar,                  
    flags varchar,                    
    isanonymous varchar,              
    isminor varchar,                  
    isnew varchar,                    
    isrobot varchar,                  
    isunpatrolled varchar,            
    metrocode varchar,                
    namespace varchar,                
    page varchar,                     
    regionisocode varchar,            
    regionname varchar,               
    user varchar                      
 )                                    
(1 row)
```

Finally, query the first 5 rows of data showing the user and how much they added.

```
trino> SELECT user, added FROM druid.druid.wikipedia LIMIT 5;
      user       | added 
-----------------+-------
 Lsjbot          |    31 
 ワーナー成増    |   125 
 181.230.118.178 |     2 
 JasonAQuest     |     0 
 Kolega2357      |     0 
(5 rows)

```

