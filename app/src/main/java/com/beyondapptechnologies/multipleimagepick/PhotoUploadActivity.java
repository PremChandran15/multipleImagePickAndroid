package com.beyondapptechnologies.multipleimagepick;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class PhotoUploadActivity extends AppCompatActivity implements View.OnClickListener {

    private String mListID,mInspectionID;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;


    private Button openCustomGallery;
    private Button mUploadPhoto;
    private GridView selectedImageGridView;

    private static final int CustomGallerySelectId = 1;//Set Intent Id
    public static final String CustomGalleryIntentKey = "ImageArray";//Set Intent Key Value
    private List<String> selectedImages;
    private GalleryAdapter adapter;
    private String imagesArray;
    private String timestampString;
    private String formattedTimestamp;
    private Long timestamp;
    private int failUplaod;
    private int successUpload;
    private ProgressDialog uploadProgress;
    private String pathPhoto;
    private String remarkPhoto;
    private int currentprogress;
    private ImageView currentImage;
    private String mTestID;
    private String mKeyID, mAddress, mLocationKey, mRegion;
    private HashMap<String,Object> photos = new HashMap<>();
    private HashMap<String, String> metadata = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_upload);

        setTitle("Photo Upload");

        uploadProgress = new ProgressDialog(PhotoUploadActivity.this);
        uploadProgress.setTitle("Uploading Picture");
        uploadProgress.setMessage("Uploading picture...");
        uploadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        uploadProgress.setProgress(0);
        uploadProgress.setCancelable(true);

        initViews();
        setListeners();

        firebaseStorage = FirebaseStorage.getInstance();

        //Storage Bucket from Google Storage. Do not alter!!
        storageReference = firebaseStorage.getReferenceFromUrl("gs://pj-smart-citizen.appspot.com");

        final StorageReference imagesRefPath = storageReference.child("images");
        final DatabaseReference checklistRef = FirebaseDatabase.getInstance().getReference().child("Checklist").child("photos");

        mUploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedImageGridView.getChildCount() == 0) {
                    Toast.makeText(PhotoUploadActivity.this, "You have not selected any picture!", Toast.LENGTH_SHORT).show();
                    return;
                }

                successUpload = 0;
                failUplaod = 0;

                AlertDialog.Builder forwardBuilder = new AlertDialog.Builder(PhotoUploadActivity.this)
                        .setTitle(R.string.upload_dialog_title)
                        .setMessage(R.string.photo_upload_dialog_message)
                        .setNegativeButton(R.string.upload_dialog_cancel_button, null)
                        .setPositiveButton(R.string.upload_dialog_ok_button, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                UploadTask uploadTask;

                                for (int i = 0; i < selectedImages.size(); i++) {

//                                    final int j = i;

                                    final Uri uri = Uri.parse("file://" + selectedImages.get(i));

                                    //final StorageReference newImage = storageReference.child("images/" + uri.getLastPathSegment());
                                    final StorageReference newImage = imagesRefPath.child(uri.getLastPathSegment());
                                    uploadTask = newImage.putFile(uri);

                                    uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                            //todo: if want to make this the full progress bar, just need to make this as the sum of all progress and add to the main progress dialog
                                            double progress = (100.0 * (taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                                            //System.out.println("Upload is " + progress + "% done");
                                            currentprogress = (int) progress;

                                            uploadProgress.setMessage("Uploading Image: " + uri.getLastPathSegment() + "...");
                                            uploadProgress.show();

                                            uploadProgress.setProgress(currentprogress);

                                        }
                                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                                            uploadProgress.dismiss();

                                            View currentView;
                                            ViewGroup currentViewGroup;

                                            int childCount = adapter.getCount();
                                            Log.i("Child Count:", " " + childCount);

                                            for(int j=0; j<selectedImageGridView.getChildCount();j++) {
                                                currentView = selectedImageGridView.getChildAt(j);
                                                if (currentView instanceof ViewGroup) {
                                                    for (int k = 0; k < ((ViewGroup) currentView).getChildCount(); k++) {
                                                        if (((ViewGroup) currentView).getChildAt(k) instanceof EditText) {
                                                            String tagET = (((ViewGroup) currentView).getChildAt(k)).getTag().toString();
                                                            Log.i("Tag EditText: ", tagET);
                                                            if (tagET.equalsIgnoreCase(uri.toString())) {
                                                                remarkPhoto = ((EditText) ((ViewGroup) currentView).getChildAt(k)).getText().toString();
                                                                if (remarkPhoto.length() == 0) {
                                                                    remarkPhoto = "N/A";
                                                                }
                                                            }
                                                        } else if (((ViewGroup) currentView).getChildAt(k) instanceof ImageView) {
                                                            currentImage = (ImageView) ((ViewGroup) currentView).getChildAt(k);
                                                            currentImage.setAlpha(1F);
                                                        }
                                                    }
                                                }
                                            }

                                            pathPhoto = uri.getLastPathSegment();

                                            Log.i("Path photo: ", pathPhoto);
                                            Log.i("Remark photo: ", remarkPhoto);

                                            //metadata.put("Filename" + j, pathPhoto);
                                            metadata.put("Filename", pathPhoto);

                                            //These are to write the photo paths to Realtime Database so that can be extracted later for angular
                                            DatabaseReference pushPhotos = checklistRef.push();

                                            pushPhotos.setValue(metadata).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(Exception e) {
                                                    Toast.makeText(PhotoUploadActivity.this, "Could not write metadata of this picture on server: " + pathPhoto + " because " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });

                                            StorageMetadata storageMetadata = new StorageMetadata.Builder()
                                                    .setCustomMetadata("Photo path", pathPhoto)
                                                    .setCustomMetadata("Remark", remarkPhoto)
                                                    .build();

                                            newImage.updateMetadata(storageMetadata).addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                                @Override
                                                public void onSuccess(StorageMetadata storageMetadata) {
                                                    successUpload++;

                                                    if (successUpload == selectedImages.size()) {

                                                        AlertDialog.Builder uploadMoreBuilder = new AlertDialog.Builder(PhotoUploadActivity.this)
                                                                .setTitle(R.string.uploadmore_dialog_title)
                                                                .setMessage(successUpload + " Photos are successfully uploaded to the server. Do you want to upload more?")
                                                                .setNegativeButton(R.string.uploadmore_dialog_cancel_button, null)
                                                                .setPositiveButton(R.string.uploadmore_dialog_ok_button, new DialogInterface.OnClickListener() {
                                                                    @Override
                                                                    public void onClick(DialogInterface dialog, int which) {

                                                                        Intent intent = new Intent(PhotoUploadActivity.this, CustomGalleryActivity.class);
                                                                        startActivityForResult(intent, CustomGallerySelectId);
                                                                    }
                                                                });
                                                        uploadMoreBuilder.create().show();
                                                    }
                                                    //Add failure listener to see if remark can be updated or not
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(Exception e) {
                                                    Toast.makeText(PhotoUploadActivity.this, "Could not write remark of this picture on server: " + pathPhoto + " because " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                }
                                            });

                                            Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                        }
                                        //Add main failure listener to see if photo can be uploaded or not
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(Exception e) {
                                            uploadProgress.dismiss();

                                            failUplaod++;

                                            Toast.makeText(PhotoUploadActivity.this, failUplaod + "photo(s) could not be uploaded. Upload failed because: " + e.getMessage(), Toast.LENGTH_LONG).show();


                                        }
                                    });

                                }

                            }

                        });
                forwardBuilder.create().show();
            }
        });

        selectedImageGridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder deleteDialog = new AlertDialog.Builder(PhotoUploadActivity.this)
                        .setTitle("Delete Item?")
                        .setMessage("Do you want to remove this item?")
                        .setNegativeButton("No", null)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                adapter.removeitem(position);
                                adapter.notifyDataSetChanged();

                                /*Or you can do it this way if the top one doesnt work:

                                selectedImageGridView.setAdapter(null);*/
                            }
                        });

                deleteDialog.create().show();


                return true;
            }
        });


    }

    private void initViews() {
        openCustomGallery = (Button) findViewById(R.id.openCustomGallery);
        selectedImageGridView = (GridView) findViewById(R.id.selectedImagesGridView);
        mUploadPhoto = (Button) findViewById(R.id.UploadPhotos);

    }

    //set Listeners
    private void setListeners() {
        openCustomGallery.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.openCustomGallery:
                //Start Custom Gallery Activity by passing intent id
                Intent intent = new Intent(PhotoUploadActivity.this, CustomGalleryActivity.class);
                startActivityForResult(intent, CustomGallerySelectId);
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestcode, int resultcode, Intent imagereturnintent) {
        super.onActivityResult(requestcode, resultcode, imagereturnintent);
        switch (requestcode) {
            case CustomGallerySelectId:
                if (resultcode == RESULT_OK) {
                    imagesArray = imagereturnintent.getStringExtra(CustomGalleryIntentKey);//get Intent data
                    //Convert string array into List by splitting by ',' and substring after '[' and before ']'
                    selectedImages = Arrays.asList(imagesArray.substring(1, imagesArray.length() - 1).split(", "));
                    //loadGridView(new ArrayList<String>(selectedImages));//call load gridview method by passing converted list into arrayList
                    adapter = new GalleryAdapter(PhotoUploadActivity.this,new ArrayList<>(selectedImages),false);
                    selectedImageGridView.setAdapter(adapter);

                }
                break;

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    //I'm saving the instance state of photos.. let's see how

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("Photos",photos);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        savedInstanceState.getSerializable("Photos");
    }
}
