package us.ml.question.classifier.extractors;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.NamedFeatureExtractor1;
import org.cleartk.token.type.Token;

/**
 * This class extends NamedFeatureExtractor1 which extracts word shape -- e.g.,
 * whether capitalized, numeric, etc. Lot of named entity recognitions APIs like
 * Stanford NER uses word shapes. This code contains snippet from Stanford NER,
 * advisable to see licensing before moving to production.
 * 
 * @author utkarsh
 *
 * @param <T>
 */
public class ShapeExtractor<T extends Token> implements NamedFeatureExtractor1<T> {

	private static final String FEATURE_NAME = "shape";
	private static final Pattern emailRegex = Pattern.compile(
			"[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@([A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,}))",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern yearRegex = Pattern.compile(
			"((?:(?:[1]{1}\\d{1}\\d{1}\\d{1})|(?:[2]{1}\\d{3})))(?![\\d])", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	/**
	 * Creates singleton feature with shape of the focus annotation.
	 */
	public List<Feature> extract(JCas jCas, Token focusAnnotation) throws CleartkExtractorException {
		String jCasText = jCas.getDocumentText();
		String wordShape = jCasText == null ? null : getWordShape(focusAnnotation.getCoveredText());
		if (wordShape == null || wordShape.isEmpty())
			throw new IllegalArgumentException(
					"Word Shape cannot be empty, TokenshapeAnnotor probably not added in Pipeline");
		Feature feature = new Feature(FEATURE_NAME, wordShape);
		return Collections.singletonList(feature);
	}

	public String getFeatureName() {
		return FEATURE_NAME;
	}

	private String getWordShape(String s) {
		int length = s.length();
		if (length == 0) {
			return "SYMBOL";
		}
		Matcher matcher = emailRegex.matcher(s);
		if (matcher.find()) {
			return "EMAIL";
		}
		Matcher yearMatcher = yearRegex.matcher(s);
		if (yearMatcher.find()) {
			return "YEAR";
		}
		boolean cardinal = false;
		boolean number = true;
		boolean seenDigit = false;
		boolean seenNonDigit = false;

		for (int i = 0; i < length; i++) {
			char ch = s.charAt(i);
			boolean digit = Character.isDigit(ch);
			if (digit) {
				seenDigit = true;
			} else {
				seenNonDigit = true;
			}
			// allow commas, decimals, and negative numbers
			digit = digit || ch == '.' || ch == ',' || (i == 0 && (ch == '-' || ch == '+'));
			if (!digit) {
				number = false;
			}
		}

		if (!seenDigit) {
			number = false;
		} else if (!seenNonDigit) {
			cardinal = true;
		}

		if (cardinal) {
			if (length < 4) {
				return "CARDINAL3";
			} else if (length == 4) {
				return "CARDINAL4";
			} else {
				return "CARDINAL5PLUS";
			}
		} else if (number) {
			return "NUMBER";
		}

		boolean seenLower = false;
		boolean seenUpper = false;
		boolean allCaps = true;
		boolean allLower = true;
		boolean initCap = false;
		boolean dash = false;
		boolean period = false;

		for (int i = 0; i < length; i++) {
			char ch = s.charAt(i);
			boolean up = Character.isUpperCase(ch);
			boolean let = Character.isLetter(ch);
			boolean tit = Character.isTitleCase(ch);
			if (ch == '-') {
				dash = true;
			} else if (ch == '.') {
				period = true;
			}

			if (tit) {
				seenUpper = true;
				allLower = false;
				seenLower = true;
				allCaps = false;
			} else if (up) {
				seenUpper = true;
				allLower = false;
			} else if (let) {
				seenLower = true;
				allCaps = false;
			}
			if (i == 0 && (up || tit)) {
				initCap = true;
			}
		}

		if (length == 2 && initCap && period) {
			return "ACRONYM1";
		} else if (seenUpper && allCaps && !seenDigit && period) {
			return "ACRONYM";
		} else if (seenDigit && dash && !seenUpper && !seenLower) {
			return "DIGIT-DASH";
		} else if (initCap && seenLower && seenDigit && dash) {
			return "CAPITALIZED-DIGIT-DASH";
		} else if (initCap && seenLower && seenDigit) {
			return "CAPITALIZED-DIGIT";
		} else if (initCap && seenLower && dash) {
			return "CAPITALIZED-DASH";
		} else if (initCap && seenLower) {
			return "CAPITALIZED";
		} else if (seenUpper && allCaps && seenDigit && dash) {
			return "ALLCAPS-DIGIT-DASH";
		} else if (seenUpper && allCaps && seenDigit) {
			return "ALLCAPS-DIGIT";
		} else if (seenUpper && allCaps && dash) {
			return "ALLCAPS";
		} else if (seenUpper && allCaps) {
			return "ALLCAPS";
		} else if (seenLower && allLower && seenDigit && dash) {
			return "LOWERCASE-DIGIT-DASH";
		} else if (seenLower && allLower && seenDigit) {
			return "LOWERCASE-DIGIT";
		} else if (seenLower && allLower && dash) {
			return "LOWERCASE-DASH";
		} else if (seenLower && allLower) {
			return "LOWERCASE";
		} else if (seenLower && seenDigit) {
			return "MIXEDCASE-DIGIT";
		} else if (seenLower) {
			return "MIXEDCASE";
		} else if (seenDigit) {
			return "SYMBOL-DIGIT";
		} else {
			return "SYMBOL";
		}
	}
}