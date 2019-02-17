package com.meowlomo.jenkins.scm_httpclient.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hudson.util.FormValidation;

public class HttpRequestValidation {

    public static FormValidation checkUrl(String value) {
        try {
            new URL(value);
            return FormValidation.ok();
        } catch (MalformedURLException ex) {
            return FormValidation.error("Invalid url");
        }
    }
}
