package com.bonsapps.grocery.utils;

import android.widget.Filter;

import com.bonsapps.grocery.adapter.AdapterProductSeller;
import com.bonsapps.grocery.adapter.AdapterProductUser;
import com.bonsapps.grocery.models.Product;

import java.util.ArrayList;

public class FilterProductUser extends Filter {

    private AdapterProductUser adapter;
    private ArrayList<Product> filterList;

    public FilterProductUser(AdapterProductUser adapter, ArrayList<Product> filterList) {
        this.adapter = adapter;
        this.filterList = filterList;
    }

    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        FilterResults results = new FilterResults();
        //Validate data for seach query
        if (constraint != null && constraint.length() > 0) {
            // change to upper case to make case insensitive
            constraint = constraint.toString().toUpperCase();

            //store our filtered list
            ArrayList<Product> filteredModel = new ArrayList<>();
            for (int i = 0; i < filterList.size(); i++) {
                //check search by title/category
                if (filterList.get(i).getProductTitle().toUpperCase().contains(constraint)
                || filterList.get(i).getProductCategory().toUpperCase().contains(constraint)) {
                    // add filtered data to list
                    filteredModel.add(filterList.get(i));
                }
            }
            results.count = filteredModel.size();
            results.values = filteredModel;
        } else {
            // return all list
            results.count = filterList.size();
            results.values = filterList;
        }
        return results;
    }

    @Override
    protected void publishResults(CharSequence constraint, FilterResults results) {
        adapter.productsList = (ArrayList<Product>) results.values;
        // refresh adapter
        adapter.notifyDataSetChanged();
    }
}
