package com.meowlomo.jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.meowlomo.jenkins.model.HttpMode;
import com.meowlomo.jenkins.model.MimeType;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;

/**
 * @author rich chen
 */
public class SCMHttpClientPublisher extends Recorder implements SimpleBuildStep {

	private static Logger LOG = Logger.getLogger(SCMHttpClientPublisher.class.getName());
	private boolean save_build_detail; // the job's build message
	private boolean save_affect_path; // scm affect paths
	private String url;
	private HttpMode httpMode;
	private MimeType contentType;
	private String requestBody;

	@DataBoundConstructor
	public SCMHttpClientPublisher(String url) {
		this.url = url;
	}

	public boolean isSaveBuildDtail() {
		return save_build_detail;
	}

	@DataBoundSetter
	public void setSaveBuildDetail(Boolean save_build_detail) {
		this.save_build_detail = save_build_detail;
	}

	public boolean isSaveAffectPath() {
		return save_affect_path;
	}

	@DataBoundSetter
	public void setSaveAffectPath(Boolean save_affect_path) {
		this.save_affect_path = save_affect_path;
	}

	public String getUrl() {
		return url;
	}

	public HttpMode getHttpMode() {
		return httpMode;
	}

	@DataBoundSetter
	public void setHttpMode(HttpMode httpMode) {
		this.httpMode = httpMode;
	}

	public MimeType getContentType() {
		return contentType;
	}

	@DataBoundSetter
	public void setContentType(MimeType contentType) {
		this.contentType = contentType;
	}

	public String getRequestBody() {
		return requestBody;
	}

	@DataBoundSetter
	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public void perform(Run<?, ?> run, FilePath filePath, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		listener.getLogger().println("trigger scm httpclient services...");
		List<String> affectPaths = getAffectedPaths(run);
		for(String path : affectPaths){
			listener.getLogger().println(path);
		}
	}

	/**
	 * Retrieve affectedPaths from the SCM plugin
	 *
	 * @param run
	 *
	 */
	public List<String> getAffectedPaths(Run<?, ?> run) {
		List<String> affectedPaths = new ArrayList<String>();
		ChangeLogSet<? extends Entry> cls = ((AbstractBuild<?, ?>) run).getChangeSet();
		if (!cls.isEmptySet()) {
			for (ChangeLogSet.Entry e : cls) {
				Collection<String> paths = e.getAffectedPaths();
				Iterator<String> it = paths.iterator();
				while (it.hasNext()) {
					String path = it.next();
					affectedPaths.add(path);
				}
			}
			return affectedPaths;
		} else {
			return null;
		}
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
			return true;
		}

		/**
		 * This human readable name is used in the configuration screen.
		 */
		@Override
		public String getDisplayName() {
			return "SCM HttpClient";
		}

		public ListBoxModel doFillHttpModeItems() {
			return HttpMode.getFillItems();
		}

		public ListBoxModel doFillContentTypeItems() {
			return MimeType.getContentTypeFillItems();
		}

		public FormValidation doCheckSpecificBuild(@AncestorInPath AbstractProject project) {
			return FormValidation.ok();
		}
	}
}
