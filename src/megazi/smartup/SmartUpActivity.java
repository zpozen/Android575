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

	static private InputStream model = null;
	static private PhraseFinder phraseHandler = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		String reading = getIntent().getStringExtra("read");
		
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
					
					String inputPhrase = getSelectedPhrase();
					TextView tv = (TextView) findViewById(R.id.def); 					
					GetWordnikDefinitionTask wordnikTask = new GetWordnikDefinitionTask();					
					Phrase mwe = getMWE(inputPhrase, getAssets());	
					if (mwe == null) {	
						tv.setText(getSelectedWord().toUpperCase() + "\n\n");
						wordnikTask.execute(new String[] { getSelectedWord() });
					}
					else {
						String prepVerb =  mwe.getVerb() + " " + mwe.getPreposition();
						Log.v("INFO", "Found MWE " + prepVerb);
						tv.setText(prepVerb.toUpperCase() + "\n\n");
						wordnikTask.execute(new String[] { prepVerb });
					}
				}
				break;
			}
			break;
		}		
		return false;
	}

	private String getSelectedWord()
	{
		EditText tv = (EditText) findViewById(R.id.edit1); 
		int selectionStart = tv.getSelectionStart();
		int selectionEnd = tv.getSelectionEnd();

		StringBuilder sb = new StringBuilder(selectionEnd - selectionStart + 1);
		for (int i = selectionStart; i <= selectionEnd; i++)
		{
			sb.append(tv.getText().charAt(i));
		}
		return sb.toString();
	}
	
	private String getSelectedPhrase()
	{
		// Just look for punctuation marks on either side of the selection.
		EditText tv = (EditText) findViewById(R.id.edit1); 
		int selectionStart = tv.getSelectionStart();
		int selectionEnd = tv.getSelectionEnd();
		if (selectionStart == selectionEnd) {
			return "";
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
		
		Log.i("INFO", "selected phrase is " + sb.toString());
		return sb.toString();
	}

	private Phrase getMWE(String inputPhrase, AssetManager assetMgr)
	{   
		Phrase ret = null;
		try {        	
			if (model == null)	{
				model = assetMgr.open("pos-en-general-brown.HiddenMarkovModel");
			}
			if (phraseHandler == null) 	{				
				phraseHandler = new PhraseFinder(model); // the phrase handler can't reach assets, so pass in a stream from here
			}			
			String [] indexed = inputPhrase.split("\\s+");
			Phrase ph = phraseHandler.getRawPhrasalVerb(indexed);			
			ret = (ph.ithinkIfoundSomething()) ? ph : null;			
			
		} catch (IOException e) {
			Log.e("ERROR", "Failed to find HMM file");
		} catch (ClassNotFoundException e) {
			Log.e("ERROR", "Failed to find aliasi classes");
		}		
		return ret;
	}

	private class GetWordnikDefinitionTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... lemmas) {
			
			Log.i("Background", "Enter");

			assert (lemmas.length == 1);			

			StringBuilder defs = new StringBuilder();
			try {				
				Word wd = WordApi.lookup(lemmas[0], true, false);
				defs.append(wd.getCanonicalForm() + "\n\n");
				
				List<Definition> def = WordApi.definitions(lemmas[0], 0, null, false, null, true, false);
				int i = 1;
				for (Definition d : def) {
					defs.append((i++) + ") " + d.getPartOfSpeech() + ": " + d.getText() + "\n");
				}
			} catch (KnickerException e) {
				defs.append(e.getMessage());
				e.printStackTrace();
			}	
			Log.i("Background", "Returning "+ defs);
			return defs.toString();
		}

		@Override
		protected void onPostExecute(String result) {
			//String[] senses = result.split("\n");

			TextView tv = (TextView) findViewById(R.id.def); 
			tv.append(result);
		}
	}

}