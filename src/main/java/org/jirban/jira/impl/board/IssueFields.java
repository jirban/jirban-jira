/*
 *
 *  JBoss, Home of Professional Open Source
 *  Copyright 2016, Red Hat, Inc., and individual contributors as indicated
 *  by the @authors tag.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.jirban.jira.impl.board;

/**
 * @author Kabir Khan
 */
public class IssueFields {
    public static final String FIELDS = "fields";
    public static final String KEY = "key";
    public static final String SUMMARY = "summary";
    public static final String LINKS = "issuelinks";
    public static final String STATUS = "status";
    public static final String PRIORITY = "priority";
    public static final String ASSIGNEE = "assignee";
    public static final String ISSUE_TYPE = "issuetype";

    public static String[] OVERVIEW_FIELDS = new String[]{KEY, SUMMARY, LINKS, STATUS, PRIORITY, ASSIGNEE, ISSUE_TYPE};

    static String toQueryStringValue(String[] fields) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : fields) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
