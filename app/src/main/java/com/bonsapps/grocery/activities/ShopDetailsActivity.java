package com.bonsapps.grocery.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bonsapps.grocery.R;
import com.bonsapps.grocery.adapter.AdapterCartItem;
import com.bonsapps.grocery.adapter.AdapterProductUser;
import com.bonsapps.grocery.constants.Constants;
import com.bonsapps.grocery.models.CartItem;
import com.bonsapps.grocery.models.Product;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

import p32929.androideasysql_library.Column;
import p32929.androideasysql_library.EasyDB;

public class ShopDetailsActivity extends AppCompatActivity {

    //declaring ui views
    private ImageView shopIv;
    private TextView shopNameTv, phoneTv, emailTv, openCloseTv, deliveryFeeTv, addressTv, filteredProductsTv;
    private ImageButton callBtn, mapBtn, cartBtn, backBtn, filterProductBtn;
    private EditText searchProductEt;
    private RecyclerView productRv;

    private String myLatitude, myLongitude, myPhone;
    private String shopName, shopEmail, shopPhone, shopAddress, shopLatitude, shopLongitude;
    private String shopUid;
    public String deliveryFee;

    private FirebaseAuth firebaseAuth;

    private ArrayList<Product> productsList;
    private AdapterProductUser adapterProductUser;

    //progress dialog
    private ProgressDialog progressDialog;
    //cart
    private ArrayList<CartItem> cartItemList;
    private AdapterCartItem adapterCartItem;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop_details);
        //init ui viewa
        shopIv = findViewById(R.id.shopIv);
        shopNameTv = findViewById(R.id.shopNameTv);
        phoneTv = findViewById(R.id.phoneTv);
        emailTv = findViewById(R.id.emailTv);
        openCloseTv = findViewById(R.id.openCloseTv);
        deliveryFeeTv = findViewById(R.id.deliveryFeeTv);
        addressTv = findViewById(R.id.addressTv);
        callBtn = findViewById(R.id.callBtn);
        mapBtn = findViewById(R.id.mapBtn);
        cartBtn = findViewById(R.id.cartBtn);
        backBtn = findViewById(R.id.backBtn);
        searchProductEt = findViewById(R.id.searchProductEt);
        filterProductBtn = findViewById(R.id.filterProductBtn);
        filteredProductsTv = findViewById(R.id.filteredProductsTv);
        productRv = findViewById(R.id.productRv);
        //init progressdialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        shopUid = getIntent().getStringExtra("shopUid");
        firebaseAuth = FirebaseAuth.getInstance();
        loadMyInfo();
        loadShopDetails();
        loadShopProducts();
        //each shop have its own products and orders so if user add items to cart
