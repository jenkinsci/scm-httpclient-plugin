package com.meowlomo.jenkins.ci.api;

import org.tmatesoft.svn.core.SVNException;

import com.meowlomo.jenkins.ci.model.CommitInfo;
import com.meowlomo.jenkins.ci.model.LastChanges;

import java.util.List;

/**
 * Created by rmpestano on 7/10/16.
 */
public interface VCSChanges<REPOSITORY, REVISION> {


    LastChanges changesOf(REPOSITORY repository);

    LastChanges changesOf(REPOSITORY repository, REVISION currentRevision, REVISION previousRevision);

    REVISION getLastTagRevision(REPOSITORY repository) throws SVNException;

    REVISION resolveCurrentRevision(REPOSITORY repository);

    CommitInfo commitInfo(REPOSITORY repository, REVISION revision);

    List<CommitInfo> getCommitsBetweenRevisions(REPOSITORY repository, REVISION currentRevision, REVISION previousRevision);
}
