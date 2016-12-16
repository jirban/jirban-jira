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
package org.jirban.jira.impl.board;

import static org.jirban.jira.impl.Constants.ISSUE_TYPES;
import static org.jirban.jira.impl.Constants.PRIORITIES;
import static org.jirban.jira.impl.Constants.STATES;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.impl.Constants;

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
        serializeSet(blacklist, STATES, missingStates);
        serializeSet(blacklist, ISSUE_TYPES, missingIssueTypes);
        serializeSet(blacklist, PRIORITIES, missingPriorities);
        serializeSet(blacklist, Constants.ISSUES, issues);
        if (blacklist.isDefined()) {
            parent.get("blacklist").set(blacklist);
        }
    }

    private void serializeSet(ModelNode blacklist, String key, Set<String> set) {
        if (set.isEmpty()) {
            return;
        }
        set.forEach(value -> blacklist.get(key).add(value));
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

        public abstract boolean isBlackListed(String issueKey);
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
        public boolean isBlackListed(String issueKey) {
            if (issues == null) {
                return false;
            }
            return issues.contains(issueKey);
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
        private String deletedIssue;

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
        public boolean isBlackListed(String issueKey) {
            if (issues == null) {
                return original.isBlacklisted(issueKey);
            }
            return issues.contains(issueKey);
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
            deletedIssue = issueKey;
            updated = true;
        }

        String getDeletedIssue() {
            return deletedIssue;
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

        String getAddedState() {
            return getAddition(original.missingStates, missingStates);
        }

        String getAddedIssueType() {
            return getAddition(original.missingIssueTypes, missingIssueTypes);
        }

        String getAddedPriority() {
            return getAddition(original.missingPriorities, missingPriorities);
        }

        String getAddedIssue() {
            return getAddition(original.issues, issues);

        }

        private String getAddition(Set<String> original, Set<String> current) {
            if (current == null) {
                return null;
            }
            Set<String> tmp = new HashSet<>(current);
            tmp.removeAll(original);
            if (tmp.size() == 0) {
                //Should not really be the case, but just in case
                return null;
            }
            //Can only be one atm
            return tmp.iterator().next();
        }
    }
}
