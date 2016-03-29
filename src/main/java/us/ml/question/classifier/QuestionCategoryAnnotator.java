package us.ml.question.classifier;

import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.CleartkExtractor;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Focus;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Following;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Ngram;
import org.cleartk.ml.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import org.cleartk.ml.feature.extractor.TypePathExtractor;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;

import us.ml.question.classifier.annotations.QuestionCategoryAnnotation;
import us.ml.question.classifier.extractors.HeadWordExtractor;
import us.ml.question.classifier.extractors.ShapeExtractor;
import us.ml.question.classifier.extractors.WHWordExtractor;

public class QuestionCategoryAnnotator extends CleartkAnnotator<String> {

  FeatureExtractor1<Sentence> headWordExtractor;
  FeatureExtractor1<Token> shapeExtractor;
  FeatureExtractor1<Sentence> whWordExtractor;
  CleartkExtractor<Token, Token> ngramExtractor;

  @Override
  public void initialize(UimaContext context) throws ResourceInitializationException {
    super.initialize(context);
    headWordExtractor = new HeadWordExtractor<Sentence>();
    shapeExtractor = new ShapeExtractor<Token>();
    whWordExtractor = new WHWordExtractor<Sentence>();
    ngramExtractor = new CleartkExtractor<Token, Token>(Token.class, new TypePathExtractor<Token>(Token.class, "lemma"),
        new Ngram(new Preceding(1), new Focus(), new Following(1)));
  }

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    Instance<String> instance = new Instance<String>();
    for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
      instance.addAll(headWordExtractor.extract(jCas, sentence));
      instance.addAll(whWordExtractor.extract(jCas, sentence));
      List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);
      for (Token token : tokens) {
        instance.addAll(this.ngramExtractor.extract(jCas, token));
        instance.addAll(this.shapeExtractor.extract(jCas, token));
      }
    }
    if (this.isTraining()) {
      QuestionCategoryAnnotation category = JCasUtil.selectSingle(jCas, QuestionCategoryAnnotation.class);
      instance.setOutcome(category.getCategory());
      this.dataWriter.write(instance);
    } else {
      String outcome = this.classifier.classify(instance.getFeatures());
      QuestionCategoryAnnotation category = new QuestionCategoryAnnotation(jCas, 0, jCas.getDocumentText().length());
      category.setCategory(outcome);
      category.addToIndexes();
    }
  }

}
