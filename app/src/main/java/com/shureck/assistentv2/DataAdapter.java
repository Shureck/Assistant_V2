package com.shureck.assistentv2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

class DataAdapter extends RecyclerView.Adapter {

    private LayoutInflater inflater;
    private ViewGroup vigp;
    private ImageView imageView;
    private TextView nameView, companyView;

    private List<RowType> dataSet;

    public DataAdapter(List<RowType> dataSet) {
        this.dataSet = dataSet;
    }

    DataAdapter(Context context, List<RowType> dataSet) {
        this.dataSet = dataSet;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemViewType(int position) {
        if (dataSet.get(position) instanceof Messages) {
            return RowType.ASK;
        } else if (dataSet.get(position) instanceof Answers) {
            return RowType.ANSWER;
        } else {
            return -1;
        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        if (viewType == RowType.ASK) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_ask, parent, false);
            return new MessagesHolder(view);
        } else if (viewType == RowType.ANSWER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_answer, parent, false);
            return new AnswersHolder(view);
        }else{
            return null;
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        RowType phone = dataSet.get(position);

        if (holder instanceof MessagesHolder) {
            ((MessagesHolder) holder).nameView
                    .setText(((Messages) dataSet.get(position)).getName());
            ((MessagesHolder) holder).companyView
                    .setText(((Messages) dataSet.get(position)).getCompany());
            ((MessagesHolder) holder).imageView
                    .setImageResource(((Messages) dataSet.get(position)).getImage());
        } else if (holder instanceof AnswersHolder) {
            ((AnswersHolder) holder).nameView
                    .setText(((Answers) dataSet.get(position)).getName());
            ((AnswersHolder) holder).companyView
                    .setText(((Answers) dataSet.get(position)).getCompany());
            ((AnswersHolder) holder).imageView
                    .setImageResource(((Answers) dataSet.get(position)).getImage());
        }

    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView nameView, companyView;
        ViewHolder(View view){
            super(view);
            imageView = (ImageView)view.findViewById(R.id.image);
            nameView = (TextView) view.findViewById(R.id.name);
            companyView = (TextView) view.findViewById(R.id.company);
        }
    }

    public class MessagesHolder extends RecyclerView.ViewHolder {

        final ImageView imageView;
        final TextView nameView, companyView;

        public MessagesHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView)itemView.findViewById(R.id.image);
            nameView = (TextView) itemView.findViewById(R.id.name);
            companyView = (TextView) itemView.findViewById(R.id.company);
        }
    }

    public static class AnswersHolder extends RecyclerView.ViewHolder {

        final ImageView imageView;
        final TextView nameView, companyView;

        public AnswersHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView)itemView.findViewById(R.id.image);
            nameView = (TextView) itemView.findViewById(R.id.name);
            companyView = (TextView) itemView.findViewById(R.id.company);
        }
    }
}