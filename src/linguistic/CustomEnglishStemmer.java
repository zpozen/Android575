
package linguistic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import linguistic.snowball.englishStemmer;

import linguistic.tokenizer.PhraseTokenizer;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tag.Tagging;

public class CustomEnglishStemmer {

	/**
	 * FIXME: this hashmap needs to go in a database on the device
	 */
	private static HashMap<String, String> verbList = new HashMap<String, String>();
	static {
		verbList.put("take-VB x-NN in-IN", "absorb, kinda");
		verbList.put("get-VB x-NN of-IN", "what");
		verbList.put("keep-VB tabs-NN on-IN", "track activity");
	}

	private static final String VERB = "vb";
	private static final String TO = "to";
	private static final String IN = "in";
	
	public static void main (String [] args) {
		CustomEnglishStemmer ces = new CustomEnglishStemmer();
		ces.findPhrasalVerb("I want to keep tabs on it.");
		ces.findPhrasalVerb("He was backing out of the driveway.");
		ces.findPhrasalVerb("she's keeping a hold of it.");
		
	}
    
	public void findPhrasalVerb(String input) {
		System.out.println(new Date());
		//String temp = "I want to keep tabs on her"; //expected indices = 123
		PhraseTokenizer tokenizer = new PhraseTokenizer();
		String [] raw_surface = tokenizer.doTokenize(input);
		try {
			invokeLingPipe(raw_surface);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void invokeLingPipe(String [] phrase) throws IOException, ClassNotFoundException {
	
        //System.out.println("Reading model from file=" + args[0]);
        FileInputStream fileIn = new FileInputStream(new File("/home/megs/tools/lingpipe-4.1.0/demos/models/pos-en-general-brown.HiddenMarkovModel"));
        ObjectInputStream objIn = new ObjectInputStream(fileIn);
        HiddenMarkovModel hmm = (HiddenMarkovModel) objIn.readObject();
        objIn.close();
        //Streams.closeQuietly(objIn);
        HmmDecoder decoder = new HmmDecoder(hmm);
        List<String> tokenList = Arrays.asList(phrase);
		for (String s : tokenList)
			System.out.print(s + " ");
        System.out.println();
        Tagging<String> tagging = decoder.tag(tokenList);
//        System.out.println("\nFIRST BEST");
        for (int i = 0; i < tagging.size(); ++i)
            System.out.print(tagging.token(i) + "-" + tagging.tag(i).toUpperCase() + " ");
        System.out.println();
		System.out.println("result verb! " + pullOutSearchString(tagging));
		List<String> rawTokens = tagging.tokens();
		List<String> tags = tagging.tags();
		String[] stemmedTokens = stemString(rawTokens);
		System.out.println("result other! " + pullOutSearchString(stemmedTokens, tags.toArray(new String[tags.size()])));
	}
	
	
	private String pullOutSearchString(Tagging tokenAndTags) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokenAndTags.size(); i++) {
			//look for a verb
			if (tokenAndTags.tag(i).startsWith(VERB)) {
				sb.append(tokenAndTags.token(i)).append("-").append(VERB); //add verb to search string
				//case 1: we have an uninterrupted verb-preposition type of phrase
				if (i+1 < tokenAndTags.size() && tokenAndTags.tag(i+1).startsWith(IN)) { //TODO: do I need to check "to"?
					sb.append(" ").append(tokenAndTags.token(i)).append("-").append(IN); //add prep to search string
					return sb.toString();
				} 
				//case 2: there's a noun phrase in between the verb and preposition
				else {
					//skip past all words in the noun phrase
					// they get normalized into "x-NN"
					for(int j = i+1; j < tokenAndTags.size(); j++) {
						if((tokenAndTags.tag(j).equals(IN) || tokenAndTags.tag(j).equals(TO))) {
							//did we actually encounter a phrase here? (this should always be the case)
							if (j != i+1) {
								sb.append(" x-nn ");
								// now tack on the prep
								sb.append(tokenAndTags.token(j)).append("-").append(tokenAndTags.tag(j));
								return sb.toString();
							}
							else //false positive
								break;
						}
					}
				}
			}
			//no love for this verb - see if there are any more
			sb = new StringBuilder();
		}
		return ""; //no verb found. what can I do?
	}
	
	private String pullOutSearchString(String[] surface, String[] tags) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < surface.length; i++) {
			//look for a verb
			if (tags[i].startsWith(VERB)) {
				sb.append(surface[i]).append("-").append(VERB); //add verb to search string
				//case 1: we have an uninterrupted verb-preposition type of phrase
				if (i+1 < tags.length && tags[i+1].startsWith(IN)) { //TODO: do I need to check "to"?
					sb.append(" ").append(surface[i]).append("-").append(IN); //add prep to search string
					return sb.toString();
				} 
				//case 2: there's a noun phrase in between the verb and preposition
				else {
					//skip past all words in the noun phrase
					// they get normalized into "x-NN"
					for(int j = i+1; j < tags.length; j++) {
						if((tags[j].equals(IN) || tags[j].equals(TO))) {
							//did we actually encounter a phrase here? (this should always be the case)
							if (j != i+1) {
								sb.append(" x-nn ");
								// now tack on the prep
								sb.append(surface[j]).append("-").append(tags[j]);
								return sb.toString();
							}
							else //false positive
								break;
						}
					}
				}
			}
			//this verb was not a winner - check for another one
			sb = new StringBuilder();
		}
		return ""; //no verb found. what can I do?
	}
	
	/**
	 * Calls Snowball stemmer on a string of English words
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
