package com.bonsapps.grocery.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bonsapps.grocery.R;
import com.bonsapps.grocery.constants.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class AddProductActivity extends AppCompatActivity {

    // ui views
    private ImageButton backBtn;
    private ImageView productIconIv;
    private EditText titleEt, descriptionEt, quantityEt, priceEt, discountedEt, discountedNoteEt;
    private TextView categoriesTv;
    private SwitchCompat discountSwitch;
    private Button addProductBtn;

    // permissions constants
    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int STORAGE_REQUEST_CODE = 300;
    // image pick constants
    private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private static final int IMAGE_PICK_CAMERA_CODE = 500;
    // permisson arrays
    private String[] cameraPermissions;
    private String[] storagePermissions;
    // image picked uri
    private Uri imageUri;

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        // init ui views
        backBtn =  findViewById(R.id.backBtn);
        productIconIv =  findViewById(R.id.productIconIv);
        titleEt =  findViewById(R.id.titleEt);
        descriptionEt =  findViewById(R.id.descriptionEt);
        categoriesTv =  findViewById(R.id.categoriesTv);
        quantityEt =  findViewById(R.id.quantityEt);
        priceEt =  findViewById(R.id.priceEt);
        discountSwitch =  findViewById(R.id.discountSwitch);
        discountedEt =  findViewById(R.id.discountedEt);
        discountedNoteEt =  findViewById(R.id.discountedNoteEt);
        addProductBtn =  findViewById(R.id.addProductBtn);
        // unchecked hide discountPriceEt, discountNoteEt
        discountedEt.setVisibility(View.GONE);
        discountedNoteEt.setVisibility(View.GONE);
        // Init array Permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        // init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        // init progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        discountSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // checked, show discountPriceEt, discountNoteEt
                    discountedEt.setVisibility(View.VISIBLE);
                    discountedNoteEt.setVisibility(View.VISIBLE);
                } else {
                    // unchecked hide discountPriceEt, discountNoteEt
                    discountedEt.setVisibility(View.GONE);
                    discountedNoteEt.setVisibility(View.GONE);
                }
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        
        productIconIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show dialog to pick image
                showImagePickDialog();
            }
        });

        categoriesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // select categories
                categoryDialog();
            }
        });

        addProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Flow:
                //1) Input Data
                //2) Validate Data
                //3) Add to DB
                inputData();
            }
        });
    }

    private String productTitle, productDescription, productCategory, productQuantity, originalPrice, discountPrice, discountNote;
    private boolean discountAvailable = false;
    private void inputData() {
        //1) Input Data
        productTitle = titleEt.getText().toString().trim();
        productDescription = descriptionEt.getText().toString().trim();
        productCategory = categoriesTv.getText().toString().trim();
        productQuantity = quantityEt.getText().toString().trim();
        originalPrice = priceEt.getText().toString().trim();
        discountAvailable = discountSwitch.isChecked(); // true/false
        //2) Validate Data
        if (TextUtils.isEmpty(productTitle)) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return; // don't proceed further
        }
        if (TextUtils.isEmpty(productDescription)) {
            Toast.makeText(this, "Description is required", Toast.LENGTH_SHORT).show();
            return; // don't proceed further
        }
        if (TextUtils.isEmpty(productCategory)) {
            Toast.makeText(this, "Product Category is required", Toast.LENGTH_SHORT).show();
            return; // don't proceed further
        }
        if (TextUtils.isEmpty(productQuantity)) {
            Toast.makeText(this, "Product Quantity is required", Toast.LENGTH_SHORT).show();
            return; // don't proceed further
        }
        if (TextUtils.isEmpty(originalPrice)) {
            Toast.makeText(this, "Price is required", Toast.LENGTH_SHORT).show();
            return; // don't proceed further
        }

        if (discountAvailable) {
            // product is with discount
            discountPrice = discountedEt.getText().toString().trim();
            discountNote = discountedNoteEt.getText().toString().trim();

            if (TextUtils.isEmpty(discountPrice)) {
                Toast.makeText(this, "Discount Price is required", Toast.LENGTH_SHORT).show();
                return; // don't proceed further
            }
            if (TextUtils.isEmpty(discountNote)) {
                Toast.makeText(this, "Discount Note is required", Toast.LENGTH_SHORT).show();
                return; // don't proceed further
            }
        } else {
            // product is without discount
            discountPrice = "0";
            discountNote = "";
        }

        addProduct();
    }

    private void addProduct() {
        // 3) Add data to db
        progressDialog.setMessage("Adding Product...");
        progressDialog.show();

        final String timestamp = ""+System.currentTimeMillis();

        if (imageUri == null) {
            // upload without image

            // setup data to upload
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("productId",""+timestamp);
            hashMap.put("productTitle",""+productTitle);
            hashMap.put("productDescription",""+productDescription);
            hashMap.put("productCategory",""+productCategory);
            hashMap.put("productQuantity",""+productQuantity);
            hashMap.put("productIcon",""); // no image, set empty
            hashMap.put("originalPrice",""+originalPrice);
            hashMap.put("discountPrice",""+discountPrice);
            hashMap.put("discountNote",""+discountNote);
            hashMap.put("discountAvailable",""+discountAvailable);
            hashMap.put("timestamp",""+timestamp);
            hashMap.put("uid",""+firebaseAuth.getUid());
            // add to db
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
            ref.child(firebaseAuth.getUid()).child("Products").child(timestamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // Add to db
                            progressDialog.dismiss();
                            Toast.makeText(AddProductActivity.this, "Product Added!...", Toast.LENGTH_SHORT).show();
                            clearData();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //Failed to add to db
                    progressDialog.dismiss();
                    Toast.makeText(AddProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // upload with image

            // first upload image to storage

            // name and path of image to be uploaded
            String filePathAndName = "product_image/"+""+timestamp;
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // image uploaded
                    // get url of uploaded image
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful());
                    Uri downloadImageUri = uriTask.getResult();

                    if (uriTask.isSuccessful()) {
                        // url of image received, upload to db
                        // setup data to upload

                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("productId",""+timestamp);
                        hashMap.put("productTitle",""+productTitle);
                        hashMap.put("productDescription",""+productDescription);
                        hashMap.put("productCategory",""+productCategory);
                        hashMap.put("productQuantity",""+productQuantity);
                        hashMap.put("productIcon",""+downloadImageUri); // no image, set empty
                        hashMap.put("originalPrice",""+originalPrice);
                        hashMap.put("discountPrice",""+discountPrice);
                        hashMap.put("discountNote",""+discountNote);
                        hashMap.put("discountAvailable",""+discountAvailable);
                        hashMap.put("timestamp",""+timestamp);
                        hashMap.put("uid",""+firebaseAuth.getUid());
                        // add to db
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                        ref.child(firebaseAuth.getUid()).child("Products").child(timestamp).setValue(hashMap)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // Add to db
                                        progressDialog.dismiss();
                                        Toast.makeText(AddProductActivity.this, "Product Added!...", Toast.LENGTH_SHORT).show();
                                        clearData();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //Failed to add to db
                                progressDialog.dismiss();
                                Toast.makeText(AddProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // failed to upload image
                    progressDialog.dismiss();
                    Toast.makeText(AddProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

    private void clearData() {
        // clear data after uploading product
        titleEt.setText("");
        descriptionEt.setText("");
        categoriesTv.setText("");
        quantityEt.setText("");
        priceEt.setText("");
        discountedEt.setText("");
        discountedNoteEt.setText("");
        productIconIv.setImageResource(R.drawable.ic_add_shopping_primary);
        imageUri = null;
    }

    private void categoryDialog() {
        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Products Category");
        builder.setItems(Constants.productCategories, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Get picked categories
                String categories = Constants.productCategories[which];
                // Set picked categories
                categoriesTv.setText(categories);
            }
        }).show();
    }

    private void showImagePickDialog() {
        // options to display in dialog
        String[] options = {"Camera","Gallery"};

        // dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Image");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // Camera clicked
                    if (checkCameraPermissions()) {
                        // permission granted
                        pickFromCamera();
                    } else {
                        // permission not granted, request
                        requestCameraPermission();
                    }
                } else {
                    // gallery picked
                    if (checkStoragePermission()) {
                        // permission granted
                        pickFromGallery();
                    } else {
                        // permission not granted, request
                        requestStoragePermission();
                    }
                }
            }
        }).show();
    }

    private void pickFromGallery() {
        // intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        // intent to pick image from camera

        // using mediastore to pick high/original quality image
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp_Image_Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION,"Temp_Image_Description");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues);

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                (PackageManager.PERMISSION_GRANTED);
        return result; // return true/false
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermissions() {
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) ==
                (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,cameraPermissions, CAMERA_REQUEST_CODE);
    }
    // handle permissions result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && storageAccepted) {
                        //both permissions granted
                        pickFromCamera();
                    } else {
                        // both/one permission not granted
                        Toast.makeText(this, "Camera & Storage permissions are required!...", Toast.LENGTH_SHORT).show();
                    }
                }
            }case STORAGE_REQUEST_CODE:{
                if (grantResults.length > 0) {
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (storageAccepted) {
                        // permission granted
                        pickFromGallery();
                    } else {
//                        permission denied
                        Toast.makeText(this, "Storage permission is required!...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    // handle image pick result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                // camera picked from gallery

                // save image picked uri
                imageUri = data.getData();

                // set to imageView
                productIconIv.setImageURI(imageUri);
            } else if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                // camera picked from gallery
                productIconIv.setImageURI(imageUri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}
