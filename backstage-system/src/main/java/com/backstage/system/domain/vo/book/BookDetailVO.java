package com.backstage.system.domain.vo.book;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

/**
 * 电子书详情视图对象
 *
 * @author backstage
 */
public class BookDetailVO implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 电子书ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 封面 */
    private String cover;

    /** 描述 */
    @JsonProperty("desc")
    private String desc;

    /** 试读内容 */
    @JsonProperty("try")
    private String tryContent;

    /** 价格 */
    private String price;

    /** 原价 */
    @JsonProperty("t_price")
    private String tPrice;

    /** 订阅数 */
    @JsonProperty("sub_count")
    private Integer subCount;

    /** 权限等级 */
    private Integer level;

    /** Book media type: 1 ebook, 2 comic. */
    private Integer mediaType;

    /** 状态：0正常，1下架 */
    private String status;

    /** 逻辑删除：0正常，1删除 */
    private Integer deleteFlag;

    /** 作者用户ID */
    private Long authorUserId;

    /** 作者用户名 */
    private String authorName;

    /** 作者头像 */
    private String authorAvatar;

    /** 作者个人简介 */
    private String authorIntroduction;

    /** 作者最高角色名称 */
    private String authorRoleName;

    /** 作者最高角色等级 */
    private Integer authorRoleLevel;

    /** 作者 GitHub 账号，用于用户名片兼容匹配 */
    private String authorGithubAccount;

    /** 章节列表 */
    @JsonProperty("book_details")
    private List<BookChapterVO> bookDetails;

    /**
     * 标签
     */
    private List<String> tags;

    /** 是否已购买 */
    private Boolean isbuy;

    /** 是否已收藏 */
    private Boolean isfava;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getCover()
    {
        return cover;
    }

    public void setCover(String cover)
    {
        this.cover = cover;
    }

    public String getDesc()
    {
        return desc;
    }

    public void setDesc(String desc)
    {
        this.desc = desc;
    }

    public String getTryContent()
    {
        return tryContent;
    }

    public void setTryContent(String tryContent)
    {
        this.tryContent = tryContent;
    }

    public String getPrice()
    {
        return price;
    }

    public void setPrice(String price)
    {
        this.price = price;
    }

    public String getTPrice()
    {
        return tPrice;
    }

    public void setTPrice(String tPrice)
    {
        this.tPrice = tPrice;
    }

    public Integer getSubCount()
    {
        return subCount;
    }

    public void setSubCount(Integer subCount)
    {
        this.subCount = subCount;
    }

    public Integer getLevel()
    {
        return level;
    }

    public void setLevel(Integer level)
    {
        this.level = level;
    }

    public Integer getMediaType() {
        return mediaType;
    }

    public void setMediaType(Integer mediaType) {
        this.mediaType = mediaType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDeleteFlag() {
        return deleteFlag;
    }

    public void setDeleteFlag(Integer deleteFlag) {
        this.deleteFlag = deleteFlag;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public void setAuthorUserId(Long authorUserId) {
        this.authorUserId = authorUserId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatar() {
        return authorAvatar;
    }

    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }

    public String getAuthorIntroduction() {
        return authorIntroduction;
    }

    public void setAuthorIntroduction(String authorIntroduction) {
        this.authorIntroduction = authorIntroduction;
    }

    public String getAuthorRoleName() {
        return authorRoleName;
    }

    public void setAuthorRoleName(String authorRoleName) {
        this.authorRoleName = authorRoleName;
    }

    public Integer getAuthorRoleLevel() {
        return authorRoleLevel;
    }

    public void setAuthorRoleLevel(Integer authorRoleLevel) {
        this.authorRoleLevel = authorRoleLevel;
    }

    public String getAuthorGithubAccount() {
        return authorGithubAccount;
    }

    public void setAuthorGithubAccount(String authorGithubAccount) {
        this.authorGithubAccount = authorGithubAccount;
    }

    public List<BookChapterVO> getBookDetails()
    {
        return bookDetails;
    }

    public void setBookDetails(List<BookChapterVO> bookDetails)
    {
        this.bookDetails = bookDetails;
    }

    public Boolean getIsbuy()
    {
        return isbuy;
    }

    public void setIsbuy(Boolean isbuy)
    {
        this.isbuy = isbuy;
    }

    public Boolean getIsfava() {
        return isfava;
    }

    public void setIsfava(Boolean isfava) {
        this.isfava = isfava;
    }

    public String gettPrice() {
        return tPrice;
    }

    public void settPrice(String tPrice) {
        this.tPrice = tPrice;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

}
