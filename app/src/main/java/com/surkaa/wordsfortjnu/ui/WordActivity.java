package com.surkaa.wordsfortjnu.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.surkaa.wordsfortjnu.R;
import com.surkaa.wordsfortjnu.word.Word;
import com.surkaa.wordsfortjnu.word.WordAdapter;
import com.surkaa.wordsfortjnu.word.WordAdapter.WordHolder;
import com.surkaa.wordsfortjnu.word.WordRepository;
import com.surkaa.wordsfortjnu.word.myDefaultWords;

import java.util.List;
import java.util.Objects;

public class WordActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    WordRepository repository;
    WordAdapter adapter;
    LiveData<List<Word>> filteredList;
    Observer<List<Word>> observer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_words);

        // 找到控件 设置监听器
        initView();
        // 仓库及适配器
        repository = new WordRepository(getApplication());
        adapter = new WordAdapter(repository, this);
        // RecyclerView的初始化
        initRecyclerView();
        // 数据filteredList的初始化
        initLiveDataList();
        // 默认数据的导入: 仅在安装后第一次运行时导入
        addDefaultWordsOnFirst();
    }

    private void addDefaultWordsOnFirst() {
        SharedPreferences shp = getSharedPreferences(getString(R.string.shp), Context.MODE_PRIVATE);
        if (shp.getBoolean(getString(R.string.shp_editor_isFirstRun), true)) {

            new myDefaultWords(this, repository).userFirstRun();

            SharedPreferences.Editor editor = shp.edit();
            editor.putBoolean(getString(R.string.shp_editor_isFirstRun), false);
            editor.apply();
        }
    }

    //<editor-fold desc="找到界面控件并设置监听事件">
    private void initView() {
        initClearBtn();
        initAddBtn();
        initTopBtn();
        initBottomBtn();
        initHelpBtn();
    }

    private void initHelpBtn() {
        ImageButton helpBtn = findViewById(R.id.img_btn_help);
        helpBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void initClearBtn() {
        ImageButton clearBtn = findViewById(R.id.img_btn_clear);
        clearBtn.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(WordActivity.this);
            builder.setTitle("确认清除所有单词吗？");
            builder.setPositiveButton("确认", (dialog, which) -> repository.clear());
            builder.setNegativeButton("取消", (dialog, which) -> {
            });
            builder.create().show();
        });
    }

    private void initAddBtn() {
        FloatingActionButton addBtn;
        addBtn = findViewById(R.id.add_word);
        addBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWordActivity.class);
            startActivity(intent);
        });
    }

    private void initTopBtn() {
        ImageButton topBtn = findViewById(R.id.btn_top);
        topBtn.setOnClickListener(v ->
                // 重新设置适配器可以使RecyclerView重启达到返回顶部的效果
                recyclerView.setAdapter(adapter)
        );
    }

    private void initBottomBtn() {
        ImageButton bottomBtn = findViewById(R.id.btn_bottom);
        bottomBtn.setOnClickListener(v ->
                recyclerView.smoothScrollToPosition(Math.max(adapter.getItemCount() - 1, 0))
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 在顶部设置出仅有一个搜索item的菜单
        getMenuInflater().inflate(R.menu.menu_search, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();

        // 设置搜索框的长度避免Word的label被压缩
        int width = getResources().getDisplayMetrics().widthPixels;
        searchView.setMaxWidth((int) (width * 0.75));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                // 去掉前后空格
                String pattern = s.trim();
                // 改变过程中移除观察者 以防错乱
                filteredList.removeObservers(WordActivity.this);
                filteredList = repository.getWordsWithPattern(pattern);
                // 重新设置观察者
                filteredList.observe(WordActivity.this, observer);
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }
    //</editor-fold>

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 用于序列号的修改, 移动, 删除等操作时用到,并配合适配器的onViewAttachedToWindow()方法
        recyclerView.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public void onAnimationFinished(@NonNull RecyclerView.ViewHolder viewHolder) {
                super.onAnimationFinished(viewHolder);
                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager != null) {
                    int first = manager.findFirstVisibleItemPosition();
                    int last = manager.findLastVisibleItemPosition();
                    for (int i = first; i <= last; i++) {
                        WordHolder holder = (WordHolder) recyclerView.findViewHolderForAdapterPosition(i);
                        if (holder != null) {
                            holder.id.setText(String.valueOf(i + 1));
                        }
                    }
                }
            }
        });

        // 滑到最底下留出一个空白区域
        RecyclerView.ItemDecoration itemDecoration = new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                if (parent.getAdapter() != null) {
                    if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
                        outRect.bottom = 250;
                    }
                }
            }
        };

        // 优化recyclerView的功能
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                Word wordFrom = Objects.requireNonNull(filteredList.getValue()).get(viewHolder.getAdapterPosition());
                Word wordTo = filteredList.getValue().get(target.getAdapterPosition());
                int idTemp = wordFrom.getId();
                wordFrom.setId(wordTo.getId());
                wordTo.setId(idTemp);
                repository.update(wordFrom, wordTo);
                adapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                return false;
            }

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                                        @NonNull RecyclerView.ViewHolder viewHolder) {
                // 第一个卡片禁止拖动, 以防bug
                if (viewHolder.getAdapterPosition() == 0) {
                    return makeMovementFlags(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
                }
                return super.getMovementFlags(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                 int direction) {
                // 设置item的滑动删除
                final Word wordToDelete = Objects.requireNonNull(filteredList.getValue()).get(viewHolder.getAdapterPosition());
                repository.delete(wordToDelete);
                Snackbar.make(findViewById(R.id.word_layout), "删除了一个词汇", Snackbar.LENGTH_SHORT)
                        .setAction("撤销", v -> repository.insert(wordToDelete))
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                // 设置item的拖动删除时的图标
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                Drawable icon = ContextCompat.getDrawable(WordActivity.this, R.drawable.ic_clear);
                if (icon == null) {
                    return;
                }
                View itemView = viewHolder.itemView;
                int iconLeft, iconRight, iconTop, iconBottom;
                int icHeight = icon.getIntrinsicHeight();
                iconTop = itemView.getTop() + (itemView.getHeight() - icHeight) / 2;
                iconBottom = iconTop + icon.getIntrinsicHeight();
                if (dX > 0) {
                    iconLeft = itemView.getLeft() + 50;
                    iconRight = iconLeft + icHeight;
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else if (dX < 0) {
                    iconRight = itemView.getRight() - 50;
                    iconLeft = iconRight - icHeight;
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else {
                    icon.setBounds(0, 0, 0, 0);
                }
                icon.setTint(Color.RED);
                icon.draw(c);
                icon.setTint(Color.WHITE);
            }
        });


        recyclerView.addItemDecoration(itemDecoration);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void initLiveDataList() {
        observer = words -> {
            int len = words.size();
            LottieAnimationView emptyView;
            emptyView = findViewById(R.id.empty_state_animation);
            if (len != 0) {
                // 暂停并隐藏
                emptyView.pauseAnimation();
                emptyView.setVisibility(View.INVISIBLE);
            } else {
                // 只播放一遍
                emptyView.setVisibility(View.VISIBLE);
                emptyView.setRepeatCount(1);
            }

            adapter.setList(words);
            if (len != adapter.getItemCount()) {
                adapter.submitList(words);
            }
            TextView tvCountWords = findViewById(R.id.tv_count_words);
            tvCountWords.setText(String.valueOf(len));
        };
        filteredList = repository.getAll();
        filteredList.observe(this, observer);
    }
}