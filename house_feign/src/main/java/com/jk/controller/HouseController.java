package com.jk.controller;

import com.alibaba.fastjson.JSON;
import com.jk.pojo.Shop;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Controller
public class HouseController {

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @RequestMapping("queryOrder")
    @ResponseBody
    public List<Shop> queryAllOrder(String text){
        System.out.println("text = [" + text + "]");
        //拿到elastic客户端
        Client client = elasticsearchTemplate.getClient();
        //参数为索引名称
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch("house")
                .setTypes("elhouse")
                //设置查询条件 boolQuery() 多条件查询
                .setQuery(QueryBuilders.matchQuery("name", text));
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("name");
        highlightBuilder.preTags("<font color='red'>");
        highlightBuilder.postTags("</font>");
        //设置高亮查询
        searchRequestBuilder.highlighter(highlightBuilder);
        //执行查询 拿到返回值
        SearchResponse searchResponse = searchRequestBuilder.get();

        //拿到命中条数
        SearchHits hits = searchResponse.getHits();
        //获取总条数 用来分页
        //hits.getTotalHits();
        //获取到结果集迭代器
        Iterator<SearchHit> iterator = hits.iterator();
        List<Shop> orderList = new ArrayList<Shop>();
        while (iterator.hasNext()){
            SearchHit next = iterator.next();
            //获取到源码内容 以json字符串的形式获取
            String sourceAsString = next.getSourceAsString();
            //获取高亮字段
            Map<String, HighlightField> highlightFields = next.getHighlightFields();
            HighlightField orderInfo = highlightFields.get("name");
            Shop order = JSON.parseObject(sourceAsString, Shop.class);

            //取得定义的高亮标签
            Text[] fragments = orderInfo.fragments();
            //为thinkName（相应字段）增加自定义的高亮标签
            String title = "";
            for(Text text1 : fragments){
                title += text1;
            }

            //使用高亮内容 替换非高亮内容
            order.setName(title);
            orderList.add(order);
        }
        return orderList;
    }




}
