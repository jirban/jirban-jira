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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.api.BoardCfg;
import org.jirban.jira.api.BoardConfigurationManager;
import org.junit.Assert;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.sal.api.transaction.TransactionCallback;

import ut.org.jirban.jira.mock.IssueTypeManagerBuilder;
import ut.org.jirban.jira.mock.PermissionManagerBuilder;
import ut.org.jirban.jira.mock.PriorityManagerBuilder;

/**
 * @author Kabir Khan
 */
public class BoardConfigurationManagerBuilder {

    private final ActiveObjects activeObjects = mock(ActiveObjects.class);
    private final ProjectManager projectManager = mock(ProjectManager.class);


    private IssueTypeManager issueTypeManager = IssueTypeManagerBuilder.getDefaultIssueTypeManager();
    private PriorityManager priorityManager = PriorityManagerBuilder.getDefaultPriorityManager();
    private PermissionManager permissionManager = PermissionManagerBuilder.getAllowsAll();

    private List<ModelNode> activeObjectEntries = new ArrayList<>();

    public BoardConfigurationManagerBuilder addConfigActiveObjects(String... resources) throws IOException {
        for (String resource : resources) {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream(resource);
            Assert.assertNotNull(resource, in);
            try (InputStream bin = new BufferedInputStream(in)){
                activeObjectEntries.add(ModelNode.fromJSONStream(bin));
            }
        }
        return this;
    }

    public BoardConfigurationManagerBuilder setPriorityManager(PriorityManager priorityManager) {
        this.priorityManager = priorityManager;
        return this;
    }

    public BoardConfigurationManagerBuilder setIssueTypeManager(IssueTypeManager issueTypeManager) {
        this.issueTypeManager = issueTypeManager;
        return this;
    }

    public BoardConfigurationManagerBuilder setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
        return this;
    }

    public BoardConfigurationManager build() {
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).thenAnswer(invocation -> ((TransactionCallback)invocation.getArguments()[0]).doInTransaction());
        when(activeObjects.get(any(Class.class), anyInt())).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            Assert.assertEquals(BoardCfg.class, args[0]);
            int id = (Integer)args[1];
            if (id < activeObjectEntries.size()) {
                return new MockBoardCfg(id, "kabir", activeObjectEntries.get(id)).boardCfg;
            }
            return null;
        });


        return new BoardConfigurationManagerImpl(activeObjects, issueTypeManager, priorityManager, permissionManager, projectManager);
    }

    private static class MockBoardCfg {
        private final BoardCfg boardCfg = mock(BoardCfg.class);
        private int id;
        private String owningUserKey;
        private ModelNode modelNode;

        public MockBoardCfg(int id, String owningUserKey, ModelNode modelNode) {
            this.id = id;
            this.owningUserKey = owningUserKey;
            this.modelNode = modelNode;

            when(boardCfg.getName()).thenReturn(modelNode.get("name").asString());
            when(boardCfg.getConfigJson()).thenReturn(modelNode.toJSONString(true));
            when(boardCfg.getOwningUser()).thenReturn(owningUserKey);
            doAnswer(invocation -> this.modelNode = ModelNode.fromJSONString((String)invocation.getArguments()[0]))
                    .when(boardCfg).setConfigJson(anyString());
            doAnswer(invocation -> this.owningUserKey = (String)invocation.getArguments()[0])
                    .when(boardCfg).setOwningUserKey(anyString());
        }
    }
}
