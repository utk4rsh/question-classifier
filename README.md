#Question Classification

#Introduction

This project is an attempt to write a SVM based Question Classifier. A lot of feature extraction is motivated by the Paper [Question Classification using Head Words and their Hypernyms](http://www.aclweb.org/anthology/D08-1097)

#Acccuracy

Cross Validation Results:


| Precision |   Recall  | FScore | Gold   | System  | Correct |  Class  |
| --------- |----------:| ------:| ------:| -------:| -------:|--------:|
|0.723	|0.723	|0.723|	5452|	5452|	3942|	OVERALL
|0.837	|0.477	|0.607	|86	|49	|41	|ABBR
|0.597	|0.834	|0.696	|1162	|1624	|969	|DESC
|0.578	|0.577	|0.577	|1250	|1247	|721	|ENTY
|0.832	|0.739	|0.783	|1223	|1087	|904	|HUM
|0.884	|0.721	|0.794	|835	|681	|602	|LOC
|0.923	|0.787	|0.849	|896	|764	|705	|NUM


Holdout Set Results:


| Precision |   Recall  | FScore | Gold   | System  | Correct |  Class  |
| --------- |----------:| ------:| ------:| -------:| -------:|--------:|
|0.726	|0.726	|0.726	|500	|500	|363	|OVERALL
|1.000	|0.778	|0.875	|9	|7	|7	|ABBR
|0.556	|0.978	|0.709	|138	|243	|135	|DESC
|0.667	|0.426	|0.519	|94	|60	|40	|ENTY
|0.887	|0.846	|0.866	|65	|62	|55	|HUM
|0.981	|0.630	|0.767	|81	|52	|51	|LOC
|0.987	|0.664	|0.794	|113	|76	|75	|NUM

