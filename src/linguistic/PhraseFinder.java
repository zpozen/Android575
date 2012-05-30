
package linguistic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import linguistic.snowball.englishStemmer;
import linguistic.tokenizer.PhraseTokenizer;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;

public class PhraseFinder {

	//if we didn't find the phrase, here's
	//something to compare to so you know
	public int DEFAULT_INDEX = 1000;


	private String currentDefinition = "";
	private String currentPhrase = "";
	private int beginningIndex = DEFAULT_INDEX;
	private int endIndex = DEFAULT_INDEX;
	List<String> rawTokens;
	List<String> tags;

	/**
	 * This will be set after each phrase query
	 * and contains the surface value of the phrasal
	 * verb. Good for querying Wordnik with ;)
	 * 
	 * Contains "" if phrase wasn't found
	 * 
	 * @return
	 */
	public String getCurrentPhrase() {
		return currentPhrase;
	}

	/**
	 * These will be set after each phrase query
	 * They will be -1 if no phrase was found.
	 * So you'll want to do a range check ;)
	 * @return
	 */
	public int getBeginningIndex() {
		return beginningIndex;
	}

	/**
	 * The end index is inclusive of the last word of the phrase
	 * (in other words, it is NOT one past like String.substring() is)
	 * @return
	 */
	public int getEndIndex() {
		return endIndex;
	}

	/**
	 * This is just because I am returning index values from the phrase search,
	 * so here is how to retrieve the definition.
	 * @return - the definition found for the last searched phrase
	 */
	public String getDefinition() {
		return currentDefinition;
	}

	private static final String VERB = "vb";
	private static final String ADV = "rb";
	private static final String IN = "in";
	private static final String PRT = "rp"; //particle

	ObjectInputStream model;
	HiddenMarkovModel hmm;

