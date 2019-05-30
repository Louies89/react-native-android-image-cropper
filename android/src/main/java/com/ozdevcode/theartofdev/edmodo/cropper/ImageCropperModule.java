package com.ozdevcode.theartofdev.edmodo.cropper;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.ozdevcode.theartofdev.edmodo.utils.ResponseHelper;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Promise;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;

import java.io.FileOutputStream;
import java.io.OutputStream;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import android.support.v4.app.ActivityCompat;
import android.provider.MediaStore;

import static android.app.Activity.RESULT_OK;
import static com.ozdevcode.theartofdev.edmodo.utils.MediaUtils.transferImageToGallery;

public class ImageCropperModule extends ReactContextBaseJavaModule implements ActivityEventListener{

  private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";

  private static final String E_PICKER_CANCELLED_KEY = "E_PICKER_CANCELLED";
  private static final String E_PICKER_CANCELLED_MSG = "User cancelled image selection";

  private static final String E_CALLBACK_ERROR = "E_CALLBACK_ERROR";
  private static final String E_FAILED_TO_SHOW_PICKER = "E_FAILED_TO_SHOW_PICKER";
  private static final String E_FAILED_TO_OPEN_CAMERA = "E_FAILED_TO_OPEN_CAMERA";
  private static final String E_NO_IMAGE_DATA_FOUND = "E_NO_IMAGE_DATA_FOUND";
  private static final String E_CAMERA_IS_NOT_AVAILABLE = "E_CAMERA_IS_NOT_AVAILABLE";
  private static final String E_CANNOT_LAUNCH_CAMERA = "E_CANNOT_LAUNCH_CAMERA";
  private static final String E_PERMISSIONS_MISSING = "E_PERMISSION_MISSING";
  private static final String E_ERROR_WHILE_CLEANING_FILES = "E_ERROR_WHILE_CLEANING_FILES";

  private static final HashMap<String, Enum> GUIDELINES = new HashMap<String, Enum>();
  private static final HashMap<String, Enum> CROPSHAPES = new HashMap<String, Enum>();
  
  private static final int OPTIONS_NONE = 0x0;
  private static final int OPTIONS_SCALE_UP = 0x1;
  public static final int OPTIONS_RECYCLE_INPUT = 0x2;

  private static final String TAG =  "imageCropperModule";
  private ReactApplicationContext reactContext;

  private ReadableMap options;
  protected Callback callback;
  

  private ResponseHelper responseHelper = new ResponseHelper();



  public ImageCropperModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addActivityEventListener(this);

    GUIDELINES.put("on", CropImageView.Guidelines.ON);
    GUIDELINES.put("off",CropImageView.Guidelines.OFF);
    GUIDELINES.put("on-touch",CropImageView.Guidelines.ON_TOUCH);

