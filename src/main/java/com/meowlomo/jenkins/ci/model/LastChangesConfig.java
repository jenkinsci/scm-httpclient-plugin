package com.meowlomo.jenkins.ci.model;

public class LastChangesConfig {

	private SinceType since = SinceType.PREVIOUS_REVISION;// by default it is current revision -1

	public LastChangesConfig() {
	}

	public LastChangesConfig(SinceType since) {
		super();
		if (since != null) {
			this.since = since;
		}
	}

	public SinceType since() {
		return since;
	}

}
