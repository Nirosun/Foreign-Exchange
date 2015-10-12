The structure of this project is like:
- dtree
	- DecisionTree.java
	- Driver.java
- prep
	- DataPrep.java
	- ProcessedDataRecord.java
	- RawDataRecord.java
- forest
	- RandomForest.java
	- Driver.java

Package forest is for random forest.
Package dtree is for decision tree.
Package prep is for data preparation.

The forest package contains 2 classes:

RandomForest:

Driver:
This class is for illustrating the use of the RandomForest class, including training a random forest, serialize/deserialize the RandomForest instance, and test the performance of the random forest.

The dtree package contains 2 classes:

DecisionTree:
This class represents a decision tree, and provides methods to train and test based on a decision tree model. This class has an inner class “Node” representing a tree node.

Driver:
This class is for illustrating use of DecisionTree class.

The prep package contains 3 classes:

DataPrep:
This class is responsible for prepare data for analysis. It has methods to read raw data records from file, process data, and write labeled data into a new cvs file. This class uses following 2 data classes. 

RawDataRecord:
This class represents a raw data record.

ProcessedDataRecord:
This class represents a processed (labeled) data record. Its attributes represent features.