/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jirban.jira.impl.board;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.dmr.ModelNode;

/**
 * Keeps track of states, issue types and priorities that affected issues belong to, but have not been
 * configured in the board configuration. Once an issue is on the blacklist, we no longer try to update
 * it. The board needs reconfiguring and the subsequent full reload to get issues off the blacklist.
 *
 * @author Kabir Khan
 */
public class Blacklist {
    /**
     * The missing states from the configuration
     */
    private final Set<String> missingStates;

    /**
     * The missing issueTypes from the configuration
     */
    private final Set<String> missingIssueTypes;

    /**
     * The missing priorities from the configuration
     */
    private final Set<String> missingPriorities;

    /**
     * The issues that have been blacklisted
     */
    private final Set<String> issues;

    private Blacklist(Set<String> missingStates, Set<String> missingIssueTypes, Set<String> missingPriorities, Set<String> issues) {
        this.missingStates = missingStates;
        this.missingIssueTypes = missingIssueTypes;
        this.missingPriorities = missingPriorities;
        this.issues = issues;
    }

    void serialize(ModelNode parent) {
        ModelNode blacklist = new ModelNode();
        serializeSet(blacklist, "states", missingStates);
        serializeSet(blacklist, "issue-types", missingIssueTypes);
        serializeSet(blacklist, "priorities", missingPriorities);
        serializeSet(blacklist, "issues", issues);
        if (blacklist.isDefined()) {
            parent.get("blacklist").set(blacklist);
        }
    }

    private void serializeSet(ModelNode blacklist, String key, Set<String> set) {
        if (set.isEmpty()) {
            return;
        }
        for (String value : set) {
            blacklist.get(key).add(value);
        }
    }

    public boolean isBlacklisted(String issueKey) {
        return issues.contains(issueKey);
    }

    abstract static class Accessor {
        abstract void addMissingState(String issueKey, String state);
        abstract void addMissingIssueType(String issueKey, String issueType);
        abstract void addMissingPriority(String issueKey, String priority);
        abstract boolean isUpdated();
        public abstract void deleteIssue(String issueKey);
    }

    static class Builder extends Accessor {
        private Set<String> missingStates;
        private Set<String> missingIssueTypes;
        private Set<String> missingPriorities;
        private Set<String> issues;


        @Override
        void addMissingState(String issueKey, String state) {
            if (missingStates == null) {
                missingStates = new TreeSet<>();
            }
            missingStates.add(state);
            blacklistIssue(issueKey);
        }

        @Override
        void addMissingIssueType(String issueKey, String issueType) {
            if (missingIssueTypes == null) {
                missingIssueTypes = new TreeSet<>();
            }
            missingIssueTypes.add(issueType);
            blacklistIssue(issueKey);
        }

        @Override
        void addMissingPriority(String issueKey, String priority) {
            if (missingPriorities == null) {
                missingPriorities = new TreeSet<>();
            }
            missingPriorities.add(priority);
            blacklistIssue(issueKey);
        }


        @Override
        boolean isUpdated() {
            return issues != null;
        }

        @Override
        public void deleteIssue(String issueKey) {
            //Should not happen with this code path
            throw new IllegalStateException();
        }

        private void blacklistIssue(String issueKey) {
            if (issues == null) {
                issues = new TreeSet<>();
            }
            issues.add(issueKey);
        }

        Blacklist build() {
            return new Blacklist(
                missingStates == null ? Collections.emptySet() : Collections.unmodifiableSet(missingStates),
                missingIssueTypes == null ? Collections.emptySet() : Collections.unmodifiableSet(missingIssueTypes),
                missingPriorities == null ? Collections.emptySet() : Collections.unmodifiableSet(missingPriorities),
                issues == null ? Collections.emptySet() : Collections.unmodifiableSet(issues));
        }
    }

    static class Updater extends Accessor {
        private final Blacklist original;
        private boolean updated;
        private Set<String> missingStates;
        private Set<String> missingIssueTypes;
        private Set<String> missingPriorities;
        private Set<String> issues;

        public Updater(Blacklist original) {
            this.original = original;
        }

        @Override
        void addMissingState(String issueKey, String state) {
            if (missingStates == null) {
                missingStates = new TreeSet<>(original.missingStates);
            }
            missingStates.add(state);
            blacklistIssue(issueKey);
            updated = true;
        }

        @Override
        void addMissingIssueType(String issueKey, String issueType) {
            if (missingIssueTypes == null) {
                missingIssueTypes = new TreeSet<>(original.missingIssueTypes);
            }
            missingIssueTypes.add(issueType);
            blacklistIssue(issueKey);
            updated = true;
        }

        @Override
        void addMissingPriority(String issueKey, String priority) {
            if (missingPriorities == null) {
                missingPriorities = new TreeSet<>(original.missingPriorities);
            }
            missingPriorities.add(priority);
            blacklistIssue(issueKey);
            updated = true;
        }

        @Override
        boolean isUpdated() {
            return updated;
        }

        @Override
        public void deleteIssue(String issueKey) {
            if (issues == null) {
                issues = new TreeSet<>(original.issues);
            }
            issues.remove(issueKey);
            updated = true;
        }

        private void blacklistIssue(String issueKey) {
            if (issues == null) {
                issues = new TreeSet<>(original.issues);
            }
            issues.add(issueKey);
        }

        Blacklist build() {
            if (!updated) {
                return original;
            }

            return new Blacklist(
                missingStates == null ? original.missingStates : Collections.unmodifiableSet(missingStates),
                missingIssueTypes == null ? original.missingIssueTypes : Collections.unmodifiableSet(missingIssueTypes),
                missingPriorities == null ? original.missingPriorities : Collections.unmodifiableSet(missingPriorities),
                issues == null ? original.issues : Collections.unmodifiableSet(issues));
        }
    }
}
