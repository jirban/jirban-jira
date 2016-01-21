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
package org.jirban.jira.impl;

import com.atlassian.jira.event.issue.AbstractIssueEventListener;
import com.atlassian.jira.event.issue.IssueEvent;

/**
 * The listener listening to issue events, and calling back to the state table.
 * This must be registered with Jira as mentioned in
 * <a href="https://confluence.atlassian.com/jira/listeners-185729466.html">https://confluence.atlassian.com/jira/listeners-185729466.html</a>
 *
 * @author Kabir Khan
 */
public class JirbanIssueEventListener extends AbstractIssueEventListener {
    @Override
    protected void handleDefaultIssueEvent(IssueEvent event) {
        System.out.println("---> Default issue event: " + event);
        super.handleDefaultIssueEvent(event);
    }

    @Override
    public void workflowEvent(IssueEvent event) {
        System.out.println("---> Workflow event: " + event);
        super.workflowEvent(event);
    }

    @Override
    public void customEvent(IssueEvent event) {
        System.out.println("---> Custom event: " + event);
        super.customEvent(event);
    }
}
