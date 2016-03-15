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
package ut.org.jirban.jira;

import static org.jirban.jira.impl.Constants.BACKLOG;
import static org.jirban.jira.impl.Constants.HEADER;
import static org.jirban.jira.impl.Constants.STATES;

import java.io.IOException;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.impl.BoardConfigurationManagerBuilder;
import org.jirban.jira.impl.config.BoardConfig;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Kabir Khan
 */
public class BoardConfigurationManagerTest {

    //TODO Add more tests for things like saving, and not having the correct permissions
    @Test
    public void testLoadConfiguration() throws IOException {
        BoardConfigurationManagerBuilder cfgManagerBuilder = new BoardConfigurationManagerBuilder();
        cfgManagerBuilder.addConfigActiveObjects("config/board-tdp.json");
        BoardConfigurationManager cfgManager = cfgManagerBuilder.build();

        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, 0);
        Assert.assertNotNull(boardConfig);
    }

    @Test
    public void testLoadConfigurationWithHeaders() throws IOException {
        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-tdp.json");
        original.protect();


        //Same header for all 4 states should work
        loadAndValidateConfiguration(original, new StateHeaderModifier("A", "A", "A", "A"));

        //Same header for pairs should work
        loadAndValidateConfiguration(original, new StateHeaderModifier("A", "A", "B", "B"));

        //Gaps are ok
        loadAndValidateConfiguration(original, new StateHeaderModifier("A", "A", null, "B"));
        loadAndValidateConfiguration(original, new StateHeaderModifier("A", null, null, "B"));
        loadAndValidateConfiguration(original, new StateHeaderModifier("A", null, null, "B"));
        loadAndValidateConfiguration(original, new StateHeaderModifier(null, null, null, "B"));
        loadAndValidateConfiguration(original, new StateHeaderModifier(null, null, "A", "B"));

        //No headers are ok
        loadAndValidateConfiguration(original, new StateHeaderModifier(null, null, null, null));

        //Test bad configs, we cannot reuse a header if it was not the last one
        loadBadConfiguration(original, new StateHeaderModifier("A", "B", "A", null));
        loadBadConfiguration(original, new StateHeaderModifier("A", null, "A", null));
        loadBadConfiguration(original, new StateHeaderModifier(null, "B", "A", "B"));

    }

    @Test
    public void testLoadConfigurationWithBacklog() throws IOException {
        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-tdp.json");
        original.protect();

        //All states as backlog is allowed although it makes little sense in real life
        loadAndValidateConfiguration(original, new BacklogModifier(true, true, true, true));

        //A few in the beginning is ok and normal
        loadAndValidateConfiguration(original, new BacklogModifier(true, true, false, false));
        loadAndValidateConfiguration(original, new BacklogModifier(true, false, false, false));
        //None is ok too
        loadAndValidateConfiguration(original, new BacklogModifier(false, false, false, false));

        //Gaps are bad, and it must be the first states
        loadBadConfiguration(original, new BacklogModifier(true, false, true, false));
        loadBadConfiguration(original, new BacklogModifier(false, false, true, false));
    }

    @Test
    public void testLoadConfigurationWithBacklogAndHeaders() throws IOException {
        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-tdp.json");
        original.protect();

        //Headers and backlog cannot be used for the same states
        //ok
        loadAndValidateConfiguration(original,
                new BacklogModifier(true, true, false, false),
                new StateHeaderModifier(null, null, "A", "B"));
        //not ok
        loadBadConfiguration(original,
                new BacklogModifier(true, true, false, false),
                new StateHeaderModifier(null, "A", "A", "B"));
        loadBadConfiguration(original,
                new BacklogModifier(true, false, false, false),
                new StateHeaderModifier("A", "A", "A", "B"));
    }

    private void loadBadConfiguration(ModelNode original, StateModifier... modifiers) throws IOException {
        try {
            loadAndValidateConfiguration(original, modifiers);
            Assert.fail("Expected failure");
        } catch (JirbanValidationException expected) {
        }
    }

    private void loadAndValidateConfiguration(ModelNode original, StateModifier... modifiers) throws IOException {
        //Loading the config validates it

        BoardConfigurationManagerBuilder cfgManagerBuilder = new BoardConfigurationManagerBuilder();
        ModelNode copy = cloneAndModifyStates(original, modifiers);
        BoardConfigurationManager cfgManager = cfgManagerBuilder.addConfigActiveObject(copy).build();
        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, 0);
        Assert.assertNotNull(boardConfig);
    }

    private ModelNode cloneAndModifyStates(ModelNode original, StateModifier... modifiers) {
        ModelNode copy = original.clone();
        List<ModelNode> states = copy.get(STATES).asList();
        for (int i = 0 ; i < states.size() ; i++) {
            ModelNode state = states.get(i);
            for (StateModifier modifier : modifiers) {
                modifier.modify(i, state);
            }
        }
        return copy;
    }

    private interface StateModifier {
        void modify(int index, ModelNode state);
    }

    private static class StateHeaderModifier implements StateModifier {
        private final String[] headers;

        StateHeaderModifier(String...headers) {
            this.headers = headers;
        }

        @Override
        public void modify(int index, ModelNode state) {
            if (headers[index] != null) {
                state.get(HEADER).set(headers[index]);
            }
        }
    }

    private static class BacklogModifier implements StateModifier {
        private final boolean[] backlog;

        public BacklogModifier(boolean... backlog) {
            this.backlog = backlog;
        }

        @Override
        public void modify(int index, ModelNode state) {
            if (backlog[index]) {
                state.get(BACKLOG).set(backlog[index]);
            }
        }
    }
}
