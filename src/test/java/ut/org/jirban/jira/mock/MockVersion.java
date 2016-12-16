/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ut.org.jirban.jira.mock;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;

/**
 * @author Kabir Khan
 */
public class MockVersion implements Version {
    public static Set<Version> createVersions(String...versionNames) {
        Set<Version> versions = new LinkedHashSet<>();
        for (String versionNmae : versionNames) {
            versions.add(new MockVersion(versionNmae));
        }
        return versions;
    }

    private final String name;

    public MockVersion(String name) {
        this.name = name;
    }

    @Override
    public Project getProject() {
        return null;
    }

    @Override
    public Long getProjectId() {
        return null;
    }

    @Override
    public Project getProjectObject() {
        return null;
    }

    @Nullable
    @Override
    public Long getId() {
        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public Long getSequence() {
        return null;
    }

    @Override
    public boolean isArchived() {
        return false;
    }

    @Override
    public boolean isReleased() {
        return false;
    }

    @Nullable
    @Override
    public Date getReleaseDate() {
        return null;
    }

    @Nullable
    @Override
    public Date getStartDate() {
        return null;
    }
}
