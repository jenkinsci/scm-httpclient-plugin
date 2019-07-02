package com.meowlomo.jenkins.scm_httpclient;

import java.util.ArrayList;
import java.util.List;

import com.meowlomo.jenkins.scm_httpclient.auth.Authenticator;
import com.meowlomo.jenkins.scm_httpclient.auth.BasicDigestAuthentication;
import com.meowlomo.jenkins.scm_httpclient.util.HttpRequestNameValuePair;

import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.util.XStream2;

public class HttpRequestGlobalConf {

    private List<BasicDigestAuthentication> basicDigestAuthentications = new ArrayList<BasicDigestAuthentication>();

    private static final XStream2 XSTREAM2 = new XStream2();

    public HttpRequestGlobalConf() {
     
    }

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void xStreamCompatibility() {
        XSTREAM2.addCompatibilityAlias("jenkins.plugins.http_request.HttpRequest$DescriptorImpl", HttpRequestGlobalConf.class);
        XSTREAM2.addCompatibilityAlias("jenkins.plugins.http_request.util.NameValuePair", HttpRequestNameValuePair.class);
    }

    public List<BasicDigestAuthentication> getBasicDigestAuthentications() {
        return basicDigestAuthentications;
    }

    public void setBasicDigestAuthentications(
            List<BasicDigestAuthentication> basicDigestAuthentications) {
        this.basicDigestAuthentications = basicDigestAuthentications;
    }

    public List<Authenticator> getAuthentications() {
        List<Authenticator> list = new ArrayList<Authenticator>();
        list.addAll(basicDigestAuthentications);
        return list;
    }

    public Authenticator getAuthentication(String keyName) {
        for (Authenticator authenticator : getAuthentications()) {
            if (authenticator.getKeyName().equals(keyName)) {
                return authenticator;
            }
        }
        return null;
    }
}
