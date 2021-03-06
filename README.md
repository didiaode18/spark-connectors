

# Architecture

- DataSource Plugin
- DAG/Pipeline Running


# Run

Spark数据格式:

- DataFrame(默认无任何前缀, 或者df.)
- SQL(前缀为sql.)
- StructureStream(前缀为ss.)

```
projectRoot="/Users/zhengqh/github/spark-connectors"
configFile="$projectRoot/core/src/main/resources/dag.conf"
mysql="/Users/zhengqh/.m2/repository/mysql/mysql-connector-java/5.1.40/mysql-connector-java-5.1.40.jar"

bin/spark-submit --master local \
--class com.zqh.spark.connectors.client.ConnectorSimpleClient \
--files $configFile --driver-java-options -Dconfig.file=$configFile \
--driver-class-path $mysql \
$projectRoot/core/target/connectors-core-jar-with-dependencies.jar spark
```

# DataSource

- https://docs.databricks.com/spark/latest/data-sources/index.html

create new module, and add dependency, for example, cassandra/avro:

```
<dependency>
    <groupId>com.datastax.spark</groupId>
    <artifactId>spark-cassandra-connector_2.11</artifactId>
    <version>2.0.5</version>
</dependency>
<dependency>
    <groupId>com.databricks</groupId>
    <artifactId>spark-avro_2.11</artifactId>
    <version>3.2.0</version>
</dependency>
```

You don't need to write any code. just package this module

```
cd cassandra
mvn package -Ppro
```

Then use as spark-submit/spark-shell --jars. we add version here for later upgrade

```
--jars connectors-cassandra-2.0.5-jar-with-dependencies.jar,connectors-avro-3.2.0-jar-with-dependencies.jar
```

If you can not find DataSource implement by someone, and you have to write you own plugin.
Then you just follow spark DataSource API. and package the module the same as above. that's all.

# Config

It's a typesafe config.

```
connectors: [
  {
    "readers" : [
      {
        "format": "sql.com.zqh.spark.connectors.test",
        "schema": """
        [{
          "name": "id",
          "type": "int"
        },{
          "name": "name",
          "type": "string"
        },{
          "name": "age",
          "type": "int"
        },{
          "name": "sex",
          "type": "string"
        },{
          "name": "company",
          "type": "string"
        }]
        """,
        "data": """
        [{
          "id": 1,
          "name": "zqh",
          "age": 11,
          "sex": "m",
          "company": "td"
        },{
          "id": 2,
          "name": "lisi",
          "age": 15,
          "sex": "f",
          "company": "td"
        }]
        """
        "outputTable": "test1"
      },{
        "format": "sql.com.zqh.spark.connectors.test",
        "schema": """
        [{
          "name": "company",
          "type": "string"
        },{
          "name": "city",
          "type": "string"
        },{
          "name": "country",
          "type": "string"
        }]
        """,
        "data": """
        [{
          "company": "td",
          "city": "hz",
          "country": "china"
        },{
          "company": "tb",
          "city": "hz",
          "country": "china"
        }]
        """,
        "outputTable": "test2"
      }
    ]
  },{
    "transformers" : [
      {
        "format": "sql",
        "sql": "select * from test1",
        "outputTable": "test3"
      },{
        "format": "sql",
        "sql": "select * from test2",
        "outputTable": "test4"
      },{
        "format": "sql",
        "sql": "select test3.*, test4.city, test4.country from test3 join test4 on test3.company=test4.company",
        "outputTable": "test5"
      }
    ]
  },
  {
    "writers" : [
      {
        "format": "sql.com.zqh.spark.connectors.test",
        "inputTable": "test5"
      }
    ]
  }
]
```

# RoadMap

- [_] Apache Kafka Connect/Kafka Streams/KSQL
- [_] Apache Flink Connectors/Flink SQL
- [_] Apache Storm
- [_] [FiloDB](https://github.com/filodb/FiloDB)
- [_] [SnappyData](http://snappydatainc.github.io/snappydata/)
- [_] [CarbonData](http://carbondata.apache.org/)
- [_] [Spark Druid](https://github.com/SparklineData/spark-druid-olap)

# Tech Used

- [_] https://github.com/sakserv/hadoop-mini-clusters
- xx

