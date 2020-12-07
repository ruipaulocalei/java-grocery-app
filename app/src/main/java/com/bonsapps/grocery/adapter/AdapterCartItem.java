package com.bonsapps.grocery.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bonsapps.grocery.R;
import com.bonsapps.grocery.activities.ShopDetailsActivity;
import com.bonsapps.grocery.models.CartItem;

import java.util.ArrayList;

import p32929.androideasysql_library.Column;
import p32929.androideasysql_library.EasyDB;

public class AdapterCartItem extends RecyclerView.Adapter<AdapterCartItem.HolderCartItem> {
    private Context context;
    public ArrayList<CartItem> cartItemList;

    public AdapterCartItem(Context context, ArrayList<CartItem> cartItemList) {
        this.context = context;
        this.cartItemList = cartItemList;
    }

    @NonNull
    @Override
    public HolderCartItem onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_cartitem, parent, false);
        return new HolderCartItem(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderCartItem holder, final int position) {
        //get data
        CartItem cartItem = cartItemList.get(position);
        final String id = cartItem.getId();
        String getpId = cartItem.getpId();
        String title = cartItem.getName();
        final String cost = cartItem.getCost();
        String price = cartItem.getPrice();
        String quantity = cartItem.getQuantity();
        //set data
        holder.itemTitleTv.setText(""+title);
        holder.itemPriceTv.setText(""+price);
        holder.itemQuantityTv.setText("["+quantity+"]"); //e.g. [3]
        holder.itemPriceEach.setText(""+price);
        //handle remove click listener delete item from cart
        holder.itemRemoveTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EasyDB easyDB = EasyDB.init(context, "ITEMS_BD")
                        .setTableName("ITEMS_TABLE")
                        .addColumn(new Column("Item_Id", new String[]{"text","unique"}))
                        .addColumn(new Column("Item_PID", new String[]{"text","unique"}))
                        .addColumn(new Column("Item_Name", new String[]{"text","unique"}))
                        .addColumn(new Column("Item_Price_Each", new String[]{"text","unique"}))
                        .addColumn(new Column("Item_Price", new String[]{"text","unique"}))
                        .addColumn(new Column("Item_Quantity", new String[]{"text","unique"}))
                        .doneTableColumn();

                easyDB.deleteRow(1, id);
                Toast.makeText(context, "Removed from cart!...", Toast.LENGTH_SHORT).show();

                //refresh list
                cartItemList.remove(position);
                notifyItemChanged(position);
                notifyDataSetChanged();
                double tx = Double.parseDouble((((ShopDetailsActivity)context).allTotalPriceTv.getText().toString().trim().replace("Kz","")));
                double totalPrice = tx - Double.parseDouble(cost.replace("Kz",""));
                double deliveryFee = Double.parseDouble((((ShopDetailsActivity)context).deliveryFee.replace("Kz","")));
                double sTotalPrice = Double.parseDouble(String.format("%.2f", totalPrice)) - Double.parseDouble(String.format(String.format("%.2f", deliveryFee)));
                ((ShopDetailsActivity)context).allTotalPrice=0.00;
                ((ShopDetailsActivity)context).sTotalTv.setText(String.format("%.2f", sTotalPrice)+"Kz");
                ((ShopDetailsActivity)context).allTotalPriceTv.setText(String.format("%.2f", Double.parseDouble(String.format("%.2f", totalPrice)))+"Kz");
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItemList.size();
    }

    class HolderCartItem extends RecyclerView.ViewHolder {
        private TextView itemTitleTv, itemPriceTv, itemPriceEach, itemQuantityTv, itemRemoveTv;
        public HolderCartItem(@NonNull View itemView) {
            super(itemView);
            itemTitleTv = itemView.findViewById(R.id.itemTitleTv);
            itemPriceTv = itemView.findViewById(R.id.itemPriceTv);
            itemPriceEach = itemView.findViewById(R.id.itemPriceEach);
            itemQuantityTv = itemView.findViewById(R.id.itemQuantityTv);
            itemRemoveTv = itemView.findViewById(R.id.itemRemoveTv);
        }
    }
}
