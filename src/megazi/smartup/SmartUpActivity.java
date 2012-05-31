package megazi.smartup;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import linguistic.Phrase;
import linguistic.PhraseFinder;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.jeremybrooks.knicker.*;
import net.jeremybrooks.knicker.dto.Definition;
import net.jeremybrooks.knicker.dto.TokenStatus;
import net.jeremybrooks.knicker.dto.Word;


public class SmartUpActivity extends Activity implements OnTouchListener {

	class TextPart {
		int start = -1;
		int end = 1;
		String text = "";
		int selWordIndex = -1;
	}
	
	static private InputStream model = null;
	static private PhraseFinder phraseHandler = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		String reading = getIntent().getStringExtra("read");
		this.setTitle(reading.substring(6)); //skip "texts/" prefix
		
		System.setProperty("WORDNIK_API_KEY", "d671ba7493f90deab796270fba309776b2f2395e2b412fae4");
		TokenStatus status;
		try {
			status = AccountApi.apiTokenStatus();
			if (status.isValid()) {
				Log.i("INFO", "SmartUpActivity::onCreate - Wordnik API key is valid.");
			} else {
				Log.e("ERROR", "SmartUpActivity::onCreate - Wordnik API key is invalid.");    	   
			}

		} catch (KnickerException e1) {			
			e1.printStackTrace();
		}  

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
    
		EditText tv = (EditText) findViewById(R.id.edit1); 
		tv.setHapticFeedbackEnabled(true);
		tv.setLongClickable(true);       
		tv.setOnTouchListener(this);

		StringBuilder text = new StringBuilder();
		InputStream initText = null;
		try {
			initText = getAssets().open(reading);

			final int buf_size = 4096;
			byte[] buf = new byte[buf_size];			
			while (true) {
				int bytes_read = initText.read(buf, 0, buf_size);
				if (bytes_read == -1) {
					break;
				}
				text.append(new String(buf));
				for (int i = 0; i < buf_size; i++) {
					buf[i] = '\0';
				}
			}			
			if (initText != null)  {       
				initText.close();
			}

		} catch (IOException e) {			
			e.printStackTrace();
			Toast.makeText(this, "Could not find initial text to load", Toast.LENGTH_LONG).show(); 
		}

