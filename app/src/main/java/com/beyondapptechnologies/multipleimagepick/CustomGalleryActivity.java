package com.beyondapptechnologies.multipleimagepick;

import android.*;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CustomGalleryActivity extends AppCompatActivity implements View.OnClickListener{

    private Button selectImages;
    private GridView galleryImagesGridView;
    private ArrayList<String> galleryImageUrls;
    private GalleryAdapter imagesAdapter;
    private TextView mNoPhotos;
    private String mInspectionID, mKeyID,mAddress;
    private DatabaseReference checklistRef;
    private Query queryRef;
    private Map<String, String> photos;
    private Boolean isChecked;
    private HashMap<String,Boolean> dialogBoxes = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_gallery);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();

        initViews();
        setListeners();

        setTitle("Photo Gallery - Please choose pictures");

        int permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 23);
        } else {

            fetchGalleryImages();

            setUpGridView();

            if(imagesAdapter.getCount() == 0){
                mNoPhotos.setVisibility(View.VISIBLE);
            }else{
                mNoPhotos.setVisibility(View.GONE);
            }

            //Todo: change photo reference based on your own database reference
            checklistRef = FirebaseDatabase.getInstance().getReference().child("Checklist").child("photos");
            queryRef = checklistRef.orderByChild("Filename");

        }

    }

    //Init all views
    private void initViews() {
        selectImages = (Button) findViewById(R.id.selectImagesBtn);
        galleryImagesGridView = (GridView) findViewById(R.id.galleryImagesGridView);
        mNoPhotos = (TextView) findViewById(R.id.noPhotosTextView);

    }

    //fetch all images from gallery
    private void fetchGalleryImages() {
        final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};//get all columns of type images
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN;//order data by date
        Cursor imagecursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
                null, orderBy + " DESC");//get all data in Cursor by sorting in DESC order

        galleryImageUrls = new ArrayList<String>();//Init array

        //Loop to cursor count
        for (int i = 0; i < imagecursor.getCount(); i++) {
            imagecursor.moveToPosition(i);
            int dataColumnIndex = imagecursor.getColumnIndex(MediaStore.Images.Media.DATA);//get column index
            galleryImageUrls.add(imagecursor.getString(dataColumnIndex));//get Image from column index
            System.out.println("Array path" + galleryImageUrls.get(i));
        }

    }

    //Set Up GridView method
    private void setUpGridView() {

        imagesAdapter = new GalleryAdapter(CustomGalleryActivity.this, galleryImageUrls, true);
        galleryImagesGridView.setAdapter(imagesAdapter);

    }

    //Set Listeners method
    private void setListeners() {
        selectImages.setOnClickListener(this);
    }


    //Show hide select button if images are selected or deselected
    public void showSelectButton() {
        ArrayList<String> selectedItems = imagesAdapter.getCheckedItems();
        if (selectedItems.size() > 0 && selectedItems.size() < 10) {
            selectImages.setText(selectedItems.size() + " - Images Selected");
            selectImages.setVisibility(View.VISIBLE);
            selectImages.setTextColor(ContextCompat.getColor(CustomGalleryActivity.this, R.color.colorWhite));
        }else if(selectedItems.size() == 10){
            selectImages.setText(selectedItems.size() + " - Images Selected");
            selectImages.setVisibility(View.VISIBLE);
            selectImages.setTextColor(ContextCompat.getColor(CustomGalleryActivity.this, R.color.colorWhite));
            Toast.makeText(CustomGalleryActivity.this, "You have reached the maximum number of pictures allowed per upload. Click on the Green button to proceed.", Toast.LENGTH_SHORT).show();

        }
        else if (selectedItems.size() >= 11){
            selectImages.setVisibility(View.VISIBLE);
            selectImages.setTextColor(ContextCompat.getColor(CustomGalleryActivity.this, R.color.colorLightGreen));
            selectImages.setText(selectedItems.size() + " - Images Selected");
            AlertDialog.Builder maxPicUpload = new AlertDialog.Builder(CustomGalleryActivity.this)
                    .setTitle("Maximum number of picture reached")
                    .setIcon(R.drawable.alerticon)
                    .setMessage("You are only allowed to select a maximum of 10 pictures at a time. Click 'Ok' to go back and choose only 10")
                    .setPositiveButton("Ok",null);
            maxPicUpload.create().show();
        }

        else if(selectedItems.size() == 0) {
            selectImages.setVisibility(View.GONE);
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.selectImagesBtn:

                //When button is clicked then fill array with selected images
                ArrayList<String> selectedItems = imagesAdapter.getCheckedItems();

                if(selectedItems.size() <= 10) {

                    //Send back result to PhotoUploadActivity with selected images
                    Intent intent = new Intent();
                    intent.putExtra(PhotoUploadActivity.CustomGalleryIntentKey, selectedItems.toString());//Convert Array into string to pass data
                    setResult(RESULT_OK, intent);//Set result OK
                    finish();//finish activity
                }
                else if(selectedItems.size() > 10){
                    Toast.makeText(CustomGalleryActivity.this, "You can only select 10 pictures at a time.", Toast.LENGTH_LONG).show();
                    return;
                }

                break;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case (23):
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    fetchGalleryImages();
                    setUpGridView();

                    if(imagesAdapter.getCount() == 0){
                        mNoPhotos.setVisibility(View.VISIBLE);
                    }else{
                        mNoPhotos.setVisibility(View.GONE);
                    }
                }
                break;

            default:
                break;
        }

    }

    public void comparePhoto(final int tag){
        //ArrayList<String> selected = imagesAdapter.getCheckedItems();
//        for(int i=0; i<selected.size();i++){
        // String url = selected.get(tag);

        Uri uri = Uri.parse("file://" + imagesAdapter.getItem(tag));
        final String url = uri.getLastPathSegment();

        Log.i("Button tag: ", url);

        //isChecked = true;
        //dialogBoxes.put(url,false);


        queryRef.equalTo(url).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && !isFinishing()) {
                    AlertDialog.Builder photosExist = new AlertDialog.Builder(CustomGalleryActivity.this)
                            .setTitle("This photo already exists")
                            .setMessage("You have already uploaded this photo: " + url + "into the database. Are you sure you want to select this again?")
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    View v = galleryImagesGridView.findViewWithTag(tag);
                                    if (v instanceof CheckBox) {
                                        ((CheckBox) v).setChecked(false);
                                    }
                                }
                            })
                            .setPositiveButton("Yes", null);

                    photosExist.create().show();
                    dialogBoxes.put(url, true);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(CustomGalleryActivity.this,"Error occured due to: " + databaseError, Toast.LENGTH_LONG).show();

            }
        });
        //}
        //return isChecked;
    }

    public HashMap<String, Boolean> getDialogStatus(){
        return dialogBoxes;
    }
}
