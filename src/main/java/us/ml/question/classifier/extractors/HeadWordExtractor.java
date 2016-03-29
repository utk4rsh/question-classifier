package us.ml.question.classifier.extractors;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

public class HeadWordExtractor<T extends Sentence> implements NamedFeatureExtractor1<T> {
  static ChunkerModel model;
  static ChunkerME chunker;
  static {
    try {
      model = new ChunkerModel(HeadWordExtractor.class.getClassLoader().getResourceAsStream("en_chunker.bin"));
      chunker = new ChunkerME(model);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }
  private static final String FEATURE_NAME = "head-word";
  private final Set<String> prepositions = new HashSet<String>(Arrays.asList("about", "above", "across", "against",
      "amid", "around", "at", "atop", "behind", "below", "beneath", "beside", "between", "beyond", "by", "for", "from",
      "down", "in", "including", "inside", "into", "mid", "near", "of", "off", "on", "onto", "opposite", "out",
      "outside", "over", "round", "through", "throughout", "to", "under", "underneath", "with", "within", "without"));

  public List<Feature> extract(JCas jCas, T sentence) throws CleartkExtractorException {
    List<Feature> features = new ArrayList<Feature>();
    List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);
    String questionType = tokens.get(0).getCoveredText();
    if (questionType.equalsIgnoreCase("when") || questionType.equalsIgnoreCase("why")) {
      features.add(new Feature(FEATURE_NAME, "NULL"));
    } else if (questionType.equalsIgnoreCase("where") && sentence.getCoveredText().contains("come from")) {
      features.add(new Feature(FEATURE_NAME, "DESC"));
    } else if (questionType.equalsIgnoreCase("how")) {
      features.add(new Feature(FEATURE_NAME, tokens.get(1).getCoveredText().toUpperCase()));
    } else if (questionType.equalsIgnoreCase("what")) {
      Feature feature = parseAndGetFeatureForWhat(jCas, sentence);
      if (feature.getValue().equals(new String("UNKNOWN"))) {
        features = extractHeadWord(jCas, sentence);
      } else {
        features.add(feature);
      }
    } else if (questionType.equalsIgnoreCase("who")
        && (tokens.get(1).getCoveredText().equalsIgnoreCase("is")
            || tokens.get(1).getCoveredText().equalsIgnoreCase("was"))
        && Character.isUpperCase(tokens.get(1).getCoveredText().charAt(0))) {
      features.add(new Feature(FEATURE_NAME, "DESC"));
    } else {
      features = extractHeadWord(jCas, sentence);
    }
    return features;
  }

