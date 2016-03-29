package us.ml.question.classifier.extractors;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;

public class WHWordExtractor<T extends Sentence> implements NamedFeatureExtractor1<T> {

  private static final String FEATURE_NAME = "wh-word";
  private final Set<String> whWords =
      new HashSet<String>(Arrays.asList("what", "which", "when", "where", "who", "how", "why"));


  public List<Feature> extract(JCas jCas, T sentence) throws CleartkExtractorException {
    List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);
    String questionType = tokens.get(0).getCoveredText();
    if (!whWords.contains(questionType.toLowerCase()))
      questionType = "rest";
    Feature feature = new Feature(FEATURE_NAME, questionType.toUpperCase());
    return Collections.singletonList(feature);
  }

  public String getFeatureName() {
    return FEATURE_NAME;
  }

}