		tv.setText(text);
	}
	
	@Override
	public boolean onTouch(View view, MotionEvent ev) {
		
		switch (view.getId()) {
		case R.id.edit1:
			
			switch (ev.getAction()) {
			case MotionEvent.ACTION_UP:
				long timediff = ev.getEventTime() - ev.getDownTime();
				Log.v("INFO", "SmartUpActivity::onTouch ACTION_UP, timediff is " + timediff + " selection is " + getSelectedWord());
				if (timediff >= ViewConfiguration.getLongPressTimeout()) {
										
					GetWordnikDefinitionTask wordnikTask = new GetWordnikDefinitionTask();
					
					TextPart curSentence = getSelectedSentence();
					if (curSentence != null) {
						String inputSentence = curSentence.text;
						
						// find a phrasal verb in the current sentence:
						Phrase mwe = extractPhrase(inputSentence);
						if (mwe != null && mwe.getVerb().equals(getSelectedWord().text)) 
						{								
							String prepVerb =  mwe.getVerb() + " " + mwe.getPreposition();
							Log.v("INFO", "Found MWE " + prepVerb);

							// send the verb+prep to wordnik							
							wordnikTask.execute(new String[] { prepVerb });
						}
						else 
						{
							// send just the selected word to wordnik
							wordnikTask.execute(new String[] { getSelectedWord().text });
						}
					}
				}
				break;
			}			
			break;
		}		
		return false;
	}

	private TextPart getSelectedWord()
	{
		TextPart ret = new TextPart();
		EditText tv = (EditText) findViewById(R.id.edit1); 
		ret.start = tv.getSelectionStart();
		ret.end = tv.getSelectionEnd();

		StringBuilder sb = new StringBuilder(ret.end - ret.start + 1);
		for (int i = ret.start; i < ret.end; i++)
		{
			sb.append(tv.getText().charAt(i));
		}
		ret.text = sb.toString();
		return ret;
	}
	
	private TextPart getSelectedSentence()
	{
		TextPart ret = new TextPart();

		// Just look for punctuation marks on either side of the selection.
		EditText tv = (EditText) findViewById(R.id.edit1); 
		int selectionStart = tv.getSelectionStart();
		int selectionEnd = tv.getSelectionEnd();
		if (selectionStart == selectionEnd) {
			return null;
		}
		
		CharSequence fullText = tv.getText();		
		int phraseStart = selectionStart;
		while (phraseStart >= 0)
		{
			char c = fullText.charAt(phraseStart);
			if (c == '.' || c == '?' || c =='!' || c == '"') {
				phraseStart++;
				break;
			}
			phraseStart--;			
		}
		
		int phraseEnd = selectionEnd;
		while (phraseEnd < fullText.length())
		{
			char c = tv.getText().charAt(phraseEnd);
			if (c == '.' || c == '?' || c =='!' || c == '"') {
				phraseEnd--;
				break;
			}
			phraseEnd++;	
		}
		
		StringBuilder sb = new StringBuilder(selectionEnd - selectionStart + 1);
		for (int i = phraseStart; i <= phraseEnd; i++)	{
			sb.append(tv.getText().charAt(i));
		}
		
		Log.i("INFO", "selected 'sentence' is " + sb.toString());
		ret.start = selectionStart;
		ret.end = selectionEnd;
		ret.text = sb.toString();
		return ret;
	}

	private Phrase extractPhrase(String inputPhrase)
	{   
		Phrase ret = null;
		try {        	
			if (model == null)	{
				model = getAssets().open("pos-en-general-brown.HiddenMarkovModel");
			}
			if (phraseHandler == null) 	{				
				phraseHandler = new PhraseFinder(model); // the phrase handler can't reach assets, so pass in a stream from here
			}			
			String [] indexed = inputPhrase.split("\\s+");
			Phrase ph = phraseHandler.getRawPhrasalVerb(indexed);			
			if (ph.ithinkIfoundSomething()) {
				ret = ph;
				Log.i("extractPhrase", "Found phrase " + ph.getVerb() + " " + ph.getPreposition());
			}
			
		} catch (IOException e) {
			Log.e("ERROR", "Failed to find HMM file");
		} catch (ClassNotFoundException e) {
			Log.e("ERROR", "Failed to find aliasi classes");
		}		
		return ret;
	}

	private class GetWordnikDefinitionTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... strs) {
			
			Log.i("Background", "Enter");
			String[] surface_strs = strs[0].split("\\s+");
			
			boolean phrasal = (surface_strs.length == 2);

			// This will reconstruct the "verb prep":
			StringBuilder sb = new StringBuilder(surface_strs[0]);
			for (int i = 1; i < surface_strs.length; i++) {
				sb.append(" " + surface_strs[i]);
			}

			StringBuilder defs = new StringBuilder();
			try {	
				String lemma = getLemmaFromWordnik(surface_strs[0]);
				Log.i("Background", "Found lemma '" + lemma + "' for " + surface_strs[0]); 
				
				defs.append(lemma.toUpperCase());
				if (phrasal) {
					defs.append(" " + surface_strs[1].toUpperCase());
				}
				defs.append("\n\n");
				
				List<Definition> def = WordApi.definitions(lemma, 0, null, false, null, true, false);
				int i = 1;
				for (Definition d : def) {
					if (!phrasal) 
					{
						defs.append((i++) + ") " + d.getPartOfSpeech() + ": " + d.getText() + "\n");
					}
					else if (d.getPartOfSpeech().equals("phrasal-verb"))
					{
						String origPrep = surface_strs[1].trim().toLowerCase();
						String defPrep = d.getText().split("\\s+")[1].toLowerCase();						
						Log.i("Background", "Orig prep is '" + origPrep + "' and def prep is '" + defPrep + "'"); 
						
						if (origPrep.equals(defPrep)) {						
							defs.append((i++) + ") " + "phrasal-verb" + ": " + d.getText() + "\n");
						}
					}
				}
			} catch (KnickerException e) {
				// defs.append("Ummmm.... Wordnik's fault!\n");
				e.printStackTrace();
			}	
			Log.i("Background", "Returning "+ defs);
			return defs.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			//String[] senses = result.split("\n");

			TextView tv = (TextView) findViewById(R.id.def); 
			tv.setText(result);
		}
		
		private String getLemmaFromWordnik (String original) {
			
			String lemma = original;
			try {				
				Word wd = WordApi.lookup(original, true, false);
				String suggested = (wd.getSuggestions().size() > 0) ? wd.getSuggestions().get(0) : "";
				String word = wd.getWord();
				if (!suggested.isEmpty()) {
					lemma = suggested;
				}
				else if (!word.isEmpty()) {
					lemma = word;
				}

			} catch (KnickerException e) {				
				Log.e("getLemmaFromWordnik", e.getMessage());
			}
			return lemma;			
		}
	}

}