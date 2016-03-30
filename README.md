#Question Classification

#Introduction

This project is an attempt to write a SVM based Question Classifier. A lot of feature extraction is motivated by the Paper [Question Classification using Head Words and their Hypernyms](http://www.aclweb.org/anthology/D08-1097)

#Problem DataSet
A typical dataset for this problem would look like the following:

1. NUM:dist How far is it from Denver to Aspen ?
2. LOC:city What county is Modesto , California in ?
3. HUM:desc Who was Galileo ?
4. DESC:def What is an atom ?
5. NUM:date When did Hawaii become a state ?
6. NUM:dist How tall is the Sears Building ?

The first two words separated by ":" denote the class or the category of the question. For simplicity and in this initial attempt, we have chosen only the first word as the category i.e. for data : "HUM:desc Who was Galileo ?" HUM is the category of the question. The second part can again be detected using another classifier (hierarchical classifiers).

Complete dataset is available [here](http://cogcomp.cs.illinois.edu/Data/QA/QC/train_1000.label)
#Implementation

We use SVM based linear classifier to build a model to classify a given question to a correct class.

##Feature Extraction

Following features are used to train the model

1. WH word type (The wh-word feature is the question wh-word in given questions. For example, the wh-word of question What is the population of China is what. We have taken all known question wh-words, namely what, which, when, where, who, how, why, and unknown
2. Head Word extractor after chunking using opennlp parser.
3. ShapeExtractor. Word shape in a given question may be useful for question classification. For instance, the question Who is Duke Ellington has a mixed shape (begins a with capital letter and follows by lower case letters) for Duke, which roughly serves as a named entity recognizer.
4. Wordnet Hypernyms for head word for better regularization of features.
5. N grams of the question text.

#Steps to Execute

1. git clone https://github.com/utk4rsh/question-classifier.git ( do git pull if you have cloned in the past)
2. mvn install:install-file -Dfile=lib/edu.mit.jwi_2.4.0.jar -DgroupId=edu.mit -DartifactId=jwi -Dversion=2.4.0 -Dpackaging=jar
3. mvn clean install
4. mvn exec:java -Dexec.mainClass="us.ml.question.classifier.QuestionCategoryEvaluation" -Dexec.cleanupDaemonThreads=false

This should produce the below accuracy.

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

#Future Work

1. More feature engineering can be done to identify feature apart from the listed ones.
2. Trying with different SVM kernels to see if there are any improvements.
3. See if this could be achieved using word2vec or Neural networks.
