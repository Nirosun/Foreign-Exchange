# Random Forest with Spark Pipeline in Scala

Added a new folder called `RandomForestSpark` for the Spark Pipeline random forest project.

## Project Structure
- build.sbt
- project
    - assembly.sbt
- src/main/scala
    - RandomForestSpark.scala

The `RandomForestSpark.scala` contains the `RandomForestSpark` object, and a `main` method, which is the entrance to the program. In this method, it reads data from Cassandra, set up a pipeline with `RandomForestClassifier`, uses `pipeline.fit` to train the model, and uses `model.transform` to make predictions on test data. Finally it saves metrics (accuracy) to Cassandra.

## Performance Metrics
Performance metrics table is called `test_perf`. It has 3 columns: `ts` (timestamp for metrics), `trees` (number of trees in random forest), `accuracy` (test accuracy).

Some sample result is as follows:
```
 ts                       | accuracy | trees
--------------------------+----------+-------
 2015-11-18 21:33:27+0000 |  0.57096 |    50
 2015-11-18 21:32:12+0000 | 0.572222 |    10
```
