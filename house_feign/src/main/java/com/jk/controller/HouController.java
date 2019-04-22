package com.jk.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jk.pojo.Shop;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.elasticsearch.search.SearchHit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Controller
public class HouController {
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @RequestMapping("tohouse")
    public String tohouse(){
        return  "house";
    }

    //查询
    @RequestMapping("queryProduct")
    @ResponseBody
    public JSONObject queryProduct(Integer page, Integer rows, Shop shop){
        JSONObject result = new JSONObject();
        Client client = elasticsearchTemplate.getClient();
        Integer startIndex = rows*(page-1);

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch("house").setTypes("elhouse");
        if(shop.getName() !=null && shop.getName() != "" ){
            searchRequestBuilder.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.matchQuery("name", shop.getName())));
        }
        searchRequestBuilder.setFrom(startIndex).setSize(rows);
        // 设置是否按查询匹配度排序
        searchRequestBuilder.setExplain(true);

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("info");
        highlightBuilder.preTags("<font color='red' >");
        highlightBuilder.postTags("</font>");
        searchRequestBuilder.highlighter(highlightBuilder);

        SearchResponse searchResponse = searchRequestBuilder.get();

        SearchHits hits = searchResponse.getHits();
        long total = hits.getTotalHits();
        System.out.println("total = [" + total + "]");

        Iterator<SearchHit> iterator = hits.iterator();

        List<Shop> list = new ArrayList<Shop>();

        while (iterator.hasNext()){
            SearchHit next = iterator.next();
            Map<String, HighlightField> highlightFields = next.getHighlightFields();

            String sourceAsString = next.getSourceAsString();
            HighlightField info = highlightFields.get("info");
            Shop shopBean = JSON.parseObject(sourceAsString, Shop.class);
            //取得定义的高亮标签
            if(info !=null) {
                Text[] fragments = info.fragments();
                //为thinkName（相应字段）增加自定义的高亮标签
                String title = "";
                for (Text text1 : fragments) {
                    title += text1;
                }
                shopBean.setName(title);
            }
            list.add(shopBean);
        }
        result.put("total",total);
        result.put("rows",list);
        return result;
    }





}
