package com.forcelain.awesomelayoutmanager.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.forcelain.awesomelayoutmanager.AwesomeLayoutManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DataProvider dataProvider;
    private AwesomeLayoutManager layoutManager;
    private ArticleAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        layoutManager = new AwesomeLayoutManager();
        layoutManager.setScaleFactor(0.5f);
        layoutManager.setPagination(true);
        layoutManager.setPageHeightFactor(.7f);
        layoutManager.setTransitionDuration(450);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ArticleAdapter();
        recyclerView.setAdapter(adapter);
        dataProvider = new FakeDataProvider(this);
        List<Article> articles = dataProvider.getArticles(0);
        adapter.setArticles(articles);
        adapter.setItemClickListener(new ArticleAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(int pos) {
                layoutManager.openItem(pos);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_one:
                adapter.setArticles(dataProvider.getArticles(1));
                return true;
            case R.id.menu_item_two:
                adapter.setArticles(dataProvider.getArticles(2));
                return true;
            case R.id.menu_item_many:
                adapter.setArticles(dataProvider.getArticles(0));
                return true;
            case R.id.menu_goto_first:
                recyclerView.scrollToPosition(0);
                return true;
            case R.id.menu_goto_last:
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        dataProvider = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (layoutManager.getOrientation() == AwesomeLayoutManager.Orientation.HORIZONTAL) {
            layoutManager.close();
        } else {
            super.onBackPressed();
        }
    }
}
