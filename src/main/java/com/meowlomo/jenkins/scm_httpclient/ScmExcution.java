/*
 * The MIT License
 *
 * Copyright 2016 rmpestano.
 * Modifications copyright (C) 2018 meowlomo.com <dev.support@meowlomo.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.meowlomo.jenkins.scm_httpclient;

import static com.meowlomo.jenkins.ci.impl.GitLastChanges.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationProvider;
import org.tmatesoft.svn.core.wc.SVNRevision;

import com.alibaba.fastjson.JSON;
import com.meowlomo.jenkins.ci.constant.JenkinsMessage;
import com.meowlomo.jenkins.ci.impl.GitLastChanges;
import com.meowlomo.jenkins.ci.impl.SvnLastChanges;
import com.meowlomo.jenkins.ci.model.CommitChanges;
import com.meowlomo.jenkins.ci.model.CommitInfo;
import com.meowlomo.jenkins.ci.model.JenkinsEnvs;
import com.meowlomo.jenkins.ci.model.LastChanges;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.scm.SubversionSCM;
import hudson.util.RunList;
import jenkins.triggers.SCMTriggerItem;

public class ScmExcution {

	private static Logger LOG = Logger.getLogger(ScmExcution.class.getName());

	private boolean printChangeLog;

	private boolean handleAffectedPaths;

	private String cut;

	private String contain;

	private static final String GIT_DIR = ".git";

	private static final String SVN_DIR = ".svn";

	private static String scm_Type = "";

	private static final short RECURSION_DEPTH = 50;

	private String vcsDir;// directory relative to workspace to start searching for the VCS directory
							// (.git or .svn)

	private transient Repository gitRepository = null;

	private File svnRepository = null;

	private boolean isGit = false;

	private boolean isSvn = false;

	private transient LastChanges lastChanges = null;

	private transient FilePath vcsDirFound = null; // location of vcs directory (.git or .svn) in job workspace (is here
													// for caching purposes)

	private ScmExcution(boolean printChangeLog, boolean handleAffectedPaths, String cut, String contain) {
		this.printChangeLog = printChangeLog;
		this.handleAffectedPaths = handleAffectedPaths;
		this.cut = cut;
		this.contain = contain;
	}

	public ScmExcution() {

	}

	public ScmExcution from(ScmHttpClient ci, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
			Map<String, String> variables, boolean printChangeLog) throws IOException, InterruptedException {
		printChangeLog = ci.isPrintChangeLog();
		handleAffectedPaths = ci.isHandleAffectedPaths();
		cut = ci.getCut();
		contain = ci.getContain();
		doScmork(build, workspace, launcher, listener, variables, printChangeLog);
		return new ScmExcution(printChangeLog, handleAffectedPaths, cut, contain);
	}

	@SuppressWarnings("deprecation")
	public void doScmork(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener,
			Map<String, String> variables, boolean printChangeLog) throws IOException, InterruptedException {
		if (!variables.isEmpty()) {
			variables.clear();
		}
		ISVNAuthenticationProvider svnAuthProvider = null;
		FilePath workspaceTargetDir = getMasterWorkspaceDir(build);// always on master
		FilePath vcsDirParam = null; // folder to be used as param on vcs directory search
		FilePath vcsTargetDir = null; // directory on master workspace containing a copy of vcsDir (.git or .svn)
		if (this.vcsDir != null && !"".equals(vcsDir.trim())) {
			vcsDirParam = new FilePath(workspace, this.vcsDir);
		} else {
			vcsDirParam = workspace;
		}

		if (findVCSDir(vcsDirParam, GIT_DIR)) {
			isGit = true;
			scm_Type = "GIT";
			// workspace can be on slave so copy resources to master
			vcsTargetDir = new FilePath(new File(workspaceTargetDir.getRemote() + "/.git"));
			File remoteGitDir = new File(workspaceTargetDir.getRemote() + "/.git");
			// copy only if directory doesn't exists
			if (!remoteGitDir.exists() || !Files.newDirectoryStream(remoteGitDir.toPath()).iterator().hasNext()) {
				vcsDirFound.copyRecursiveTo("**/*", vcsTargetDir);
			}
			gitRepository = repository(workspaceTargetDir.getRemote() + "/.git");
		} else if (findVCSDir(vcsDirParam, SVN_DIR)) {
			isSvn = true;
			scm_Type = "SVN";
			SubversionSCM scm = null;
			try {
				Collection<? extends SCM> scMs = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(build.getParent())
						.getSCMs();
				scm = (SubversionSCM) scMs.iterator().next();
				svnAuthProvider = scm.createAuthenticationProvider(build.getParent(), scm.getLocations()[0]);
			} catch (NoSuchMethodError e) {
				if (scm != null) {
					svnAuthProvider = scm.getDescriptor().createAuthenticationProvider();
				}

			} catch (Exception ex) {
				LOG.log(Level.WARNING, "Problem creating svn auth provider", ex);
			}

			vcsTargetDir = new FilePath(new File(workspaceTargetDir.getRemote() + "/.svn"));
			File remoteSvnDir = new File(workspaceTargetDir.getRemote() + "/.svn");
			// copy only if directory doesn't exists
			if (!remoteSvnDir.exists() || !Files.newDirectoryStream(remoteSvnDir.toPath()).iterator().hasNext()) {
				vcsDirFound.copyRecursiveTo("**/*", vcsTargetDir);
			}

			svnRepository = new File(workspaceTargetDir.getRemote());

		}

		if (!isGit && !isSvn) {
			throw new RuntimeException(
					String.format("Git or Svn directories not found in workspace %s.", vcsDirParam.toURI().toString()));
		}
		boolean hasTargetRevision = false;
		String targetRevision = null;
		String targetBuild = null;
		String previousBuild = null; // create the diff with the revision of an previous build

		final EnvVars env = build.getEnvironment(listener);
		@SuppressWarnings("deprecation")
		Map<String, String> vars = build.getEnvVars();
		Set<Entry<String, String>> entries = vars.entrySet();
		for (Entry<String, String> entry : entries) {
			if (entry.getKey().equals("BUILD_ID")) {
				String currentNumber = entry.getValue();
				int previousNumber = Integer.parseInt(currentNumber) - 1;
				previousBuild = "" + previousNumber;
			}
		}
		if (!previousBuild.equals("0")) {
			targetBuild = env.expand(previousBuild);
			targetRevision = findBuildRevision(targetBuild, build.getParent().getBuilds());
			if (targetRevision.equals("")) {
				hasTargetRevision = false;
			} else {
				hasTargetRevision = true;
			}
		}

		List<CommitInfo> commitInfoList = null;
		try {
			if (isGit) {
				if (hasTargetRevision) {
					// compares current build repository revision with provided one
					lastChanges = GitLastChanges.getInstance().changesOf(gitRepository,
							GitLastChanges.getInstance().resolveCurrentRevision(gitRepository),
							gitRepository.resolve(targetRevision));
					commitInfoList = getCommitsBetweenRevisions(lastChanges.getCurrentRevision().getCommitId(),
							targetRevision, null);
					lastChanges.addCommits(
							commitChanges(commitInfoList, lastChanges.getPreviousRevision().getCommitId(), null));

				} else {
					// get current repository build revision
					lastChanges = GitLastChanges.getInstance().changesOf(gitRepository);
					lastChanges.addCommit(new CommitChanges(lastChanges.getCurrentRevision(), lastChanges.getDiff()));
				}

			} else if (isSvn) {
				SvnLastChanges svnLastChanges = getSvnLastChanges(svnAuthProvider);
				if (hasTargetRevision) {
					// compares current build repository revision with provided one
					Long svnRevision = Long.parseLong(targetRevision);
					lastChanges = svnLastChanges.changesOf(svnRepository, SVNRevision.HEAD,
							SVNRevision.create(svnRevision));
					commitInfoList = getCommitsBetweenRevisions(lastChanges.getCurrentRevision().getCommitId(),
							targetRevision, svnAuthProvider);
					lastChanges.addCommits(commitChanges(commitInfoList,
							lastChanges.getPreviousRevision().getCommitId(), svnAuthProvider));
				} else {
					// get current repository build revision
					lastChanges = svnLastChanges.changesOf(svnRepository);
					lastChanges.addCommit(new CommitChanges(lastChanges.getCurrentRevision(), lastChanges.getDiff()));
				}

			}
			build.addAction(new LastChangesBuildAction(build, lastChanges, null));
			if (commitInfoList == null) {
				listener.getLogger().println("the build has published changes first time [current]"
						+ lastChanges.getCurrentRevision().getCommitId());
			} else {
				if (commitInfoList.size() == 0) {
					listener.getLogger()
							.println("No changes between revision [current] "
									+ lastChanges.getCurrentRevision().getCommitId() + " and [previous]"
									+ lastChanges.getPreviousRevision().getCommitId());
				} else {
					JenkinsMessage jm = new JenkinsMessage();
					JenkinsEnvs jenkins_envs = getJenkinsEnvs(build, scm_Type);
					List<String> originAffectedPaths = getOriginAffectedPathList(build);
					jm.setCommitInfoList(commitInfoList);
					jm.setJes(jenkins_envs);
					jm.setAffectedPaths(originAffectedPaths);
					String CI = JSON.toJSONString(jm);
					if (printChangeLog) {
						listener.getLogger().println(" CI > " + CI + "\n");
					}
					variables.put("CI", CI);
					if (handleAffectedPaths == true) {
						List<String> affectedPaths = getAffectedPaths(build, handleAffectedPaths, cut, contain);
						String AFFECTED_PATH = JSON.toJSONString(affectedPaths);
						listener.getLogger().println("HANDLE_AFFECTED_PATH > " + AFFECTED_PATH + "\n");
						variables.put("AFFECTED_PATH", AFFECTED_PATH);
					} else {
						String AFFECTED_PATH = JSON.toJSONString(originAffectedPaths);
						variables.put("AFFECTED_PATH", AFFECTED_PATH);
					}

				}
			}
		} catch (Exception e) {
			listener.error("Changes NOT published due to the following error: "
					+ (e.getMessage() == null ? e.toString() : e.getMessage())
					+ (e.getCause() != null ? " - " + e.getCause() : ""));
			LOG.log(Level.SEVERE, "Could not publish Changes.", e);
		} finally {
			if (vcsTargetDir != null && vcsTargetDir.exists()) {
				vcsTargetDir.deleteRecursive();// delete copied dir on master
			}
		}
	}

	private FilePath getMasterWorkspaceDir(Run<?, ?> build) {
		if (build != null && build.getRootDir() != null) {
			return new FilePath(build.getRootDir());
		} else {
			return new FilePath(Paths.get("").toFile());
		}
	}

	/**
	 *
	 * @return boolean indicating weather the vcs directory was found or not
	 */
	private boolean findVCSDir(FilePath workspace, String dir) throws IOException, InterruptedException {
		FilePath vcsDir = null;
		if (workspace.child(dir).exists()) {
			vcsDirFound = workspace.child(dir);
			return true;
		}
		int recursionDepth = RECURSION_DEPTH;
		while ((vcsDir = findVCSDirInSubDirectories(workspace, dir)) == null && recursionDepth > 0) {
			recursionDepth--;
		}
		if (vcsDir == null) {
			return false;
		} else {
			vcsDirFound = vcsDir; // vcs directory gitDir;
			return true;
		}
	}

	private FilePath findVCSDirInSubDirectories(FilePath sourceDir, String dir)
			throws IOException, InterruptedException {
		List<FilePath> filePaths = sourceDir.listDirectories();
		if (filePaths == null || filePaths.isEmpty()) {
			return null;
		}

		for (FilePath filePath : sourceDir.listDirectories()) {
			if (filePath.getName().equalsIgnoreCase(dir)) {
				return filePath;
			} else {
				return findVCSDirInSubDirectories(filePath, dir);
			}
		}
		return null;
	}

	private static String findBuildRevision(String targetBuild, RunList<?> builds) {

		if (builds == null || builds.isEmpty()) {
			return null;
		}

		Integer buildParam = null;
		try {
			buildParam = Integer.parseInt(targetBuild);
		} catch (NumberFormatException ne) {

		}
		if (buildParam == null) {
			throw new RuntimeException(String.format(
					"%s is an invalid build number for 'previousBuild' param. It must resolve to an integer.",
					targetBuild));
		}
		LastChangesBuildAction actionFound = null;
		for (@SuppressWarnings("rawtypes")
		Run build : builds) {
			if (build.getNumber() == buildParam) {
				actionFound = build.getAction(LastChangesBuildAction.class);
				break;
			}
		}

		if (actionFound == null) {
			return "";
		}

		return actionFound.getBuildChanges().getCurrentRevision().getCommitId();
	}

	/**
	 * Retrieve commits between two revisions
	 *
	 * @param currentRevision
	 * @param previousRevision
	 */
	private List<CommitInfo> getCommitsBetweenRevisions(String currentRevision, String previousRevision,
			ISVNAuthenticationProvider svnAuthProvider) throws IOException {
		List<CommitInfo> commits = new ArrayList<>();
		if (isGit) {
			commits = GitLastChanges.getInstance().getCommitsBetweenRevisions(gitRepository,
					gitRepository.resolve(currentRevision), gitRepository.resolve(previousRevision));
		} else if (isSvn) {
			commits = SvnLastChanges.getInstance(svnAuthProvider).getCommitsBetweenRevisions(svnRepository,
					SVNRevision.create(Long.parseLong(currentRevision)),
					SVNRevision.create(Long.parseLong(previousRevision)));
		}

		return commits;
	}

	private List<CommitChanges> commitChanges(List<CommitInfo> commitInfoList, String oldestCommit,
			ISVNAuthenticationProvider svnAuthProvider) {
		if (commitInfoList == null || commitInfoList.isEmpty()) {
			return null;
		}

		List<CommitChanges> commitChanges = new ArrayList<>();

		try {
			Collections.sort(commitInfoList, new Comparator<CommitInfo>() {
				@Override
				public int compare(CommitInfo c1, CommitInfo c2) {
					try {
						DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT);
						return format.parse(c1.getCommitDate()).compareTo(format.parse(c2.getCommitDate()));
					} catch (ParseException e) {
						LOG.severe(String.format("Could not parse commit dates %s and %s ", c1.getCommitDate(),
								c2.getCommitDate()));
						return 0;
					}
				}
			});

			for (int i = commitInfoList.size() - 1; i >= 0; i--) {
				LastChanges lastChanges = null;
				if (isGit) {
					ObjectId previousCommit = gitRepository.resolve(commitInfoList.get(i).getCommitId() + "^1");
					lastChanges = GitLastChanges.getInstance().changesOf(gitRepository,
							gitRepository.resolve(commitInfoList.get(i).getCommitId()), previousCommit);
				} else {
					if (i == 0) {
						lastChanges = SvnLastChanges.getInstance(svnAuthProvider).changesOf(svnRepository,
								SVNRevision.parse(commitInfoList.get(i).getCommitId()),
								SVNRevision.parse(oldestCommit));
					} else { // get changes comparing current commit (i) with previous one (i -1)
						lastChanges = SvnLastChanges.getInstance(svnAuthProvider).changesOf(svnRepository,
								SVNRevision.parse(commitInfoList.get(i).getCommitId()),
								SVNRevision.parse(commitInfoList.get(i - 1).getCommitId()));
					}
				}
				String diff = lastChanges != null ? lastChanges.getDiff() : "";
				commitChanges.add(new CommitChanges(commitInfoList.get(i), diff));
			}

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Could not get commit changes.", e);
		}

		return commitChanges;
	}

	private SvnLastChanges getSvnLastChanges(ISVNAuthenticationProvider svnAuthProvider) {
		return svnAuthProvider != null ? SvnLastChanges.getInstance(svnAuthProvider) : SvnLastChanges.getInstance();
	}

	@SuppressWarnings("null")
	private JenkinsEnvs getJenkinsEnvs(Run<?, ?> build, String scm_Type) {
		JenkinsEnvs jes = new JenkinsEnvs();
		@SuppressWarnings("deprecation")
		Map<String, String> vars = build.getEnvVars();
		Set<Entry<String, String>> entries = vars.entrySet();
		for (Entry<String, String> entry : entries) {
			if (entry.getKey().equals("BUILD_ID")) {
				String build_id = entry.getValue();
				jes.setBuild_id(build_id);
			}
			if (entry.getKey().equals(scm_Type + "_URL")) {
				String scm_Url = entry.getValue();
				jes.setScm_url(scm_Url);
			}
			if (scm_Type.equals("SVN")) {
				if (entry.getKey().equals(scm_Type + "_REVISION")) {
					String scm_Revision = entry.getValue();
					jes.setScm_branch(scm_Revision);
				}
			}
			if (scm_Type.equals("GIT")) {
				if (entry.getKey().equals(scm_Type + "_BRANCH")) {
					String scm_Bracnch = entry.getValue();
					jes.setScm_branch(scm_Bracnch);
				}
			}

			if (entry.getKey().equals("JOB_NAME")) {
				String job_name = entry.getValue();
				jes.setJob_name(job_name);
			}
			if (entry.getKey().equals("LOGNAME")) {
				String logname = entry.getValue();
				jes.setLogname(logname);
			}
		}
		return jes;
	}

	private List<String> getOriginAffectedPathList(Run<?, ?> build) {
		List<String> affectedPaths = new ArrayList<>();
		@SuppressWarnings("rawtypes")
		AbstractBuild build2 = (AbstractBuild) build;
		@SuppressWarnings("unchecked")
		ChangeLogSet<ChangeLogSet.Entry> cls = build2.getChangeSet();
		if (!cls.isEmptySet()) {
			for (ChangeLogSet.Entry e : cls) {
				Collection<String> paths = e.getAffectedPaths();
				@SuppressWarnings("rawtypes")
				Iterator it = paths.iterator();
				while (it.hasNext()) {
					String path = (String) it.next();
					if (path.substring(0, 1).equals("/"))
						path = path.substring(1, path.length());
					affectedPaths.add(path);
				}
			}
		}
		return affectedPaths;
	}

	private List<String> getAffectedPaths(Run<?, ?> build, boolean handleAffectedPaths, String cut, String contain) {
		List<String> affectedPaths = new ArrayList<>();
		@SuppressWarnings("rawtypes")
		AbstractBuild build2 = (AbstractBuild) build;
		@SuppressWarnings("unchecked")
		ChangeLogSet<ChangeLogSet.Entry> cls = build2.getChangeSet();
		if (!cls.isEmptySet()) {
			for (ChangeLogSet.Entry e : cls) {
				Collection<String> paths = e.getAffectedPaths();
				@SuppressWarnings("rawtypes")
				Iterator it = paths.iterator();
				while (it.hasNext()) {
					String path = (String) it.next();
					if (path.substring(0, 1).equals("/"))
						path = path.substring(1, path.length());
					if (handleAffectedPaths) {
						if (!cut.isEmpty() && path.contains(cut)) {
							path = path.replace(cut, "");
						}
						if (!contain.isEmpty()) {
							if (contain.contains(";")) {
								String[] p = contain.split(";");
								for (int i = 0; i < p.length; i++) {
									if (path.contains(p[i])) {
										path = p[i];
									}
								}
							} else {
								if (path.contains(contain)) {
									path = contain;
								}
							}
						}
					}
					affectedPaths.add(path);
				}
			}
		}
		return affectedPaths;
	}

}
