package com.surkaa.wordsfortjnu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.surkaa.wordsfortjnu.word.Word;
import com.surkaa.wordsfortjnu.word.WordRepository;

import java.util.List;

public class WordAdapter extends ListAdapter<Word, WordAdapter.WordHolder> {

    private final WordRepository wordRepository;
    private List<Word> list;
    final static String HTTPS = "https://m.youdao.com/dict?le=eng&q=";

    protected WordAdapter(WordRepository wordRepository) {
        super(new DiffUtil.ItemCallback<Word>() {
            @Override
            public boolean areItemsTheSame(@NonNull Word oldItem, @NonNull Word newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Word oldItem, @NonNull Word newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.wordRepository = wordRepository;
    }

    public void setList(List<Word> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public WordHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_word, parent, false);
        final WordHolder holder = new WordHolder(view);
        holder.constraintLayout.setOnClickListener(v -> {
            if (!holder.aSwitch.isChecked()) {
                Uri uri = Uri.parse(HTTPS + holder.english.getText());
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                holder.itemView.getContext().startActivity(intent);
            }
        });
        holder.aSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Word word = list.get(holder.getAdapterPosition());
            Log.d("switch", "isChecked = " + word.getId() + ':' + isChecked);
            word.setClose(isChecked);
            wordRepository.update(word);
            holder.imageView.setSelected(isChecked);
            holder.chip.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
            holder.meaning.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            holder.count.setVisibility(isChecked ? View.INVISIBLE : View.VISIBLE);
        });
        holder.chip.setOnClickListener(v -> {
            Word word = list.get(holder.getAdapterPosition());
            word.addCount();
            notifyItemChanged(holder.getAdapterPosition());
            holder.aSwitch.setChecked(true);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull WordHolder holder, int position) {
        Word word = list.get(position);
        holder.english.setText(word.getEnglish());
        holder.meaning.setText(word.getMeaning());
        holder.cardView.setBackground(getDrawable(holder.itemView.getContext(), word.getCount()));
        holder.count.setText(word.getCount() > 13 ? "A" : String.valueOf(word.getCount()));
        holder.num.setText(String.valueOf(word.getNumInExamination()));
        holder.aSwitch.setChecked(word.isClose());
    }

    // 根据用户记忆次数返回背景颜色
    @SuppressLint("UseCompatLoadingForDrawables")
    public Drawable getDrawable(Context context, int count) {
        int[] range = {1, 4, 8, 13};
        if (count <= range[0]) {
            return context.getDrawable(R.drawable.shape_white);
        } else if (count <= range[1]) {
            return context.getDrawable(R.drawable.shape_blue);
        } else if (count <= range[2]) {
            return context.getDrawable(R.drawable.shape_green);
        } else if (count <= range[3]) {
            return context.getDrawable(R.drawable.shape_yellow);
        } else {
            return context.getDrawable(R.drawable.shape_purple);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull WordHolder holder) {
        super.onViewAttachedToWindow(holder);
        // 手动刷新卡片序号(序号是界面上的，不是数据库中的)
        holder.id.setText(String.valueOf(holder.getAdapterPosition() + 1));
    }

    static class WordHolder extends RecyclerView.ViewHolder {
        Chip chip;
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        Switch aSwitch;
        CardView cardView;
        ImageView imageView;
        ConstraintLayout constraintLayout;
        TextView id, english, meaning, num, count;

        public WordHolder(@NonNull View itemView) {
            super(itemView);
            // 获取item的控件
            chip = itemView.findViewById(R.id.chip);
            aSwitch = itemView.findViewById(R.id.switch1);
            cardView = itemView.findViewById(R.id.card_view);
            imageView = itemView.findViewById(R.id.image_view_come);
            constraintLayout = itemView.findViewById(R.id.constraint_layout_for_web);
            id = itemView.findViewById(R.id.id_no);
            english = itemView.findViewById(R.id.english);
            meaning = itemView.findViewById(R.id.meaning);
            num = itemView.findViewById(R.id.num);
            count = itemView.findViewById(R.id.count);
        }
    }
}