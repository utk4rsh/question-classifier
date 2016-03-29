package us.ml.question.classifier;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.cleartk.util.ViewUriUtil;

import us.ml.question.classifier.annotations.QuestionCategoryAnnotation;

public class GoldQuestionCategoryAnnotator extends JCasAnnotator_ImplBase {

  @Override
  public void process(JCas jCas) throws AnalysisEngineProcessException {
    try {
      JCas uriView = jCas.getView(ViewUriUtil.URI);
      URI uri = new URI(uriView.getSofaDataURI());
      File file = new File(uri.getPath());
      QuestionCategoryAnnotation document = new QuestionCategoryAnnotation(jCas, 0, jCas.getDocumentText().length());
      document.setCategory(file.getParentFile().getName());
      document.addToIndexes();

    } catch (CASException e) {
      throw new AnalysisEngineProcessException(e);
    } catch (URISyntaxException e) {
      throw new AnalysisEngineProcessException(e);
    }
  }
}
