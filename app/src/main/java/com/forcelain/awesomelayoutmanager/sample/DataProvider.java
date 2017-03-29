package com.forcelain.awesomelayoutmanager.sample;

import java.util.List;

public interface DataProvider {
    List<Article> getArticles(int limit);
}
