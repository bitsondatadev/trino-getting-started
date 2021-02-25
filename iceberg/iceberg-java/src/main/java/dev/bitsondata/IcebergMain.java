package dev.bitsondata;

import org.apache.iceberg.*;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.hive.HiveCatalog;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.types.Types;

import java.util.Map;


public class IcebergMain
{

    public static void main( String[] args )
    {
        Configuration conf = new Configuration();
        conf.set("hive.metastore.uris", "thrift://localhost:9083");
        conf.set("hive.metastore.warehouse.dir", "s3a://iceberg/");
        conf.set("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        conf.set("javax.jdo.option.ConnectionDriverName", "com.mysql.cj.jdbc.Driver");
        conf.set("javax.jdo.option.ConnectionURL", "jdbc:mysql://localhost:3306/metastore_db");
        conf.set("javax.jdo.option.ConnectionUserName", "admin");
        conf.set("javax.jdo.option.ConnectionPassword", "admin");
        conf.set("fs.s3a.access.key", "minio");
        conf.set("fs.s3a.secret.key", "minio123");
        conf.set("fs.s3a.endpoint", "http://localhost:9000");
        conf.set("fs.s3a.path.style.access", "true");

        Catalog catalog = new HiveCatalog(conf);

        //first, when dealing with hive, we need to make sure the "namespace"
        //is created. Namespace is equivalent to "database" in Hive or
        //"schema" in Trino.

        Namespace namespace = Namespace.of("logging");

        if (((HiveCatalog)catalog).namespaceExists(namespace)) {
            System.out.println(((HiveCatalog)catalog).loadNamespaceMetadata(namespace));
        } else {
            ((HiveCatalog)catalog).createNamespace(namespace, Map.of("location", "s3a://iceberg/logging.db/"));
        }


        System.out.println("namespaces: " + ((HiveCatalog)catalog).listNamespaces());


        Schema schema = new Schema(
                Types.NestedField.required(1, "level", Types.StringType.get()),
                Types.NestedField.required(2, "event_time", Types.TimestampType.withZone()),
                Types.NestedField.required(3, "message", Types.StringType.get()),
                Types.NestedField.optional(4, "call_stack", Types.ListType.ofRequired(5, Types.StringType.get()))
        );

        PartitionSpec spec = PartitionSpec.builderFor(schema)
                .hour("event_time")
                .identity("level")
                .build();

        TableIdentifier name = TableIdentifier.of("logging", "logs");
        Map<String, String> tableProps = Map.of("write.format.default", "orc");

        final Table table;
        if (catalog.tableExists(name)) {
            table = catalog.loadTable(name);
        } else {
            table = catalog.createTable(name, schema, spec, tableProps);
        }

        System.out.println("name: " + table.name());
        System.out.println("______________________________");
        System.out.println("schema: " + table.schema());
        System.out.println("location: " + table.location());
        System.out.println("spec: " + table.spec());
        System.out.println("snapshot: " + table.snapshots());

    }
}
