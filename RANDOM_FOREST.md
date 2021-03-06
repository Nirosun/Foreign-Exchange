# Random Forest

## Output
In this run, it grew 50 trees for the random forest, calculated the performance statistics (error rate and accuracy) of the forest after each tree was added into the forest.

Then, the serialization/deserialization methods (using java object streams) for RandomForest objects were tested.

Finally, it tested the performance of the grown random forest on the test file.

The output is as follows:

```
Training:
1 trees, error rate: 0.4091860608299471, accuracy: 0.5908139391700529
2 trees, error rate: 0.406455617628802, accuracy: 0.593544382371198
3 trees, error rate: 0.4026134122287968, accuracy: 0.5973865877712032
4 trees, error rate: 0.406126968503937, accuracy: 0.593873031496063
5 trees, error rate: 0.40840543881334984, accuracy: 0.5915945611866502
6 trees, error rate: 0.4045717094752366, accuracy: 0.5954282905247634
7 trees, error rate: 0.40225890529973934, accuracy: 0.5977410947002606
8 trees, error rate: 0.40496480177843647, accuracy: 0.5950351982215636
9 trees, error rate: 0.40483831152801775, accuracy: 0.5951616884719823
10 trees, error rate: 0.40364711680630855, accuracy: 0.5963528831936915
11 trees, error rate: 0.4038296305388487, accuracy: 0.5961703694611513
12 trees, error rate: 0.41102726387536515, accuracy: 0.5889727361246349
13 trees, error rate: 0.40226071999017077, accuracy: 0.5977392800098292
14 trees, error rate: 0.41260462826193994, accuracy: 0.58739537173806
15 trees, error rate: 0.4132434089515635, accuracy: 0.5867565910484365
16 trees, error rate: 0.4093926727317433, accuracy: 0.5906073272682567
17 trees, error rate: 0.4138482351494281, accuracy: 0.5861517648505719
18 trees, error rate: 0.39749416533595383, accuracy: 0.6025058346640462
19 trees, error rate: 0.4059635830380056, accuracy: 0.5940364169619944
20 trees, error rate: 0.4144809752493535, accuracy: 0.5855190247506465
21 trees, error rate: 0.4044526901669759, accuracy: 0.5955473098330242
22 trees, error rate: 0.4140056713105659, accuracy: 0.585994328689434
23 trees, error rate: 0.40286702916460704, accuracy: 0.597132970835393
24 trees, error rate: 0.41110975162119173, accuracy: 0.5888902483788083
25 trees, error rate: 0.4122720551996057, accuracy: 0.5877279448003943
26 trees, error rate: 0.41502417255485313, accuracy: 0.5849758274451469
27 trees, error rate: 0.40363368524429166, accuracy: 0.5963663147557083
28 trees, error rate: 0.4075946245838984, accuracy: 0.5924053754161016
29 trees, error rate: 0.40916349342832575, accuracy: 0.5908365065716743
30 trees, error rate: 0.4094042343673067, accuracy: 0.5905957656326932

Serializing forest into file...

Deserializing forest from file...

Testing:
Test error rate: 0.4308080808080808, accuracy: 0.5691919191919192

Time elapsed: 692 ms
```

There were 5 features in total, and for building each tree, 2 features were used.

For comparison, if applying a single decision tree with all features, the performance would be:

```
Test error: 0.4313131313131313, accuracy: 0.5686868686868687
```

## Analysis
From the results, we can see that the performance of using random forest is almost same with using a single decision tree. Possible reason is that:

The distribution of the training label is: 42% true, 58% false. For most of the small trees, because their features were not distinguishable enough, their decisions were dominated by the prior of the label. Thus, they always voted FALSE. Only for trees using distinguishable features (“diff_bid” and “delta_bid”), they would vote for TRUE sometime. Because the number of trees voting for TRUE was always less than the number of trees voting for FALSE, the majority vote result of the forest would always be FALSE. Thus the accuracy was 0.57, similar to proportion of FALSE labels.

To solve this problem, in following assignments, I should add more distinguishable/useful features (e.g. features extracted from other currencies), and remove some unuseful features, to improve the accuracies of trees, and make the majority vote result of the forest more accurate.
