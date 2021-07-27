package com.gs.spider.model.commons;

import com.google.common.base.MoreObjects;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Webpage
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Webpage {
    /**
     * 附件id列表
     */
    public List<String> attachmentList;
    /**
     * 图片ID列表
     */
    public List<String> imageList;
    /**
     * 正文
     */
    private String content;
    /**
     * 标题
     */
    private String title;
    /**
     * 链接
     */
    private String url;
    /**
     * 域名
     */
    private String domain;
    /**
     * 爬虫id,可以认为是taskid
     */
    private String spiderUUID;
    /**
     * 模板id
     */
    @SerializedName("spiderInfoId")
    private String spiderInfoId;
    /**
     * 分类
     */
    private String category;
    /**
     * 网页快照
     */
    private String rawHTML;
    /**
     * 关键词
     */
    private List<String> keywords;
    /**
     * 摘要
     */
    private List<String> summary;
    /**
     * 抓取时间
     */
    @SerializedName("gatherTime")
    private Date gathertime;
    /**
     * 网页id,es自动分配的
     */
    private String id;
    /**
     * 文章的发布时间
     */
    private Date publishTime;
    /**
     * 命名实体
     */
    private Map<String, Set<String>> namedEntity;
    /**
     * 动态字段
     */
    private Map<String, Object> dynamicFields;
    /**
     * 静态字段
     */
    private Map<String, Object> staticFields;
    /**
     * 本网页处理时长
     */
    private long processTime;

    private String contentCleaned;

    private String algoSummary;

    private String domainName;

    private int level;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("content", content)
            .add("title", title)
            .add("url", url)
            .add("domain", domain)
            .add("spiderUUID", spiderUUID)
            .add("spiderInfoId", spiderInfoId)
            .add("category", category)
            .add("rawHTML", rawHTML)
            .add("keywords", keywords)
            .add("summary", summary)
            .add("gathertime", gathertime)
            .add("id", id)
            .add("publishTime", publishTime)
            .add("namedEntity", namedEntity)
            .add("dynamicFields", dynamicFields)
            .add("staticFields", staticFields)
            .add("attachmentList", attachmentList)
            .add("imageList", imageList)
            .add("contentCleaned", contentCleaned)
            .add("domainName", domainName)
            .add("level", level)
            .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Webpage webpage = (Webpage) o;

        return new EqualsBuilder()
            .append(getContent(), webpage.getContent())
            .append(getTitle(), webpage.getTitle())
            .append(getUrl(), webpage.getUrl())
            .append(getDomain(), webpage.getDomain())
            .append(getSpiderUUID(), webpage.getSpiderUUID())
            .append(getSpiderInfoId(), webpage.getSpiderInfoId())
            .append(getCategory(), webpage.getCategory())
            .append(getRawHTML(), webpage.getRawHTML())
            .append(getKeywords(), webpage.getKeywords())
            .append(getSummary(), webpage.getSummary())
            .append(getGathertime(), webpage.getGathertime())
            .append(getId(), webpage.getId())
            .append(getPublishTime(), webpage.getPublishTime())
            .append(getNamedEntity(), webpage.getNamedEntity())
            .append(getDynamicFields(), webpage.getDynamicFields())
            .append(getStaticFields(), webpage.getStaticFields())
            .append(getAttachmentList(), webpage.getAttachmentList())
            .append(getImageList(), webpage.getImageList())
            .append(getContentCleaned(), webpage.getContentCleaned())
            .append(getAlgoSummary(), webpage.getAlgoSummary())
            .append(getDomainName(), webpage.getDomainName())
            .append(getLevel(), webpage.getLevel())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(getContent())
            .append(getTitle())
            .append(getUrl())
            .append(getDomain())
            .append(getSpiderUUID())
            .append(getSpiderInfoId())
            .append(getCategory())
            .append(getRawHTML())
            .append(getKeywords())
            .append(getSummary())
            .append(getGathertime())
            .append(getId())
            .append(getPublishTime())
            .append(getNamedEntity())
            .append(getDynamicFields())
            .append(getStaticFields())
            .append(getAttachmentList())
            .append(getImageList())
            .append(getContentCleaned())
            .append(getAlgoSummary())
            .append(getDomainName())
            .append(getLevel())
            .toHashCode();
    }
}
