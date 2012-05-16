package megazi.smartup;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import linguistic.PhraseFinder;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
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


public class SmartUpActivity extends Activity implements OnTouchListener {

	static private InputStream model = null;
	static private PhraseFinder phraseHandler = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {


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
			initText = getAssets().open("Asimov.txt");

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
//				if (timediff >= ViewConfiguration.getLongPressTimeout())
//				{
					// see if this is part of a WME
					String inputPhrase = "These numbers just don't add up."; // getSelectedPhrase(); 
					String mwe = getMWE(inputPhrase, getAssets());

					if (mwe.length() == 0){
						GetWordnikDefinitionTask task = new GetWordnikDefinitionTask();
						task.execute(new String[] { getSelectedWord() });
					}
					else {
						Toast.makeText(this, mwe, Toast.LENGTH_LONG).show();
					}
//				}
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
		// TODO: need to call the tokenizer instead.
		
		EditText tv = (EditText) findViewById(R.id.edit1); 
		int selectionStart = tv.getSelectionStart();
		int selectionEnd = tv.getSelectionEnd();
		
		int phraseStart = selectionStart;
		while (phraseStart >= 0)
		{
			char c = tv.getText().charAt(phraseStart);
			if (c == '.' || c == '?' || c =='!') {
				phraseStart++;
				break;
			}
			phraseStart--;			
		}
		
		int phraseEnd = selectionEnd;
		while (phraseEnd < tv.getText().length())
		{
			char c = tv.getText().charAt(phraseStart);
			if (c == '.' || c == '?' || c =='!') {
				phraseEnd--;
				break;
			}
			phraseEnd++;	
		}
		Log.i("INFO", "selected phrase starts at " + selectionStart + " and ends at " + selectionEnd);
		
		StringBuilder sb = new StringBuilder(selectionEnd - selectionStart + 1);
		for (int i = phraseStart; i <= phraseEnd; i++)	{
			sb.append(tv.getText().charAt(i));
		}
		
		Log.i("INFO", "selected phrase is " + sb.toString());
		return sb.toString();
	}

	private String getMWE(String inputPhrase, AssetManager assetMgr)
	{
		String result = "";        
		try {        	
			if (model == null)	{
				model = assetMgr.open("pos-en-general-brown.HiddenMarkovModel");
			}
			if (phraseHandler == null) 	{
				// the phrase handler can't reach assets, so pass in a stream from here
				phraseHandler = new PhraseFinder(model);
			}

			//probably the easiest way to mark the highlights is to break the input string into
			//something with indices. it's up to you to map the indices to the larger text
			//context in TextView
			String [] indexed = inputPhrase.split("\\s+");
			if(phraseHandler.findPhrasalVerb(indexed)) {
				StringBuilder sb = new StringBuilder();
				//pull the phrase out of the text we sent
				for (int i = phraseHandler.getBeginningIndex(); i <= phraseHandler.getEndIndex(); i++)
					sb.append(indexed[i]).append(" ");
				result = sb.toString();
				//debugging
				Log.w("WTF", result.toString());
			} else
				Log.w("WTF", " not found ! ");

		} catch (IOException e) {
			Log.e("ERROR", "Failed to find HMM file");
			//e.printStackTrace();
		} catch (ClassNotFoundException e) {
			Log.e("ERROR", "Failed to find aliasi classes");
			//e.printStackTrace();
		}        
		return result;
	}

	private class GetWordnikDefinitionTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... lemmas) {
			
			Log.i("Background", "Enter");

			assert (lemmas.length == 1);			

			StringBuilder defs = new StringBuilder();
			try {
				List<Definition> def = WordApi.definitions(lemmas[0]);
				
				defs.append(lemmas[0].toUpperCase()+ "\n\n");

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
			tv.setText(result);
		}
	}

}