    CROPSHAPES.put("rectangle",CropImageView.CropShape.RECTANGLE);
    CROPSHAPES.put("oval",CropImageView.CropShape.OVAL);

  }

  @Override
  public String getName() {
    return "ImageCropperManager";
  }

  @Override
  public Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();
    return constants;
  }

  @ReactMethod
  public void selectImage(final ReadableMap options,final Callback callback) {
    Activity currentActivity = getCurrentActivity();

    if (currentActivity == null)
    {
      responseHelper.invokeError(callback, "can't find current Activity");
      return;
    }

    this.callback = callback;
    this.options = options;

    String GUIDELINES_KEY = "guideLines",
            TITLE_KEY="title",
            CROPSHAPE_KEY = "cropShape",
            CROPMENU_BUTTON_TITLE_KEY= "cropMenuCropButtonTitle",
            REQUESTED_SIZE_HEIGHT_KEY = "requestedSizeHeight",
            REQUESTED_SIZE_WIDTH_KEY = "requestedSizeWidth",
            ALLOW_COUNTER_ROTATION_KEY = "allowCounterRotation",
            ALLOW_FLIPPING_KEY = "allowFlipping",
            ASPECT_RATIO_KEY="aspectRatio", //array [x,y]
            AUTO_ZOOM_KEY="autoZoomEnabled",
            MAX_ZOOM_KEY="maxZoom",
            FIX_RATIO_KEY="fixAspectRatio",
            CROP_WINDOW_PADDING_RATIO_KEY="initialCropWindowPaddingRatio",
            BORDER_CORNER_THICKNESS_KEY="borderCornerThickness",
            BORDER_CORNER_OFFSET_KEY="borderCornerOffset",
            BORDER_CORNER_LENGTH_KEY="borderCornerLength",
            GUIDELINES_THICKNESS_KEY="guidelinesThickness",
            SNAP_RADIUS_KEY="snapRadius",
            SHOW_CROP_OVERLAY_KEY="showCropOverlay",
            MIN_CROP_WINDOW_KEY="minCropWindowWidthHeight",
            FLIP_HORIZONTALLY_KEY="flipHorizontally",
            FLIP_VERTICALLY_KEY="flipVertically";

    CropImageView.Guidelines guidelines = ((CropImageView.Guidelines)GUIDELINES.get(options.getString(GUIDELINES_KEY)))!=null ?
            (CropImageView.Guidelines)GUIDELINES.get(options.getString(GUIDELINES_KEY)) : CropImageView.Guidelines.ON;
    CropImageView.CropShape cropShape = ((CropImageView.CropShape)CROPSHAPES.get(options.getString(CROPSHAPE_KEY))) != null ?
            (CropImageView.CropShape)CROPSHAPES.get(options.getString(CROPSHAPE_KEY)) : CropImageView.CropShape.RECTANGLE;

    CropImage.activity()
            .setGuidelines(guidelines)
            .setActivityTitle(options.getString(TITLE_KEY))
            .setCropShape(cropShape)
            .setCropMenuCropButtonTitle(options.getString(CROPMENU_BUTTON_TITLE_KEY))
            .setRequestedSize(
                    options.getInt(REQUESTED_SIZE_HEIGHT_KEY),
                    options.getInt(REQUESTED_SIZE_WIDTH_KEY))
            .setAllowCounterRotation(options.getBoolean(ALLOW_COUNTER_ROTATION_KEY))
            .setAllowFlipping(options.getBoolean(ALLOW_FLIPPING_KEY))
            .setInitialCropWindowPaddingRatio(0)
            .setAspectRatio(
                    options.getArray(ASPECT_RATIO_KEY).getInt(0)>0?options.getArray(ASPECT_RATIO_KEY).getInt(0):1,
                    options.getArray(ASPECT_RATIO_KEY).getInt(1)>0?options.getArray(ASPECT_RATIO_KEY).getInt(1):1)
            .setAutoZoomEnabled(options.getBoolean(AUTO_ZOOM_KEY))
            .setMaxZoom(options.getInt(MAX_ZOOM_KEY))
            .setFixAspectRatio(options.getBoolean(FIX_RATIO_KEY))
            .setInitialCropWindowPaddingRatio((float)options.getDouble(CROP_WINDOW_PADDING_RATIO_KEY))
            .setBorderCornerThickness((float)options.getDouble((BORDER_CORNER_THICKNESS_KEY)))
            .setBorderCornerOffset((float)options.getDouble(BORDER_CORNER_OFFSET_KEY))
            .setBorderCornerLength((float)options.getDouble(BORDER_CORNER_LENGTH_KEY))
            .setGuidelinesThickness((float)options.getDouble(GUIDELINES_THICKNESS_KEY))
            .setSnapRadius((float)options.getDouble(SNAP_RADIUS_KEY))
            .setShowCropOverlay(options.getBoolean(SHOW_CROP_OVERLAY_KEY))
            .setMinCropWindowSize(
                    options.getArray(MIN_CROP_WINDOW_KEY).getInt(0)>10?options.getArray(MIN_CROP_WINDOW_KEY).getInt(0):10,
                    options.getArray(MIN_CROP_WINDOW_KEY).getInt(1)>10?options.getArray(MIN_CROP_WINDOW_KEY).getInt(1):10
            )
            .setFlipHorizontally(options.getBoolean(FLIP_HORIZONTALLY_KEY))
            .setFlipVertically(options.getBoolean(FLIP_VERTICALLY_KEY))
            .start(getCurrentActivity());




  }

  private BitmapFactory.Options validateImage(String path) {
  	BitmapFactory.Options options = new BitmapFactory.Options();
  	options.inJustDecodeBounds = true;
  	options.inPreferredConfig = Bitmap.Config.RGB_565;
  	BitmapFactory.decodeFile(path, options);
  	return options;
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

    if (requestCode != CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
      return;
    }
	
      responseHelper.cleanResponse();

      Exception error =  null;
      Uri resultUri= null;

      CropImage.ActivityResult result = CropImage.getActivityResult(data);

      if (resultCode == RESULT_OK) {
        resultUri = result.getUri();
		
		if(options.hasKey("dstPath")){
		  String dstPath = options.hasKey("dstPath") ? options.getString("dstPath") : getTmpDir();
		  String fileName = options.hasKey("fileName") ? options.getString("fileName") : "pp-" + System.currentTimeMillis() + ".jpg";
		  try {
		    File originalFile = new File(resultUri.getPath());
		    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.reactContext.getContentResolver(), resultUri);
		    
		    File dir = new File(dstPath);
		    if (!dir.exists()) {
		  	dir.mkdirs();
		    }
          
		    File outFile = new File(dstPath, fileName);
		    outFile.createNewFile();
		    OutputStream fOut = new FileOutputStream(outFile);
		    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
		    fOut.flush();
		    fOut.close();
		    //Delete original file
		    if (outFile.exists() && originalFile.exists()) {
		  	  originalFile.delete();
		    }
			resultUri = Uri.fromFile(outFile);
		  }catch (Exception ex){
		    Log.e(TAG,ex.getMessage());
		    error = ex;
		  }
		}
		else{
		  Boolean transferFileToExternalDir = options.hasKey("transferFileToExternalDir")&&
				  options.getBoolean("transferFileToExternalDir");
		  String externalDirectoryName = this.options.getString("externalDirectoryName");
		
          //do transfer
          if(transferFileToExternalDir){
            try {
              File transImage= transferImageToGallery(getReactApplicationContext(), resultUri, externalDirectoryName);
              if(transImage!=null){
                resultUri = Uri.fromFile(transImage);
              }
            }catch (Exception ex){
              Log.e(TAG,ex.getMessage());
              error = ex;
            }
          
          }
		}
      } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
        error= result.getError();
      }

    if(error!=null){
      responseHelper.invokeError(callback,error.getMessage());
    }else if(resultUri==null||result.getOriginalUri()==null){
      responseHelper.invokeResponse(callback);
    }else{
	  
      responseHelper.putString("uri",resultUri.toString());
      responseHelper.putString("path",resultUri.getPath());
      responseHelper.putString("originalUri",result.getOriginalUri().toString());
	  
	  String originalPath = result.getOriginalUri().getPath().replace("/raw//","");
	  // originalPath = result.getOriginalUri().getPath().replace("//","/");
	  
	  responseHelper.putString("originalPath",originalPath);
	  
	  BitmapFactory.Options options = validateImage(resultUri.getPath());
	  
	  
	  responseHelper.putString("mime",options.outMimeType);
	  responseHelper.putInt("width",options.outWidth);
	  responseHelper.putInt("height",options.outHeight);
	  
	  File nfile = new File(resultUri.getPath());
	   
	  responseHelper.putInt("sizeByte",(int) nfile.length());
	  responseHelper.putDouble ("sizeKb",((double) nfile.length())/1024);
	  responseHelper.putDouble("sizeMb",((double) nfile.length())/(1024*1024));
	  responseHelper.putString("name",nfile.getName());
	  
	  
	  BitmapFactory.Options options1 = validateImage(originalPath);
	  responseHelper.putString("omime",options1.outMimeType);
	  responseHelper.putInt("owidth",options1.outWidth);
	  responseHelper.putInt("oheight",options1.outHeight);
	  
	  File ofile = new File(originalPath);
	  
	  responseHelper.putInt("osizeByte",(int) ofile.length());
	  responseHelper.putDouble ("osizeKb",((double) ofile.length())/1024);
	  responseHelper.putDouble("osizeMb",((double) ofile.length())/(1024*1024));
	  responseHelper.putString("oname",ofile.getName());
	  
      responseHelper.invokeResponse(callback);
    }

    callback = null;
    this.options = null;

  }
  
  @Override
  public void onNewIntent(Intent intent) {

  }
  @ReactMethod
  public void createThumbnail(ReadableMap options, Callback callback) {
	String mediaType = options.hasKey("mediaType") ? options.getString("mediaType") : "";
	int width = options.hasKey("width") ? options.getInt("width") : 100;
	int height = options.hasKey("height") ? options.getInt("height") : 100;
	String srcPath = options.hasKey("srcPath") ? options.getString("srcPath") : "";
	String dstPath = options.hasKey("dstPath") ? options.getString("dstPath") : getTmpDir();
	String fileName = options.hasKey("fileName") ? options.getString("fileName") : "thumb-" + System.currentTimeMillis() + ".jpg";
	
	srcPath = srcPath.replace("file://","");
	dstPath = dstPath.replace("file://","");

	if (mediaType.equals("") || srcPath.equals("")) {
		callback.invoke("INVALID_OPTIONS: mediaType, srcPath & dstPath are mandatory parameters");
		return;
	}
	
	try {
		Bitmap bitmap = null;
	    if(mediaType.equals("video")){
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			retriever.setDataSource(srcPath);
			String mType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
			if (mType == null){
				callback.invoke("INVALID_MEDIA_TYPE : mediaType is not 'video'");
				return;
			}
		    bitmap = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
			
	    }
	    else if(mediaType.equals("image")){
		    bitmap = BitmapFactory.decodeFile(srcPath);
		}	
		
		if (bitmap != null) {
			bitmap = extractThumbnail(bitmap, width, height, OPTIONS_RECYCLE_INPUT);
			File dir = new File(dstPath);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			File file = new File(dstPath, fileName);
			file.createNewFile();
			OutputStream fOut = new FileOutputStream(file);
			
			// ConvolutionMatrix convMatrix = new ConvolutionMatrix(3);
			// convMatrix.setAll(1);
			// convMatrix.Matrix[1][1] = options.hasKey("Matrix11") ?options.getInt("Matrix11") : 5;
			// convMatrix.Factor = options.hasKey("Factor") ?options.getInt("Factor") : 13;
			// convMatrix.Offset = options.hasKey("Offset") ?options.getInt("Offset") : 1;
			// bitmap = ConvolutionMatrix.computeConvolution3x3(bitmap, convMatrix);
			
			//Added By chandrajyoti for alternative of above commented code
			//Start
				ConvolutionMatrix convMatrix = new ConvolutionMatrix();
				bitmap = ConvolutionMatrix.convolute(bitmap, new float[] { 1 ,  1, 1 , 
																																	 1 ,  1, 1 , 
																																	 1 ,  1, 1   }, 
																							(options.hasKey("Factor") ?options.getInt("Factor") : 8), //For this logic default value should be 8
																							(options.hasKey("Offset") ?options.getInt("Offset") : 1)
																							);
			//End
			
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
			fOut.flush();
			fOut.close();
			
			WritableMap map = Arguments.createMap();
			
			map.putString("uri", "file://" + dstPath + '/' + fileName);
			map.putString("path", dstPath + '/' + fileName);
			map.putInt("width", bitmap.getWidth());
			map.putInt("height", bitmap.getHeight());
			
			File ofile = new File(dstPath + '/' + fileName);
			
			map.putInt("sizeByte",(int) ofile.length());
			map.putDouble ("sizeKb",((double) ofile.length())/1024);
			map.putDouble("sizeMb",((double) ofile.length())/(1024*1024));
			
			callback.invoke(map);
		}
		else{
			callback.invoke("File not found from the source path while creating thumbnail");
		}
		
	} catch (Exception e) {
		callback.invoke(e.getMessage());
	}
  }

  private static Bitmap extractThumbnail(Bitmap source, int width, int height, int options) {
    if (source == null)
      return null;

    float scale;
    if (source.getWidth() < source.getHeight())
      scale = width / (float) source.getWidth();
    else
      scale = height / (float) source.getHeight();
    Matrix matrix = new Matrix();
    matrix.setScale(scale, scale);
    Bitmap thumbnail = transform(matrix, source, width, height, options);
    return thumbnail;
  }

  private static Bitmap transform(Matrix scaler, Bitmap source, int targetWidth, int targetHeight, int options) {
    // boolean scaleUp = (options & OPTIONS_SCALE_UP) != 0;
    boolean recycle = (options & OPTIONS_RECYCLE_INPUT) != 0;

    // int deltaX = source.getWidth() - targetWidth;
    // int deltaY = source.getHeight() - targetHeight;
    // if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
      // Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
      // Canvas c = new Canvas(b2);

      // int deltaXHalf = Math.max(0, deltaX / 2);
      // int deltaYHalf = Math.max(0, deltaY / 2);
      // Rect src = new Rect(deltaXHalf, deltaYHalf, deltaXHalf + Math.min(targetWidth, source.getWidth()), deltaYHalf + Math.min(targetHeight, source.getHeight()));
      // int dstX = (targetWidth - src.width()) / 2;
      // int dstY = (targetHeight - src.height()) / 2;
      // Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight - dstY);
      // c.drawBitmap(source, src, dst, null);
      // if (recycle)
        // source.recycle();
      // return b2;
    // }

    float bitmapWidthF = source.getWidth();
    float bitmapHeightF = source.getHeight();
    float bitmapAspect = bitmapWidthF / bitmapHeightF;
    float viewAspect = (float) targetWidth / targetHeight;

    float scale = bitmapAspect > viewAspect ? targetHeight / bitmapHeightF : targetWidth / bitmapWidthF;
    if (scale < .9F || scale > 1F)
      scaler.setScale(scale, scale);
    else
      scaler = null;

    Bitmap b1;
    if (scaler != null)
      b1 = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), scaler, false);
    else
      b1 = source;

    if (recycle && b1 != source)
      source.recycle();

    int dx1 = Math.max(0, b1.getWidth() - targetWidth);
    int dy1 = Math.max(0, b1.getHeight() - targetHeight);

    Bitmap b2 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth, targetHeight);

    if (b2 != b1 && (recycle || b1 != source))
      b1.recycle();
	
    return b2;
  }

    private String getTmpDir() {
		final Activity activity = getCurrentActivity();
        String tmpDir = activity.getCacheDir()+ "";
        //Boolean created = new File(tmpDir).mkdir();

        return tmpDir;
    }
  
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        fileOrDirectory.delete();
    }
  
    @ReactMethod
    public void clean(final Promise promise) {

        final Activity activity = getCurrentActivity();
        final ImageCropperModule module = this;

        if (activity == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }

        permissionsCheck(activity, promise, Arrays.asList(Manifest.permission.WRITE_EXTERNAL_STORAGE), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    File file = new File(getTmpDir());
                    if (!file.exists()) throw new Exception("File does not exist");

                    deleteRecursive(file);
                    promise.resolve(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    promise.reject(E_ERROR_WHILE_CLEANING_FILES, ex.getMessage());
                }

                return null;
            }
        });
    }

    private void permissionsCheck(final Activity activity, final Promise promise, final List<String> requiredPermissions, final Callable<Void> callback) {

        List<String> missingPermissions = new ArrayList<>();

        for (String permission : requiredPermissions) {
            int status = ActivityCompat.checkSelfPermission(activity, permission);
            if (status != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (!missingPermissions.isEmpty()) {

            ((PermissionAwareActivity) activity).requestPermissions(missingPermissions.toArray(new String[missingPermissions.size()]), 1, new PermissionListener() {

                @Override
                public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                    if (requestCode == 1) {

                        for (int grantResult : grantResults) {
                            if (grantResult == PackageManager.PERMISSION_DENIED) {
                                promise.reject(E_PERMISSIONS_MISSING, "Required permission missing");
                                return true;
                            }
                        }

                        try {
                            callback.call();
                        } catch (Exception e) {
                            promise.reject(E_CALLBACK_ERROR, "Unknown error", e);
                        }
                    }

                    return true;
                }
            });

            return;
        }

        // all permissions granted
        try {
            callback.call();
        } catch (Exception e) {
            promise.reject(E_CALLBACK_ERROR, "Unknown error", e);
        }
    }

  
  
}