package us.ml.question.classifier;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.component.ViewCreatorAnnotator;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.testing.util.HideOutput;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.eval.AnnotationStatistics;
import org.cleartk.eval.Evaluation_ImplBase;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.ml.jar.Train;
import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
import org.cleartk.opennlp.tools.PosTaggerAnnotator;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import com.google.common.base.Function;
import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import us.ml.question.classifier.annotations.QuestionCategoryAnnotation;

public class QuestionCategoryEvaluation extends Evaluation_ImplBase<File, AnnotationStatistics<String>> {

  public interface Options {
    @Option(longName = "train-dir", description = "Specify the directory containing the training documents. ",
        defaultValue = "data/train")
    public File getTrainDirectory();

    @Option(longName = "test-dir",
        description = "Specify the directory containing the test (aka holdout/validation) documents.  ",
        defaultValue = "data/test")
    public File getTestDirectory();

    @Option(longName = "models-dir",
        description = "specify the directory in which to write out the trained model files",
        defaultValue = "models/document_classification/models")
    public File getModelsDirectory();

    @Option(longName = "training-args",
        description = "specify training arguments to be passed to the learner.  For multiple values specify -ta for each - e.g. '-ta -t -ta 0'",
        defaultValue = {"-c", "2.0", "-s", "0", "-t", "0"})
    public List<String> getTrainingArguments();
  }

  public static List<File> getFilesFromDirectory(File directory) {
    IOFileFilter fileFilter = FileFilterUtils.makeSVNAware(HiddenFileFilter.VISIBLE);
    IOFileFilter dirFilter = FileFilterUtils
        .makeSVNAware(FileFilterUtils.and(FileFilterUtils.directoryFileFilter(), HiddenFileFilter.VISIBLE));
    return new ArrayList<File>(FileUtils.listFiles(directory, fileFilter, dirFilter));
  }

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);

    List<File> trainFiles = getFilesFromDirectory(options.getTrainDirectory());
    List<File> testFiles = getFilesFromDirectory(options.getTestDirectory());

    QuestionCategoryEvaluation evaluation =
        new QuestionCategoryEvaluation(options.getModelsDirectory(), options.getTrainingArguments());

    // Run Cross Validation
    List<AnnotationStatistics<String>> foldStats = evaluation.crossValidation(trainFiles, 2);
    AnnotationStatistics<String> crossValidationStats = AnnotationStatistics.addAll(foldStats);

    System.err.println("Cross Validation Results:");
    System.err.print(crossValidationStats);
    System.err.println();
    System.err.println(crossValidationStats.confusions());
    System.err.println();

    // Run Holdout Set
    AnnotationStatistics<String> holdoutStats = evaluation.trainAndTest(trainFiles, testFiles);
    System.err.println("Holdout Set Results:");
    System.err.print(holdoutStats);
    System.err.println();
    System.err.println(holdoutStats.confusions());
  }

  public static final String GOLD_VIEW_NAME = "DocumentClassificationGoldView";

  public static final String SYSTEM_VIEW_NAME = CAS.NAME_DEFAULT_SOFA;

  private List<String> trainingArguments;

  public QuestionCategoryEvaluation(File baseDirectory) {
    super(baseDirectory);
    this.trainingArguments = Arrays.<String>asList();
  }

  public QuestionCategoryEvaluation(File baseDirectory, List<String> trainingArguments) {
    super(baseDirectory);
    this.trainingArguments = trainingArguments;
  }

  @Override
  protected CollectionReader getCollectionReader(List<File> items) throws Exception {
    return UriCollectionReader.getCollectionReaderFromFiles(items);
  }

  @Override
  public void train(CollectionReader collectionReader, File outputDirectory) throws Exception {
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(UriToDocumentTextAnnotator.getDescription());
    builder.add(SentenceAnnotator.getDescription());
    builder.add(TokenAnnotator.getDescription());
    builder.add(PosTaggerAnnotator.getDescription());
    builder.add(DefaultSnowballStemmer.getDescription("English"));
    builder.add(AnalysisEngineFactory.createEngineDescription(GoldQuestionCategoryAnnotator.class));
    AnalysisEngineDescription documentClassificationAnnotator = AnalysisEngineFactory.createEngineDescription(
        QuestionCategoryAnnotator.class, CleartkAnnotator.PARAM_IS_TRAINING, true,
        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY, outputDirectory,
        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, LibSvmStringOutcomeDataWriter.class.getName());
    builder.add(documentClassificationAnnotator);
    SimplePipeline.runPipeline(collectionReader, builder.createAggregateDescription());
    System.err.println("Train model and write model.jar file.");
    HideOutput hider = new HideOutput();
    Train.main(outputDirectory, this.trainingArguments.toArray(new String[this.trainingArguments.size()]));
    hider.restoreOutput();
  }

  @Override
  protected AnnotationStatistics<String> test(CollectionReader collectionReader, File directory) throws Exception {
    AnnotationStatistics<String> stats = new AnnotationStatistics<String>();

    AggregateBuilder builder = new AggregateBuilder();

    final String defaultViewName = CAS.NAME_DEFAULT_SOFA;
    final String goldViewName = "GoldView";
    builder.add(AnalysisEngineFactory.createEngineDescription(ViewCreatorAnnotator.class,
        ViewCreatorAnnotator.PARAM_VIEW_NAME, goldViewName));
    builder.add(UriToDocumentTextAnnotator.getDescription(), defaultViewName, goldViewName);
    builder.add(AnalysisEngineFactory.createEngineDescription(GoldQuestionCategoryAnnotator.class), defaultViewName,
        goldViewName);
    builder.add(UriToDocumentTextAnnotator.getDescription());
    builder.add(SentenceAnnotator.getDescription());
    builder.add(TokenAnnotator.getDescription());
    builder.add(PosTaggerAnnotator.getDescription());
    builder.add(DefaultSnowballStemmer.getDescription("English"));
    AnalysisEngineDescription documentClassificationAnnotator = AnalysisEngineFactory.createEngineDescription(
        QuestionCategoryAnnotator.class, CleartkAnnotator.PARAM_IS_TRAINING, false,
        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, JarClassifierBuilder.getModelJarFile(directory));
    builder.add(documentClassificationAnnotator);

    AnalysisEngine engine = builder.createAggregate();

    // Run and evaluate
    Function<QuestionCategoryAnnotation, ?> getSpan = AnnotationStatistics.annotationToSpan();
    Function<QuestionCategoryAnnotation, String> getCategory =
        AnnotationStatistics.annotationToFeatureValue("category");
    JCasIterator iter = new JCasIterator(collectionReader, engine);
    while (iter.hasNext()) {
      JCas jCas = iter.next();
      JCas goldView = jCas.getView(goldViewName);
      JCas systemView = jCas.getView(defaultViewName);
      // Get results from system and gold views, and update results accordingly
      Collection<QuestionCategoryAnnotation> goldCategories =
          JCasUtil.select(goldView, QuestionCategoryAnnotation.class);
      Collection<QuestionCategoryAnnotation> systemCategories =
          JCasUtil.select(systemView, QuestionCategoryAnnotation.class);
      stats.add(goldCategories, systemCategories, getSpan, getCategory);
    }

    return stats;
  }
}
