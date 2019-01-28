package com.meowlomo.jenkins.model;

import org.apache.http.entity.ContentType;

import hudson.util.ListBoxModel;

public enum MimeType {

    NOT_SET(null),
    TEXT_HTML(ContentType.TEXT_HTML),
    TEXT_PLAIN(ContentType.TEXT_PLAIN),
    APPLICATION_FORM(ContentType.APPLICATION_FORM_URLENCODED),
    APPLICATION_JSON(ContentType.create("application/json")),
    APPLICATION_JSON_UTF8(ContentType.APPLICATION_JSON);

    private final ContentType contentType;

    MimeType(ContentType contentType) {
        this.contentType = contentType;
    }

    public String getValue() {
        return contentType.getMimeType();
    }

    public ContentType getContentType() {
        return contentType;
    }

    public static ListBoxModel getContentTypeFillItems() {
        ListBoxModel items = new ListBoxModel();
        for (MimeType mimeType : values()) {
            items.add(mimeType.name());
        }
        return items;
    }
}

