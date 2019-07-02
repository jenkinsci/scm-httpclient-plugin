package com.github.jenkins.scm_httpclient;

import org.jvnet.hudson.test.JenkinsRule;
import org.apache.commons.io.FileUtils;
import hudson.model.*;
import hudson.tasks.Shell;
import org.junit.Test;
import org.junit.Rule;

public class ScmHttpClientTest {
  @Rule public JenkinsRule j = new JenkinsRule();
  
  @Test public void first() throws Exception {
    FreeStyleProject project = j.createFreeStyleProject();
    project.getBuildersList().add(new Shell("echo hello"));
    
    FreeStyleBuild build = project.scheduleBuild2(0).get();
    
    System.out.println(build.getDisplayName() + " completed");
    // TODO: change this to use HtmlUnit
    String s = FileUtils.readFileToString(build.getLogFile());
//    System.out.println(s+" <== s");
//    assertThat(s, contains("+ echo hello"));
  }
}
//public class ScmHttpClientTest {
//	@Rule
//	public JenkinsRule j = new JenkinsRule();
//	
//	@Test
//	public void simpleGetTest() throws Exception {
//		// Run build
//		FreeStyleProject project = this.j.createFreeStyleProject();
////		project.getBuildersList().add(httpRequest);
//		FreeStyleBuild build = project.scheduleBuild2(0).get();
//
//	}
//
//}
