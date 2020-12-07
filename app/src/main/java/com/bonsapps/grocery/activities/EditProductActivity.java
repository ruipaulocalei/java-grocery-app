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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

public class EditProductActivity extends AppCompatActivity {

    private String productId;

    // ui views
    private ImageButton backBtn;
    private ImageView productIconIv;
    private EditText titleEt, descriptionEt, quantityEt, priceEt, discountedEt, discountedNoteEt;
    private TextView categoriesTv;
    private SwitchCompat discountSwitch;
    private Button updateProductBtn;

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
        setContentView(R.layout.activity_edit_product);

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
        updateProductBtn =  findViewById(R.id.updateProductBtn);

        //get id of the product from intent
        productId = getIntent().getStringExtra("productId");

        // unchecked hide discountPriceEt, discountNoteEt
        discountedEt.setVisibility(View.GONE);
        discountedNoteEt.setVisibility(View.GONE);
        // Init array Permissions
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        // init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();
        loadProductDetails(); //to set on views
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

        updateProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Flow:
                //1) Input Data
                //2) Validate Data
                //3) update to DB
                inputData();
            }
        });
    }

    private void loadProductDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(firebaseAuth.getUid()).child("Products").child(productId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //get data
                        String id = ""+dataSnapshot.child("productId").getValue();
                        String productTitle = ""+dataSnapshot.child("productTitle").getValue();
                        String productDescription = ""+dataSnapshot.child("productDescription").getValue();
                        String productCategory = ""+dataSnapshot.child("productCategory").getValue();
                        String productQuantity = ""+dataSnapshot.child("productQuantity").getValue();
                        String productIcon = ""+dataSnapshot.child("productIcon").getValue();
                        String originalPrice = ""+dataSnapshot.child("originalPrice").getValue();
                        String discountPrice = ""+dataSnapshot.child("discountPrice").getValue();
                        String discountNote = ""+dataSnapshot.child("discountNote").getValue();
                        String discountAvailable = ""+dataSnapshot.child("discountAvailable").getValue();
                        String timestamp = ""+dataSnapshot.child("timestamp").getValue();
                        String uid = ""+dataSnapshot.child("uid").getValue();

                        //set data to view
                        if (discountAvailable.equals("true")) {
                            discountSwitch.setChecked(true);
                            priceEt.setVisibility(View.VISIBLE);
                            discountedNoteEt.setVisibility(View.VISIBLE);
                        } else {
                            discountSwitch.setChecked(false);
                            priceEt.setVisibility(View.GONE);
                            discountedNoteEt.setVisibility(View.GONE);
                        }

                        titleEt.setText(productTitle);
                        descriptionEt.setText(productDescription);
                        categoriesTv.setText(productCategory);
                        quantityEt.setText(productQuantity);
                        priceEt.setText(originalPrice);
                        discountedNoteEt.setText(discountNote);
                        discountedEt.setText(discountPrice);

                        try {
                            Picasso.get().load(productIcon).placeholder(R.drawable.ic_add_shopping_white).into(productIconIv);
                        } catch (Exception e) {
                            productIconIv.setImageResource(R.drawable.ic_add_shopping_white);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

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

        updateProduct();
    }

    private void updateProduct() {
        //show progress
        progressDialog.setMessage("Updating product...");
        progressDialog.show();

        if (imageUri == null) {
            //update without image

            //setup data in hashmap to update
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("productTitle",""+productTitle);
            hashMap.put("productDescription",""+productDescription);
            hashMap.put("productCategory",""+productCategory);
            hashMap.put("productQuantity",""+productQuantity);
            hashMap.put("originalPrice",""+originalPrice);
            hashMap.put("discountPrice",""+discountPrice);
            hashMap.put("discountNote",""+discountNote);
            hashMap.put("discountAvailable",""+discountAvailable);

            //update to db
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
            reference.child(firebaseAuth.getUid()).child("Products").child(productId).updateChildren(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            progressDialog.dismiss();
                            Toast.makeText(EditProductActivity.this, "updated!...", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(EditProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            //update without image

            //first upload image
            //image name and path on firebase storage
            String filePathAndName = "product_image/"+""+productId;//overide previous image using same id
            //upload image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePathAndName);
            storageReference.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // image upload, get url of uploaded image
                            Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                            while (!uriTask.isSuccessful());
                            Uri downloadImageUri = uriTask.getResult();
                            if (uriTask.isSuccessful()) {
                                //setup data in hashmap to update
                                HashMap<String, Object> hashMap = new HashMap<>();
                                hashMap.put("productTitle",""+productTitle);
                                hashMap.put("productDescription",""+productDescription);
                                hashMap.put("productCategory",""+productCategory);
                                hashMap.put("productIcon",""+downloadImageUri);
                                hashMap.put("productQuantity",""+productQuantity);
                                hashMap.put("originalPrice",""+originalPrice);
                                hashMap.put("discountPrice",""+discountPrice);
                                hashMap.put("discountNote",""+discountNote);
                                hashMap.put("discountAvailable",""+discountAvailable);

                                //update to db
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
                                reference.child(firebaseAuth.getUid()).child("Products").child(productId).updateChildren(hashMap)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressDialog.dismiss();
                                                Toast.makeText(EditProductActivity.this, "updated!...", Toast.LENGTH_SHORT).show();
                                            }
                                        }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        Toast.makeText(EditProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    progressDialog.dismiss();
                    Toast.makeText(EditProductActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
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
