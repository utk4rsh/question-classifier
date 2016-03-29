package us.ml.question.classifier;

import java.io.File;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.opennlp.tools.PosTaggerAnnotator;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.util.ae.UriToDocumentTextAnnotator;
import org.cleartk.util.cr.UriCollectionReader;

import com.lexicalscope.jewel.cli.CliFactory;
import com.lexicalscope.jewel.cli.Option;

import us.ml.question.classifier.annotations.QuestionCategoryAnnotation;

/**
 * Hello world!
 *
 */
public class App {

  public interface Options {
    @Option(longName = "test-dir", description = "Specify the directory containing the documents to label.",
        defaultValue = "data/train/ABBR")
    public File getTestDirectory();

    @Option(longName = "models-dir", description = "specify the directory containing the trained model jar",
        defaultValue = "models/document_classification/models/train_and_test")
    public File getModelsDirectory();
  }

  public static void main(String[] args) throws Exception {
    Options options = CliFactory.parseArguments(Options.class, args);
    CollectionReader reader = UriCollectionReader.getCollectionReaderFromDirectory(options.getTestDirectory(),
        UriCollectionReader.RejectSystemFiles.class, UriCollectionReader.RejectSystemDirectories.class);
    AggregateBuilder builder = new AggregateBuilder();
    builder.add(UriToDocumentTextAnnotator.getDescription());
    builder.add(SentenceAnnotator.getDescription());
    builder.add(TokenAnnotator.getDescription());
    builder.add(PosTaggerAnnotator.getDescription());
    builder.add(DefaultSnowballStemmer.getDescription("English"));
    builder.add(AnalysisEngineFactory.createEngineDescription(QuestionCategoryAnnotator.class,
        CleartkAnnotator.PARAM_IS_TRAINING, false, GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH,
        JarClassifierBuilder.getModelJarFile(options.getModelsDirectory())));
    SimplePipeline.runPipeline(reader, builder.createAggregateDescription(),
        AnalysisEngineFactory.createEngineDescription(PrintClassificationsAnnotator.class));
  }

  static int count = 1;

  public static class PrintClassificationsAnnotator extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      for (QuestionCategoryAnnotation mention : JCasUtil.select(jCas, QuestionCategoryAnnotation.class)) {
        System.out.println(mention.getCategory());
      }
    }

  }

}
