package com.meowlomo.jenkins.ci.model;

/**
 * Created by pestano on 20/03/16.
 */
public enum SinceType {

//    PREVIOUS_REVISION("Previous revision"),LAST_SUCCESSFUL_BUILD("Last successful build"), LAST_TAG("Last tag");
	PREVIOUS_REVISION("Previous build");
    public final String name;

    SinceType(String value) {
        this.name = value;
    }

    public String getName() {
        return name;
    }
}
