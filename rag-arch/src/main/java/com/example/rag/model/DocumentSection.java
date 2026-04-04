package com.example.rag.model;

import java.util.List;

public class DocumentSection {

    private String headingText;
    private String fullHeadingPath;
    private int level;
    private int startPage;
    private int endPage;

    public String getHeadingText() {
        return headingText;
    }

    public void setHeadingText(String headingText) {
        this.headingText = headingText;
    }

    public String getFullHeadingPath() {
        return fullHeadingPath;
    }

    public void setFullHeadingPath(String fullHeadingPath) {
        this.fullHeadingPath = fullHeadingPath;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getStartPage() {
        return startPage;
    }

    public void setStartPage(int startPage) {
        this.startPage = startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public void setEndPage(int endPage) {
        this.endPage = endPage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<PageText> getPageTexts() {
        return pageTexts;
    }

    public void setPageTexts(List<PageText> pageTexts) {
        this.pageTexts = pageTexts;
    }

    private String text;
    private List<PageText> pageTexts;

    // getters

}
