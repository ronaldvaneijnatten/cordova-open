package com.disusered;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;

import android.net.Uri;
import android.content.Intent;
import android.webkit.MimeTypeMap;
import android.content.ActivityNotFoundException;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * This class starts an activity for an intent to view files
 */
public class Open extends CordovaPlugin {

  public static final String OPEN_ACTION = "open";
  public static final String DELETE_ACTION = "delete";

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    String pathOrg = args.getString(0);

    if (action.equals(OPEN_ACTION)) {
      try {
        String path = this.copyFile(pathOrg);
        this.chooseIntent(path, callbackContext);
      } catch (Exception e) {
        e.printStackTrace();
        callbackContext.error(3);
      }
      return true;

    } else  if (action.equals(DELETE_ACTION)) {
        try {
            String path = this.getTempFilepath(pathOrg);
            if (!this.deleteFile(path)) callbackContext.error(4);
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(5);
        }
        return true;
    }

    return false;
  }

  /**
   * Returns the MIME type of the file.
   *
   * @param path
   * @return
   */
  private static String getMimeType(String path) {
    String mimeType = null;

    String extension = MimeTypeMap.getFileExtensionFromUrl(path);
    if (extension != null) {
      MimeTypeMap mime = MimeTypeMap.getSingleton();
      mimeType = mime.getMimeTypeFromExtension(extension);
    }

    System.out.println("Mime type: " + mimeType);

    return mimeType;
  }

  /**
   * Creates an intent for the data of mime type
   *
   * @param path
   * @param callbackContext
   */
  private void chooseIntent(String path, CallbackContext callbackContext) {
    if (path != null && path.length() > 0) {
      try {
        Uri uri = Uri.parse(path);
        String mime = getMimeType(path);
        Intent fileIntent = new Intent(Intent.ACTION_VIEW);

        if( Build.VERSION.SDK_INT > 15 ){
          fileIntent.setDataAndTypeAndNormalize(uri, mime); // API Level 16 -> Android 4.1
        } else {
          fileIntent.setDataAndType(uri, mime);
        }

        cordova.getActivity().startActivity(fileIntent);

        callbackContext.success();
      } catch (ActivityNotFoundException e) {
        e.printStackTrace();
        callbackContext.error(1);
      }
    } else {
      callbackContext.error(2);
    }
  }

  private String getTempFilepath(String filePath) throws Exception {
    File folder = cordova.getActivity().getExternalCacheDir();
    if (folder==null) throw new Exception("External cache dir not available");
    String filePath2 = "file://" + folder.toString() + filePath.substring(filePath.lastIndexOf('/'));  //"file:///storage/emulated/0/Android/data/com.blaud.bcm/files/" + path.substring(path.lastIndexOf('/'));
    System.out.println("Filepath '"+filePath+"' > '"+filePath2+"'");
    return filePath2;
  }

  private boolean deleteFile(String filePath) {
    File file2 = new File(filePath.substring(7));
    if (file2.exists()) file2.delete();
    return !file2.exists();
  }

  private String copyFile(String path) throws Exception {

    File file = new File( path.substring(7));
    FileInputStream fis = null;
    if (!file.exists()) throw new java.lang.Exception("Source file '"+path+"' does not exist");

    String path2 = getTempFilepath(path);
    File file2 = new File(path2.substring(7));
    FileOutputStream fos = null;
    if (file2.exists()) file2.delete();
    if (file2.exists()) throw new java.lang.Exception("Target file '"+path2+" already exists and can't be deleted");

    try {
      fis = new FileInputStream(file);
      fos = new FileOutputStream(file2);
      byte[] bytes = new byte[1024000];
      for (int count=0; count!=-1; ) {
        count = fis.read(bytes);
        if(count>0) fos.write(bytes, 0, count);
      }

    } finally {
      try {
        if (fis!=null) fis.close();
        if (fos!=null) fos.close();
      } catch (Exception e) {
        System.out.println("Error closing stream: "+e);
      }
    }

    return path2;
  }
}
