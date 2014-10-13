package com.ballabito.redhanded;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity {

	public static final String EXTRA_MESSAGE = "com.ballabito.redhanded.MESSAGE";
	public static final String SAVE_FILE_NAME = "checkin.txt";
	private static final int REQUEST_TAKE_PHOTO = 1;
	public boolean hasCamera;
	
	public class DeleteFileDialogFragment extends DialogFragment {
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        builder.setMessage(R.string.dialog_delete_file)
	               .setPositiveButton("yes", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       deleteFile();
	                       dismiss();
	                   }
	               })
	               .setNegativeButton("no", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       dialog.cancel();
	                   }
	               });
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        hasCamera = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        // set the TextView file_view to scroll
        ((TextView) findViewById(R.id.file_view)).setMovementMethod(new ScrollingMovementMethod());
        ((TextView) findViewById(R.id.file_view)).setMovementMethod(LinkMovementMethod.getInstance());
        // set the checkin button and checkout checkbox based on the last entry in the save file
        //setCheckinButtonText();
        // load the TextView file_view with the parse json data from the save file
        loadFileView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_delete) {
        	confirmDeleteFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Update the save file with a new check in or out
    // called when the checkin button is clicked
    public void updateFile(View view) {
    	FileWriter out;
    	
    	// get the optional message from the EditText and set it back to the hint
    	EditText note_t = (EditText) findViewById(R.id.note_text);
    	String note = note_t.getText().toString();
    	note_t.setText("");
    	// Hide the keyboard
    	InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    	inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    	
    	CheckBox checkout_box = (CheckBox) findViewById(R.id.checkout_checkbox);
    	boolean is_check_out = checkout_box.isChecked();
    	CheckBox picture_box = (CheckBox) findViewById(R.id.picture_checkbox);
    	boolean take_picture = picture_box.isChecked();
    	// create new JSON object to add to the save file
    	try {
    		out = new FileWriter(new File(this.getFilesDir(), SAVE_FILE_NAME), true);
    		JSONObject json = new JSONObject();
    		Calendar c = Calendar.getInstance();
    		Date date = c.getTime();
    		if(is_check_out) json.put("check","out");
    		else json.put("check", "in");
    		json.put("date", date.toString());
    		if(!note.equals("")) json.put("note", note);
    		if(take_picture) {
    			File pic = takePicture(date);
    			json.put("picture", pic.getAbsolutePath());
    		}
    		// for future sections:
    		// if(location box is checked) get location, json.put("location", location.toString())
    		out.append(json.toString() + "\n");
    		out.close();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	// Set the checkin button back to default and uncheck all boxes
    	checkout_box.setChecked(false);
    	picture_box.setChecked(false);
    	setCheckinButtonText(checkout_box);
    	
    	// load the new save file into the TextView file_view
    	loadFileView();
    }
    
    // load or reload the file_view TextView for when the file has been updated or the program has started
    public void loadFileView(){
    	TextView fileview = (TextView) findViewById(R.id.file_view);
        File file = new File(this.getFilesDir(), SAVE_FILE_NAME);
        if(!file.exists()) {
        	// if the file hasnt been created, set the checkin button to its default
        	Button check_b = (Button) findViewById(R.id.checkin_button);
        	check_b.setText(getString(R.string.button_checkin));
        	fileview.setText("No saved checkin file...");
        }
        else {
        	try {
        		BufferedReader br = new BufferedReader(new FileReader(file));
        		StringBuilder text = new StringBuilder();
        		String line = "";
        		// parse each JSON object in the file into human readable terms
        		while ((line = br.readLine()) != null) {
        			try{
        			JSONObject json = new JSONObject(line);
        			text.append("Check " + json.getString("check") + "\n");
        			text.append("    Date: " + json.getString("date") + "\n");
        			if(json.has("note"))
        				text.append("    Note: " + json.getString("note") + "\n");
        			if(json.has("picture"))
        				text.append("    Picture: " + json.getString("picture") + "\n");
        			// if JSON.has("location"), print "    Location: " + location
        			} catch (Exception e) {
        				fileview.setText("Error reading json file");
        			}
        		}
        		br.close();
        		
        		//Add the links to the Picture: line if any...
        		String finText = text.toString();
        		SpannableString clickable = new SpannableString(finText);
        		int start = finText.indexOf("Picture: ") + 9;
        		int end = finText.indexOf(".jpg", start) + 4;
        		//text.append("Start: " + start + " End: " + end +"\n");
        		//text.append("First found picture: " + finText.substring(start,end));
        		while(start > 9 && end > start) {
        			final String picFilePath = finText.substring(start,end);
		    		ClickableSpan clickableSpan = new ClickableSpan() {
		    		    @Override
		    		    public void onClick(View textView) {
		    		    	showPicture(picFilePath);
		    		    }
		    		};
		    		clickable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		    		start = finText.indexOf("Picture: ", end) + 9;
	        		end = finText.indexOf(".jpg", start) + 4;
        		}
        		
        		fileview.setText(clickable);
        	}
        	catch(Exception e){
        		fileview.setText("Error reading checkin save file");
        	}
        }
    }
    
    public void confirmDeleteFile() {
	    DialogFragment newFragment = new DeleteFileDialogFragment();
	    newFragment.show(getSupportFragmentManager(), "delete_file");
	}
    
    // Delete the save file. called when selected from options menu
    public void deleteFile() {
    	//first we need to remove all the pictures
    	String text = ((TextView) findViewById(R.id.file_view)).toString();
		int start = text.indexOf("Picture: ") + 9;
		int end = text.indexOf(".jpg", start) + 4;
		
		while(start > 9 && end > start) {
			String picFilePath = text.substring(start,end);
    		File pic = new File(picFilePath);
    		if(!pic.delete()) System.out.println("Failed to delete " + pic.getPath());
    		start = text.indexOf("Picture: ", end) + 9;
    		end = text.indexOf(".jpg", start) + 4;
		}
    	File file = new File(this.getFilesDir(), SAVE_FILE_NAME);
    	file.delete();
    	loadFileView();
    }
    
    
    // Used to set the checkin button and checkout checkbox depending on the last entry made.
    public void setCheckinButtonText() {
    	Button b = (Button) findViewById(R.id.checkin_button);
    	CheckBox c = (CheckBox) findViewById(R.id.checkout_checkbox);
    	String last = "", line;
    	try{
    		File file = new File(this.getFilesDir(), SAVE_FILE_NAME);
    		BufferedReader in = new BufferedReader(new FileReader(file));
    		if(!file.exists()){
    			b.setText(getString(R.string.button_checkin));
    			c.setChecked(false);
    			in.close();
    			return;
    		}
    		while ((line = in.readLine()) != null) {
    			last = line;
    		}
    		in.close();
    		if(last.equals("")){
    			b.setText(getString(R.string.button_checkin));
    			c.setChecked(false);
    			return;
    		}
    		JSONObject json = new JSONObject(last);
    		if(json.getString("check").equals("in")){
    			b.setText(getString(R.string.button_checkout));
    		    c.setChecked(true);
    		} else {
    			b.setText(getString(R.string.button_checkin));
    			c.setChecked(false);
    		}
        	
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    //sets the checkin button based on the checkout checkbox
    public void setCheckinButtonText(View view){
    	Button checkin_b = (Button) findViewById(R.id.checkin_button);
    	CheckBox out_box = (CheckBox) view;
    	
    	if(out_box.isChecked()) {
    		checkin_b.setText(R.string.button_checkout);
    	} else {
    		checkin_b.setText(R.string.button_checkin);
    	}
    }
    
    public File takePicture(Date date){
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(date);
	    String imageFileName = "RED_" + timeStamp + "_";
	    File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
	    File image;
		try {
			image = File.createTempFile(imageFileName, ".jpg", storageDir);
		} catch (IOException e) {
			image = null;
		}
		
		//attempt to take a picture
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	    // Ensure that there's a camera activity to handle the intent
	    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
	        // Create the File where the photo should go
	        // Continue only if the File was successfully created
	        if (image != null) {
	            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
	                    Uri.fromFile(image));
	            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
	            // Can add these photos to the Android Gallery with:
	            //galleryAddPic(image);
	        }
	    }
		
	    // image may be null
	    return image;
    }
    
    // add image to the android gallery
    private void galleryAddPic(File image) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(image);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    
    // the class that handles a popup picture
    public class ShowPictureDialogFragment extends DialogFragment {
    	Bitmap bm;
    	String path;
    	
    	ShowPictureDialogFragment(Bitmap b, String p) {
    		super();
    		bm = b;
    		path = p;
    	}
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
	        try {
		        ExifInterface exif = new ExifInterface(path);
	            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
	            Matrix matrix = new Matrix();
	            if (orientation == 6) {
	                matrix.postRotate(90);
	            }
	            else if (orientation == 3) {
	                matrix.postRotate(180);
	            }
	            else if (orientation == 8) {
	                matrix.postRotate(270);
	            }
	            bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true); // rotating bitmap
	        } catch (Exception e) {
	        	System.out.println("Failed to rotate picture " + path);
	        }
	        ImageView iv = new ImageView(MainActivity.this);
	        iv.setImageBitmap(bm);
	        
	        builder.setPositiveButton("continue", new DialogInterface.OnClickListener() {
	                   public void onClick(DialogInterface dialog, int id) {
	                       dismiss();
	                   }
	               })
	               .setView(iv);
	        // Create the AlertDialog object and return it
	        return builder.create();
	    }
	}
    
    public void showPicture(String picFullPath){
    	System.out.println("Clicked: " + picFullPath + "\n");
    	Bitmap image = decodeSampledBitmapFromResource(picFullPath, 500, 500);
    	ShowPictureDialogFragment spdf = new ShowPictureDialogFragment(image, picFullPath);
    	spdf.show(getSupportFragmentManager(), "show_picture");
    }
    
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
	    // Raw height and width of image
	    final int height = options.outHeight;
	    final int width = options.outWidth;
	    int inSampleSize = 1;
	
	    if (height > reqHeight || width > reqWidth) {
	
	        final int halfHeight = height / 2;
	        final int halfWidth = width / 2;
	
	        // Calculate the largest inSampleSize value that is a power of 2 and keeps both
	        // height and width larger than the requested height and width.
	        while ((halfHeight / inSampleSize) > reqHeight
	                && (halfWidth / inSampleSize) > reqWidth) {
	            inSampleSize *= 2;
	        }
	    }
	    return inSampleSize;
    }
    
    public static Bitmap decodeSampledBitmapFromResource(String pictureFullPath,
            int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pictureFullPath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(pictureFullPath, options);
    }
}