	public static void main (String [] args) {
		PhraseFinder phraser;
		try {
			FileInputStream fis = new FileInputStream(new File(args[0]));
			phraser = new PhraseFinder(fis);
			String [] indexed = "The kids kick angry cats and the pot will boil over.".split("\\s+");  //case 1
			testPhraseFinder(phraser, indexed);
			indexed = "The excess stuff will burn off.".split("\\s+"); //case 2
			testPhraseFinder(phraser, indexed);
			indexed = "Write the answer down right here.".split("\\s+"); //case 3
			testPhraseFinder(phraser, indexed);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void testPhraseFinder(PhraseFinder phraser, String [] indexed) throws IOException, ClassNotFoundException {
		Phrase p = phraser.getRawPhrasalVerb(indexed); 
		if (p != null) {
			StringBuilder sb = new StringBuilder();
			//pull the phrase out of the text we sent
			for (int i = p.getVerbIndex(); i <= p.getPrepIndex(); i++)
				sb.append(indexed[i]).append(" ");
			sb.append(p.getVerbPOS());
			System.out.println(sb.toString());
			System.out.println(p.getVerb() + " " + p.getPreposition());
		} else
			System.out.println(" not found ! ");
	}
		

	public PhraseFinder(InputStream model) throws StreamCorruptedException, IOException, ClassNotFoundException {
		this.model = new ObjectInputStream(model);
		this.hmm = (HiddenMarkovModel) this.model.readObject();        
	}

	protected void finalize() throws Throwable {
		try {
			model.close();
		} finally {
			super.finalize();
		}
	}

	/**
	 * This is a workaround to the fact that the onboard stemmer 
	 * doesn't handle tense stemming at all
	 * @param input - tokenized sentence with a phrasal verb
	 * @return - Phrase object holding useful info about the phrase, if we found one
	 * 			Keep in mind that we are checking in DBLookup for the phrase, but not using that for our
	 * 			basis of approval of whether we found a phrase or not
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public Phrase getRawPhrasalVerb(String [] input) throws IOException, ClassNotFoundException {
		Phrase phrase = new Phrase();
		findPhrasalVerb(input);
		phrase.setVerb(rawTokens.get(beginningIndex));
		phrase.setVerbIndex(beginningIndex);
		phrase.setPreposition(rawTokens.get(endIndex));
		phrase.setPrepIndex(endIndex);
		phrase.setVerbPOS(tags.get(beginningIndex));
		return phrase;
	}
	/**
	 * Sets values on PhraseFinder indicating the location of the phrasal verb
	 * and also the definition
	 * 
	 * @param input - a string of words that probably contains a phrasal verb
	 * 				tokenized by whitespace
	 * @return boolean indicating whether we found a phrase or not
	 * 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private boolean findPhrasalVerb(String [] input) throws IOException, ClassNotFoundException {
		
		//reset everything across runs
		this.beginningIndex = DEFAULT_INDEX;
		this.endIndex = DEFAULT_INDEX;
		this.currentDefinition = "";
		this.currentPhrase = "";
		
		//we received a string tokenized by whitespace. What I really want is just 
		//the words with no punctuation, so let's do that now
		//XXX: when we get cross-sentence strings this might screw up the POS tagger
		for (int i = 0; i < input.length; i++) {
			//I'm doing it this way to keep the index length the same
			input[i] = input[i].replaceAll("[^\\w|']", ""); //removes punctuation except for apostrophes
			input[i] = input[i].toLowerCase();
		}

		Tagging<String> tagging = invokeLingPipe(input);

		//split it up so I can get the lemma
		rawTokens = tagging.tokens();
		tags = tagging.tags();
		//XXX the original lemmatizer doesn't work very well
		//String[] stemmedTokens = stemString(rawTokens);
		String[] stemmedTokens = rawTokens.toArray(new String[rawTokens.size()]);

		//do analysis on the POS tags
		String searchString = pullOutSearchString(stemmedTokens, tags.toArray(new String[tags.size()]), false);

		//see if the phrasal verb is in our dictionary
		currentDefinition = DBLookup.search(searchString);
		
		//we're sacrificing the second lookup in order to more easily handle the past tense problem
//		if (DBLookup.NOT_FOUND.equals(currentDefinition)) {
//			//if we encountered a thing that looked phrasal but isn't in our dictionary
//			// try again in case there's a second verb in the sentence
//			this.beginningIndex = DEFAULT_INDEX;
//			this.endIndex = DEFAULT_INDEX;
//			searchString = pullOutSearchString(stemmedTokens, tags.toArray(new String[tags.size()]), true);
//			currentDefinition = DBLookup.search(searchString);
//		}

		//if index range is default, we didn't find it
		return this.beginningIndex != DEFAULT_INDEX;
	}
	
	//unused
	public String getLemma(String word) {
		return PorterStemmerTokenizerFactory.stem(word);
	}

	/**
	 * Loads LingPipe's HMM decoder, which POS tags the input string
	 * @param phrase - tokenized words to be tagged
	 * @return a Tagging object, which has the surface text and tags
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Tagging<String> invokeLingPipe(String [] phrase) throws IOException, ClassNotFoundException {
		//FileInputStream fileIn = new FileInputStream(new File("/home/megs/tools/lingpipe-4.1.0/demos/models/pos-en-general-brown.HiddenMarkovModel"));
		// HiddenMarkovModel hmm = (HiddenMarkovModel) model.readObject();
		// model.close();
		HmmDecoder decoder = new HmmDecoder(hmm);
		List<String> tokenList = Arrays.asList(phrase);
		return decoder.tag(tokenList);
	}

	/**
	 * Check the POS structure to identify the phrasal verb
	 * 
	 * This looks for word patterns in the structure of
	 * 
	 * verb { preposition | particle | adjective }
	 * verb noun* { preposition | particle | adjective }
	 * 
	 * (The POS tagger we're using likes to label "open" (as in "burst open")
	 * as an adjective. The skipFirstVerb option helps move past false positives
	 * that this might cause.)
	 * 
	 * @param surface
	 * @param tags
	 * @return
	 */
	private String pullOutSearchString(String[] surface, String[] tags, boolean skipFirstVerb) {
		StringBuilder surfacePhrase = new StringBuilder();
		//debugging
		for (int t = 0; t < tags.length; t++)
			System.out.print(surface[t] + "-" + tags[t] + " ");
		System.out.println();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < surface.length; i++) {
			//look for a verb
			if (tags[i].startsWith(VERB)) {
				//disable the boolean so we'll process the next verb, if any
				 if(skipFirstVerb) {
					 skipFirstVerb = false;
					 continue;
				 }
				this.beginningIndex = i;
				sb.append(surface[i]).append("-").append(VERB); //add verb to search string
				surfacePhrase.append(surface[i]);
				//case 1: we have an uninterrupted verb-preposition type of phrase
				if (i+1 < tags.length && (tags[i+1].startsWith(IN) || tags[i+1].startsWith(PRT) || tags[i+1].equals(ADV))) { //don't check "to"; those are infinitives
					sb.append(" ").append(surface[i+1]).append("-").append(IN); //add prep to search string
					surfacePhrase.append(" ").append(surface[i+1]);
					this.endIndex = i+1;
					currentPhrase = surfacePhrase.toString();
					return sb.toString();
				} 
				//case 2: there's a noun phrase in between the verb and preposition
				else {
					//skip past all words in the noun phrase
					// they get normalized into "x-nn"
					for(int j = i+1; j < tags.length; j++) {
						if((tags[j].equals(IN) || tags[j].equals(PRT) || tags[j].equals(ADV)) && DBLookup.isKnownParticle(surface[j])) {
							//did we actually encounter a phrase here? (this should always be the case)
							if (j != i+1) {
								sb.append(" x-nn ");
								// now tack on the prep
								sb.append(surface[j]).append("-").append(IN);
								surfacePhrase.append(" ").append(surface[j]);
								this.endIndex = j;
								currentPhrase = surfacePhrase.toString();
								return sb.toString();
							}
							else {//false positive
								this.beginningIndex = DEFAULT_INDEX;
								break;
							}
						}
					}
				}
			}
			//this verb was not a winner - check for another one
			sb = new StringBuilder();
			surfacePhrase = new StringBuilder();
		}
		//reset everything, we didn't find it
		this.beginningIndex = this.endIndex = DEFAULT_INDEX;
		return ""; //no verb found. sorry.
	}

	/**
	 * Calls Snowball stemmer on a string of English words
	 * Caveat: this doesn't account for tense, so to improve the system
	 * we'd need some morphological analysis
	 * @param tokenizedInput - the word list, should be tokenized or our indices will mismatch
	 * @return the stemmed versions of the word
	 * @throws Throwable
	 */

	public String[] stemString(List<String> tokenizedInput) {
		String [] temp = tokenizedInput.toArray(new String[tokenizedInput.size()]);
		return stemString(temp);
	}

	public String[] stemString(String[] tokenizedInput) {

		englishStemmer stemmer = new englishStemmer();
		ArrayList<String> result = new ArrayList<String>();
		//StringBuffer input = new StringBuffer();

		for (String s : tokenizedInput) {
			stemmer.setCurrent(s.toLowerCase());
			stemmer.stem();
			result.add(stemmer.getCurrent());
		}

		return result.toArray(new String[result.size()]);
	}
}