  private Feature parseAndGetFeatureForWhat(JCas jCas, T sentence) {
    Feature feature = new Feature(FEATURE_NAME, "UNKNOWN");
    List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, sentence);
    if ((tokens.get(1).getCoveredText().equalsIgnoreCase("is")
        || tokens.get(1).getCoveredText().equalsIgnoreCase("are"))
        && (tokens.get(2).getCoveredText().equalsIgnoreCase("a")
            || tokens.get(2).getCoveredText().equalsIgnoreCase("an")
            || tokens.get(2).getCoveredText().equalsIgnoreCase("the"))) {
      feature = new Feature(FEATURE_NAME, "DESC");
    } else if (tokens.get(1).getCoveredText().equalsIgnoreCase("do")
        || tokens.get(1).getCoveredText().equalsIgnoreCase("does")
            && (tokens.get(tokens.size() - 1).getCoveredText().equalsIgnoreCase("mean"))) {
      feature = new Feature(FEATURE_NAME, "DESC");
    } else if (tokens.get(1).getCoveredText().equalsIgnoreCase("does")
        && (tokens.get(tokens.size() - 1).getCoveredText().equalsIgnoreCase("do"))) {
      feature = new Feature(FEATURE_NAME, "DESC");
    } else if ((tokens.get(1).getCoveredText().equalsIgnoreCase("do")
        || (tokens.get(1).getCoveredText().equalsIgnoreCase("does")) && (sentence.getCoveredText().contains("stand for")
            || sentence.getCoveredText().contains("abbreviation") || sentence.getCoveredText().contains("mean")))) {
      feature = new Feature(FEATURE_NAME, "ABBR");
    } else if ((tokens.get(1).getCoveredText().equalsIgnoreCase("is")
        || (tokens.get(1).getCoveredText().equalsIgnoreCase("are"))
            && sentence.getCoveredText().contains("used for"))) {
      feature = new Feature(FEATURE_NAME, "DESC");
    } else if (sentence.getCoveredText().toLowerCase().contains("what do you call")) {
      feature = new Feature(FEATURE_NAME, "ENTY");
    } else if (sentence.getCoveredText().toLowerCase().contains("number of")
        || sentence.getCoveredText().toLowerCase().contains("many")) {
      feature = new Feature(FEATURE_NAME, "NUM");
    }
    return feature;
  }

  public String getFeatureName() {
    return FEATURE_NAME;
  }


  /**
   * Add phrase chunks and POS tags to a sentence
   * 
   * @return
   * 
   * @throws IOException
   * @throws InvalidFormatException
   */
  private List<Feature> extractHeadWord(JCas jCas, T sentence) {
    List<Token> tokenList = JCasUtil.selectCovered(jCas, Token.class, sentence);
    List<Feature> features = new ArrayList<Feature>();
    try {
      String[] tokens = new String[tokenList.size()];
      String[] posTags = new String[tokenList.size()];
      int ix = 0;
      for (Token token : tokenList) {
        tokens[ix] = token.getCoveredText();
        posTags[ix] = token.getPos();
        ix++;
      }

      Span[] result = chunker.chunkAsSpans(tokens, posTags);
      for (int i = 0; i < result.length; i++) {
        if (result[i].getType().startsWith("NP")) {
          List<Token> constituentWords = new ArrayList<Token>();
          for (Token word : JCasUtil.selectCovered(jCas, Token.class, tokenList.get(result[i].getStart()).getBegin(),
              tokenList.get(result[i].getEnd() - 1).getEnd())) {
            constituentWords.add(word);
          }
          int headWordId = constituentWords.size() - 1;
          for (int a = constituentWords.size() - 2; a > 1; a--) {
            Token wtA = constituentWords.get(a);
            if ("IN".equals(wtA.getPos()) || ",".equals(wtA.getPos()) || prepositions.contains(wtA.getCoveredText())) {
              headWordId = a - 1;
            } else {
              headWordId = a;
              break;
            }
          }
          Token headWordToken = constituentWords.get(headWordId);
          String hypernym = extractLastHypernym(constituentWords.get(headWordId));
          if (!(headWordId == 0 && ("IN".equals(headWordToken.getPos()) || ",".equals(headWordToken.getPos())
              || prepositions.contains(headWordToken.getCoveredText())))) {
            // Feature feature =
            // new Feature(FEATURE_NAME, constituentWords.get(headWordId).getCoveredText().toUpperCase());
            Feature feature = new Feature(FEATURE_NAME, hypernym.toUpperCase());
            features.add(feature);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return features;
  }

  private String extractLastHypernym(Token token) throws IOException {
    String result = token.getCoveredText();
    String path = "wordnet" + File.separator + "dict";
    URL url = new URL("file", null, path);
    IDictionary dict = new Dictionary(url);
    dict.open();
    IIndexWord idxWord = dict.getIndexWord(token.getCoveredText().toLowerCase(), getCorrectPOS(token.getPos()));
    if (idxWord != null && idxWord.getWordIDs().size() > 0) {
      IWordID wordID = idxWord.getWordIDs().get(0);
      IWord word = dict.getWord(wordID);
      ISynset synset = word.getSynset();
      List<ISynsetID> hypernyms = synset.getRelatedSynsets(Pointer.HYPERNYM);
      List<IWord> words;
      for (ISynsetID sid : hypernyms) {
        words = dict.getSynset(sid).getWords();
        for (Iterator<IWord> i = words.iterator(); i.hasNext();) {
          result = i.next().getLemma();
        }
      }
    }
    dict.close();
    return result;
  }

  private POS getCorrectPOS(String pos) {
    if (pos.startsWith("NN"))
      return POS.NOUN;
    else if (pos.startsWith("VB"))
      return POS.VERB;
    return POS.NOUN;
  }
}
