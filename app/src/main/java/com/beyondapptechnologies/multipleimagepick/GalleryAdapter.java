package com.beyondapptechnologies.multipleimagepick;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by premkumar on 15/09/2017.
 */

public class GalleryAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<String> imageUrls;
    private SparseBooleanArray mSparseBooleanArray;//Variable to store selected Images
    private DisplayImageOptions options;
    private boolean isCustomGalleryActivity;//Variable to check if gridview is to setup for Custom Gallery or not
    public HashMap<String,String> remarkMap = new HashMap<>();

    public GalleryAdapter(Context context, ArrayList<String> imageUrls, boolean isCustomGalleryActivity) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.isCustomGalleryActivity = isCustomGalleryActivity;
        mSparseBooleanArray = new SparseBooleanArray();

        //This is from the Nostra Universal Image Loader to configure the image options.. maybe if I do not reset view or something maybe I will not have the images cleared la I think
        options = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .resetViewBeforeLoading(true).cacheOnDisk(true)
                .considerExifParams(true).bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    //Method to return selected Images
    public ArrayList<String> getCheckedItems() {
        ArrayList<String> mTempArry = new ArrayList<String>();

        for (int i = 0; i < imageUrls.size(); i++) {
            if (mSparseBooleanArray.get(i)) {
                mTempArry.add(imageUrls.get(i));
            }
        }

        return mTempArry;
    }

    @Override
    public int getCount() {
        return imageUrls.size();
    }

    @Override
    public Object getItem(int i) {
        return imageUrls.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        //the if else here is to check if the current view is not null, then inflate the current view, this was to prevent the pictures and edit text values getting messed up while scrolling. Like for example, the top picture was at bottom and likewise the bottom pic was at top and vice versa.
        View view;
        if (convertView == null) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.customgridview_item, viewGroup, false);//Inflate layout


        }else {
            view = (View) convertView;

        }


        CheckBox mCheckBox = (CheckBox) view.findViewById(R.id.selectCheckBox);
        final ImageView imageView = (ImageView) view.findViewById(R.id.galleryImageView);
        final EditText mEditText = (EditText) view.findViewById(R.id.pictureRemark);
        //EditText mEditText = (EditText) view.findViewById(position);

        //If Context is PhotoUploadActivity then hide checkbox
        if (!isCustomGalleryActivity) {
            mCheckBox.setVisibility(View.GONE);
            imageView.setAlpha(0.65F);
        }else mEditText.setVisibility(View.GONE);

        ImageLoader.getInstance().displayImage("file://" + imageUrls.get(position), imageView, options);//Load Images over ImageView

        mEditText.setTag("file://" + imageUrls.get(position));
        //imageView.setTag("file://" + imageUrls.get(position));

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s != null || s.length()>0){
                    String remark = s.toString();
                    remarkMap.put(mEditText.getTag().toString(),remark);
                }else if (s.length() == 0) {
                    String remark = "";
                    remarkMap.put(mEditText.getTag().toString(),remark);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s!=null) {
                    String remark = s.toString();
                    remarkMap.put(mEditText.getTag().toString(), remark);
                }
            }
        });

        mCheckBox.setTag(position);//Set Tag for CheckBox
        mCheckBox.setChecked(mSparseBooleanArray.get(position));
        mCheckBox.setOnCheckedChangeListener(mCheckedChangeListener);

        return view;

    }

    //this is what I have to override if I wanna display the dialog box on item selected to crosscheck
    CompoundButton.OnCheckedChangeListener mCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            //this works well as of 6 October 2016 1630 hrs

            Uri uri = Uri.parse("file://" + getItem((Integer) buttonView.getTag()));
            String url = uri.getLastPathSegment();

            HashMap < String, Boolean > dialogBox = ((CustomGalleryActivity) context).getDialogStatus();

            if(dialogBox.get(url) == null){
                dialogBox.put(url,false);
            }

            mSparseBooleanArray.put((Integer) buttonView.getTag(), isChecked);//Insert selected checkbox value inside boolean array
            ((CustomGalleryActivity) context).showSelectButton();//call custom gallery activity method

            if (isChecked && !dialogBox.get(url)) {
                ((CustomGalleryActivity) context).comparePhoto((Integer) buttonView.getTag());
            }


        }
    };

    //Getter for the remarkMap HashMap
    public String getMap(String tag){

        return remarkMap.get(tag);
    }

    public void clearData(){
        imageUrls.clear();
    }

    public void removeitem(int position){
        imageUrls.remove(position);

    }
}
