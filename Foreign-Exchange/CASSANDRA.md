# Cassandra

## Tables
There are 4 tables created in Cassandra, in the `test` keyspace.

#### `train_data`
This table stores training records.
```sql
CREATE TABLE train_data (
    id bigint PRIMARY KEY,
    avg_bid boolean,
    range_bid boolean,
    diff_bid boolean,
    delta_bid boolean,
    spread boolean,
    label boolean
);
```

#### `test_data`
This table stores testing records.
```sql
CREATE TABLE train_data (
    id bigint PRIMARY KEY,
    avg_bid boolean,
    range_bid boolean,
    diff_bid boolean,
    delta_bid boolean,
    spread boolean,
    label boolean
);
```

#### `validation_perf`
This tables stores performance (accuracy) of validation during training (growing trees). `trees` is number of trees grown at the point.
```sql
CREATE TABLE validation_perf (
    trees int PRIMARY KEY,
    accuracy double
);
```

#### `test_perf`
This table stores performance (accuracy) of testing. `trees` is the total number of trees in the random forest.
```sql
CREATE TABLE test_perf (
    trees int PRIMARY KEY,
    accuracy double
);
```

## Related Methods
In `DataPrep`, added method `csvToCassandra` to transform data from csv to Cassandra. It further called `insertData` to insert records into tables.

In `RandomForest`, added method `readInRecordsFromCassandra` to read in records from Cassandra. Added `createPerformanceTable` to create performance metrics tables, and `insertPerformance` to insert performance metrics into table. 
