# Label and Features

## Label
The label is the directionality label of the “bid” value. It has two values: TRUE, FALSE.

The label is decided in this way: for each record in data file (AKA “current record”), starting from next record, check if the bid value of this record is different from the bid value of current record. If different, if it’s greater, set label as TRUE, else, set label as FALSE. If the bid value is the same with bid value of current record, check next record, do this until a record with different bid value is found.

## Features
There are currently 5 features. They are:

“avg_bid”: 
Average bid in the time window.

“range_bid”: 
Range of bid (max - min) in the time window, showing the level of price change.

“diff_bid”:
Difference between bid values of last record and first record in window.

“delta_bid”:
Difference between bid values of current record and previous record.

“spread”:
Difference between "bid" and "ask" value, or so-called "pip" value.
