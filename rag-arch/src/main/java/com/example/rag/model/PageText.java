package com.example.rag.model;

public class PageText {

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    private int pageNumber;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    private String text;

    // getters
    public PageText() {}
    public PageText(int pageNumber, String text) {
        this.pageNumber = pageNumber;
        this.text = text;
    }


}
