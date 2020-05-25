package com.shureck.assistentv2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.shureck.assistentv2.Answers;
import com.shureck.assistentv2.Messages;
import com.shureck.assistentv2.R;
import com.shureck.assistentv2.RowType;

import java.util.List;

class NfcVAdapter extends RecyclerView.Adapter {

    private LayoutInflater inflater;
    private ViewGroup vigp;
    private TextView nameView, companyView;

    private List<Nfc_struct> dataSet;

    public NfcVAdapter(List<Nfc_struct> dataSet) {
        this.dataSet = dataSet;
    }

    NfcVAdapter(Context context, List<Nfc_struct> dataSet) {
        this.dataSet = dataSet;
        this.inflater = LayoutInflater.from(context);
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.nfc_adapter, parent, false);
        return new com.shureck.assistentv2.NfcVAdapter.ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Nfc_struct phone = dataSet.get(position);

        ((com.shureck.assistentv2.NfcVAdapter.ViewHolder) holder).nameView
                .setText(phone.name+" - "+phone.uid);
        ((com.shureck.assistentv2.NfcVAdapter.ViewHolder) holder).companyView
                .setText(phone.descrip);

    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final TextView nameView, companyView;
        ViewHolder(View view){
            super(view);
            nameView = (TextView) view.findViewById(R.id.text13);
            companyView = (TextView) view.findViewById(R.id.text14);
        }
    }
}