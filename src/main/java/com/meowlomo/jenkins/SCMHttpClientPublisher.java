package com.meowlomo.jenkins;

import java.io.IOException;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;

public class SCMHttpClientPublisher extends Recorder implements SimpleBuildStep{
	private String url;

	@DataBoundConstructor
	public SCMHttpClientPublisher(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	@Override
	public void perform(Run<?, ?> arg0, FilePath arg1, Launcher arg2, TaskListener arg3)
			throws InterruptedException, IOException {

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
        public FormValidation doCheckSpecificBuild(@AncestorInPath AbstractProject project) {
                return FormValidation.ok();
        }
     }
}
