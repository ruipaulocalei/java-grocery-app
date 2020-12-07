package com.bonsapps.grocery.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bonsapps.grocery.R;
import com.bonsapps.grocery.models.Product;
import com.bonsapps.grocery.utils.FilterProductUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import p32929.androideasysql_library.Column;
import p32929.androideasysql_library.EasyDB;

public class AdapterProductUser extends RecyclerView.Adapter<AdapterProductUser.ProductUserHolder> implements Filterable {
    private Context context;
    public ArrayList<Product> productsList, filterList;
    private FilterProductUser filter;

    public AdapterProductUser(Context context, ArrayList<Product> productsList) {
        this.context = context;
        this.productsList = productsList;
        this.filterList = productsList;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new FilterProductUser(this, filterList);
        }
        return filter;
    }

    class ProductUserHolder extends RecyclerView.ViewHolder{
        // ui views
        private ImageView productIconIv;
        private TextView discountedNoteTv, titleTv, descriptionTv, addToCartTv, discountedPriceTv, originalPriceTv;
        public ProductUserHolder(@NonNull View itemView) {
            super(itemView);
            productIconIv = itemView.findViewById(R.id.productIconIv);
            discountedNoteTv = itemView.findViewById(R.id.discountedNoteTv);
            titleTv = itemView.findViewById(R.id.titleTv);
            descriptionTv = itemView.findViewById(R.id.descriptionTv);
            addToCartTv = itemView.findViewById(R.id.addToCartTv);
            discountedPriceTv = itemView.findViewById(R.id.discountedPriceTv);
            originalPriceTv = itemView.findViewById(R.id.originalPriceTv);
        }
    }
    @NonNull
    @Override
    public ProductUserHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_product_user,parent,false);
        return new ProductUserHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductUserHolder holder, int position) {
        //get data
        final Product product = productsList.get(position);
        String discountAvailable = product.getDiscountAvailable();
        String discountNote = product.getDiscountNote();
        String discountPrice = product.getDiscountPrice();
        String productCategory = product.getProductCategory();
        String originalPrice = product.getOriginalPrice();
        String productDescription = product.getProductDescription();
        String productTitle = product.getProductTitle();
        String productQuantity = product.getProductQuantity();
        String productId = product.getProductId();
        String timestamp = product.getTimestamp();
        String productIcon = product.getProductIcon();
        // set data

        holder.titleTv.setText(productTitle);
        holder.discountedNoteTv.setText(discountNote);
        holder.descriptionTv.setText(productDescription);
        holder.originalPriceTv.setText(originalPrice+" Kz");
        holder.discountedPriceTv.setText(discountPrice+" Kz");

        if (discountAvailable.equals("true")) {
            // product is on discount
            holder.discountedPriceTv.setVisibility(View.VISIBLE);
            holder.discountedNoteTv.setVisibility(View.VISIBLE);
            holder.originalPriceTv.setPaintFlags(holder.originalPriceTv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        } else {
            // product is not on discount
            holder.discountedPriceTv.setVisibility(View.GONE);
            holder.discountedNoteTv.setVisibility(View.GONE);
            holder.originalPriceTv.setPaintFlags(0);
        }

        try {
            Picasso.get().load(productIcon).placeholder(R.drawable.ic_add_shopping_primary).into(holder.productIconIv);
        }catch (Exception e) {
            holder.productIconIv.setImageResource(R.drawable.ic_add_shopping_primary);
        }

        holder.addToCartTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //add product to cart
                showQuantityDialog(product);
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show product details
            }
        });
    }

    private double cost = 0.0, finalCost = 0.0;
    private int quantity = 0;
    private void showQuantityDialog(Product product) {
        //inflate layout for dialog
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_quantity, null);
        //init viewa
        ImageView productIv = view.findViewById(R.id.productIv);
        final TextView titleTv = view.findViewById(R.id.titleTv);
        TextView pQuantityTv = view.findViewById(R.id.pQuantityTv);
        TextView descriptionTv = view.findViewById(R.id.descriptionTv);
        TextView discountedNoteTv = view.findViewById(R.id.discountedNoteTv);
        final TextView originalPriceTv = view.findViewById(R.id.originalPriceTv);
        TextView priceDiscountedTv = view.findViewById(R.id.priceDiscountedTv);
        final TextView finalPriceTv = view.findViewById(R.id.finalPriceTv);
        ImageButton decrementBtn = view.findViewById(R.id.decrementBtn);
        final TextView quantityTv = view.findViewById(R.id.quantityTv);
        ImageButton incrementBtn = view.findViewById(R.id.incrementBtn);
        Button continueBtn = view.findViewById(R.id.continueBtn);

        //get data from model
       final String productId = product.getProductId();
       String title = product.getProductTitle();
       String productQuantity = product.getProductQuantity();
       String description = product.getProductDescription();
       String discountNote = product.getDiscountNote();
       String image = product.getProductIcon();

       final String price;
       if (product.getDiscountAvailable().equals("true")){
           //product have discount
           price = product.getDiscountPrice();
           discountedNoteTv.setVisibility(View.VISIBLE);
           originalPriceTv.setPaintFlags(originalPriceTv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); //add strike through on original price
       } else {
           //product don't have discount
           discountedNoteTv.setVisibility(View.GONE);
           priceDiscountedTv.setVisibility(View.GONE);
           price = product.getOriginalPrice();
       }
       cost = Double.parseDouble(price.replaceAll("Kz",""));
       finalCost = Double.parseDouble(price.replaceAll("Kz",""));
       quantity = 1;

       //dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);

        //set data
        try {
            Picasso.get().load(image).placeholder(R.drawable.ic_cart_grey).into(productIv);
        } catch (Exception e){
            productIv.setImageResource(R.drawable.ic_cart_grey);
        }
        titleTv.setText(""+title);
        pQuantityTv.setText(""+productQuantity);
        descriptionTv.setText(""+description);
        discountedNoteTv.setText(""+discountNote);
        quantityTv.setText(""+quantity);
        originalPriceTv.setText(""+product.getOriginalPrice());
        priceDiscountedTv.setText(""+product.getDiscountPrice());
        finalPriceTv.setText(""+finalCost);

        final AlertDialog dialog = builder.create();
        dialog.show();

        //increment quantity of the product
        incrementBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalCost += cost;
                quantity++;

                finalPriceTv.setText(finalCost+"Kz");
                quantityTv.setText(""+quantity);
            }
        });

        //decrement quantity of product, only quantity is > 1
        decrementBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (quantity > 1) {
                    finalCost -= cost;
                    quantity--;

                    finalPriceTv.setText(finalCost+"Kz");
                    quantityTv.setText(quantity+"");
                }
            }
        });

        continueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String title = titleTv.getText().toString().trim();
                String priceEach = price;
                String totalPrice = finalPriceTv.getText().toString().trim().replace("Kz","");
                String quantity = quantityTv.getText().toString().trim();

                //add to db(SQLite)
                addToCart(productId, title, priceEach, totalPrice, quantity);

                dialog.dismiss();
            }
        });
    }

    private int itemId = 1;
    private void addToCart(String productId, String title, String priceEach, String price, String quantity) {
        itemId++;
        EasyDB easyDB = EasyDB.init(context, "ITEMS_BD")
                .setTableName("ITEMS_TABLE")
                .addColumn(new Column("Item_Id", new String[]{"text","unique"}))
                .addColumn(new Column("Item_PID", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Name", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Price_Each", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Price", new String[]{"text","unique"}))
                .addColumn(new Column("Item_Quantity", new String[]{"text","unique"}))
                .doneTableColumn();

        Boolean b = easyDB.addData("Item_Id",itemId)
                .addData("Item_PID", productId)
                .addData("Item_Name", title)
                .addData("Item_Price_Each", priceEach)
                .addData("Item_Price", price)
                .addData("Item_Quantity", quantity)
                .doneDataAdding();

        Toast.makeText(context, "Product Added!...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return productsList.size();
    }

}
