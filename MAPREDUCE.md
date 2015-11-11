# MapReduce

Added a new package called `mapreduce` for MapReduce related stuff. This package includes following classes:

### RandomForestMR
The driver class for running MapReduce.

### RandomForestMapper
The mapper class, read records from `Cassandra` and trains a decision tree using randomly selected features and data records.

### RandomForestReducer
The reducer class, receives a collection of json texts of trees, and combine them together to form a json array.

### FakeInputFormat
This class represents a input format that fakes input splits to ensure that the number of mappers equals to the number of decision trees in the random forest.