//        and go back and open cart in different shop then cart should be different
        //so delete cart data whenever user open this activity
        deleteCartData();

        //search
        searchProductEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    adapterProductUser.getFilter().filter(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //go previous activity
                onBackPressed();
            }
        });

        cartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //show cart dialog
                showCartDialog();
            }
        });

        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialPhone();
            }
        });

        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMap();
            }
        });

        filterProductBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ShopDetailsActivity.this);
                builder.setTitle("Choose Category");
                builder.setItems(Constants.productCategories1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // get selected item
                        String selected = Constants.productCategories1[which];
                        filteredProductsTv.setText(selected);
                        if (selected.equals("All")) {
                            loadShopProducts();
                        } else {
                            // load filtered
                            adapterProductUser.getFilter().filter(selected);
                        }
                    }
                }).show();
            }
        });
    }

    private void deleteCartData() {
        EasyDB easyDB = EasyDB.init(this, "ITEMS_BD")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id", new String[]{"text","unique"}))
                .addColumn(new Column("Item_PID", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Name", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Price_Each", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Price", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Quantity", new String[]{"text","unique"}))
                .doneTableColumn();
        easyDB.deleteAllDataFromTable();//delete all records from cart
    }

    public double allTotalPrice = 0.00;
    //need to access these views in adapter so making public
    public TextView sTotalTv, dFeeTv, allTotalPriceTv;
    public Button checkoutBtn;

    private void showCartDialog() {
        //init list
        cartItemList = new ArrayList<>();
        //inflate cart layout
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_cart, null);
        //init views
        TextView shopNameTv = view.findViewById(R.id.shopNameTv);
        RecyclerView cartItemsRv = view.findViewById(R.id.cartItemsRv);
        sTotalTv = view.findViewById(R.id.sTotalTv);
        dFeeTv = view.findViewById(R.id.dFeeTv);
        allTotalPriceTv = view.findViewById(R.id.totalTv);
        checkoutBtn = view.findViewById(R.id.checkoutBtn);
        //alertdialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(view);

        shopNameTv.setText(shopName);

        EasyDB easyDB = EasyDB.init(this, "ITEMS_BD")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id", new String[]{"text","unique"}))
                .addColumn(new Column("Item_PID", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Name", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Price_Each", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Price", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Quantity", new String[]{"text","unique"}))
                .doneTableColumn();

        //get all records from db
        Cursor res = easyDB.getAllData();
        while (res.moveToNext()) {
            String id = res.getString(1);
            String pId = res.getString(2);
            String name = res.getString(3);
            String price = res.getString(4);
            String cost = res.getString(5);
            String quantity = res.getString(6);

            allTotalPrice += Double.parseDouble(cost);

            CartItem cartItem = new CartItem(""+id, ""+pId,""+name,
                    ""+price,""+cost,""+quantity);
            cartItemList.add(cartItem);
        }
        //setup adapter
        adapterCartItem =  new AdapterCartItem(this, cartItemList);
        //set to recyclerview
        cartItemsRv.setAdapter(adapterCartItem);

        dFeeTv.setText(deliveryFee+"Kz");
        sTotalTv.setText(String.format("%.2f", allTotalPrice)+"Kz");
        allTotalPriceTv.setText((allTotalPrice + Double.parseDouble(deliveryFee.replace("Kz","")))+"Kz");
        //show dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        //reset total price on dialog
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                allTotalPrice = 0.00;
            }
        });

        //place Order
        checkoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //first validate delivery address
                if (myLatitude.equals("") || myLatitude.equals("null") || myLongitude.equals("") || myLongitude.equals("null")) {
                    Toast.makeText(ShopDetailsActivity.this, "Please enter your address in your profile before", Toast.LENGTH_SHORT).show();
                    return; //don't procede further
                }
                if (myPhone.equals("") || myPhone.equals("null")) {
                    Toast.makeText(ShopDetailsActivity.this, "Please enter your phone in your profile before", Toast.LENGTH_SHORT).show();
                    return; //don't procede further
                }
                if (cartItemList.size() == 0) {
                    Toast.makeText(ShopDetailsActivity.this, "No items in cart", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                submitOrder();
            }
        });

    }

    private void submitOrder() {
        progressDialog.setMessage("Placing Order...");
        progressDialog.show();

        //for order id and order time
        final String timestamp = ""+System.currentTimeMillis();

        String cost = allTotalPriceTv.getText().toString().trim().replace("Kz","");
        //setup order data
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("orderId", ""+timestamp);
        hashMap.put("orderTime", ""+timestamp);
        hashMap.put("orderCost", ""+cost);
        hashMap.put("orderStatus", "In Progress"); //In progress/completed/canceled
        hashMap.put("orderBy", ""+firebaseAuth.getUid());
        hashMap.put("orderTo", ""+shopUid);

        //add to db
        final DatabaseReference ref =  FirebaseDatabase.getInstance().getReference("Users").child(shopUid).child("Orders");
        ref.child(timestamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //order info added now add order items
                for (int i = 0; i<cartItemList.size(); i++) {
                    String pId = cartItemList.get(i).getpId();
                    String id = cartItemList.get(i).getId();
                    String cost1 = cartItemList.get(i).getCost();
                    String name = cartItemList.get(i).getName();
                    String price = cartItemList.get(i).getPrice();
                    String quantity = cartItemList.get(i).getQuantity();

                    HashMap<String, String> hashMap1 = new HashMap<>();
                    hashMap1.put("pId",pId);
                    hashMap1.put("name",name);
                    hashMap1.put("cost",cost1);
                    hashMap1.put("price",price);
                    hashMap1.put("quantity",quantity);

                    ref.child(timestamp).child("Items").child(pId).setValue(hashMap1);
                }
                progressDialog.dismiss();
                Toast.makeText(ShopDetailsActivity.this, "Order Placed Successfully", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(ShopDetailsActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openMap() {
        String address = "https://maps.google.com/maps?saddr=" + myLatitude + "," + myLongitude + "&daddr=" + shopLatitude + "," + shopLongitude;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(address));
        startActivity(intent);
    }

    private void dialPhone() {
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(shopPhone))));
        Toast.makeText(this, "" + shopPhone, Toast.LENGTH_SHORT).show();
    }

    private void loadMyInfo() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(firebaseAuth.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    // get user data
                    String name = "" + ds.child("name").getValue();
                    String email = "" + ds.child("email").getValue();
                    myPhone = "" + ds.child("phone").getValue();
                    String profileImage = "" + ds.child("profileImage").getValue();
                    String city = "" + ds.child("city").getValue();
                    myLatitude = "" + ds.child("latitude").getValue();
                    myLongitude = "" + ds.child("longitude").getValue();
//                    String accountType = ""+ds.child("accountType");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void loadShopDetails() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //get shop data
                String name = "" + dataSnapshot.child("name").getValue();
                shopName = "" + dataSnapshot.child("shopName").getValue();
                shopEmail = "" + dataSnapshot.child("email").getValue();
                shopPhone = "" + dataSnapshot.child("phone").getValue();
                shopLatitude = "" + dataSnapshot.child("latitude").getValue();
                shopAddress = "" + dataSnapshot.child("address").getValue();
                shopLongitude = "" + dataSnapshot.child("longitude").getValue();
                deliveryFee = "" + dataSnapshot.child("deliveryFee").getValue();
                String profileImage = "" + dataSnapshot.child("profileImage").getValue();
                String shopOpen = "" + dataSnapshot.child("shopOpen").getValue();

                //set data
                shopNameTv.setText(shopName);
                emailTv.setText(shopEmail);
                deliveryFeeTv.setText("Delivery Fee: " + deliveryFee + " Kz");
                addressTv.setText(shopAddress);
                phoneTv.setText(shopPhone);

                if (shopOpen.equals("true")) {
                    openCloseTv.setText("Open");
                } else {
                    openCloseTv.setText("Closed");
                }

                try {
                    Picasso.get().load(profileImage).into(shopIv);
                } catch (Exception e) {

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void loadShopProducts() {
        //init list
        productsList = new ArrayList<>();

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.child(shopUid).child("Products").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                //clear list before adding items
                productsList.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Product product = ds.getValue(Product.class);
                    productsList.add(product);
                }
                //setup adapter
                adapterProductUser = new AdapterProductUser(ShopDetailsActivity.this, productsList);
                //set adapet
                productRv.setAdapter(adapterProductUser);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


}
