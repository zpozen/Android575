package linguistic;

/** 
 * Utility class that holds info about verb that
 * PhraseFinder found in a sentence
 *
 */
public class Phrase {

	private String verbPOS;
	private String verb;
	private String preposition;
	private int verbIndex;
	private int prepIndex;
	public String getVerbPOS() {
		return verbPOS;
	}
	public void setVerbPOS(String verbPOS) {
		this.verbPOS = verbPOS;
	}
	public String getVerb() {
		return verb;
	}
	public void setVerb(String verb) {
		this.verb = verb;
	}
	public String getPreposition() {
		return preposition;
	}
	public void setPreposition(String preposition) {
		this.preposition = preposition;
	}
	public int getVerbIndex() {
		return verbIndex;
	}
	public void setVerbIndex(int verbIndex) {
		this.verbIndex = verbIndex;
	}
	public int getPrepIndex() {
		return prepIndex;
	}
	public void setPrepIndex(int prepIndex) {
		this.prepIndex = prepIndex;
	}
	
}
