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
package ut.org.jirban.jira;

import static org.jirban.jira.impl.Constants.BACKLOG;
import static org.jirban.jira.impl.Constants.CODE;
import static org.jirban.jira.impl.Constants.DONE;
import static org.jirban.jira.impl.Constants.HEADER;
import static org.jirban.jira.impl.Constants.RANK_CUSTOM_FIELD_ID;
import static org.jirban.jira.impl.Constants.STATES;

import java.io.IOException;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.impl.BoardConfigurationManagerBuilder;
import org.jirban.jira.impl.config.BoardConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.mock.component.MockComponentWorker;

import ut.org.jirban.jira.mock.CustomFieldManagerBuilder;
import ut.org.jirban.jira.mock.UserManagerBuilder;

/**
 * @author Kabir Khan
 */
public class BoardConfigurationManagerTest {
    @Before
    public void initializeMocks() throws Exception {
        MockComponentWorker worker = new MockComponentWorker();
        new UserManagerBuilder()
                .addDefaultUsers()
                .build(worker);
        worker.init();
    }

    //TODO Add more tests for things like saving, and not having the correct permissions
    @Test
    public void testLoadConfiguration() throws IOException {
        BoardConfigurationManagerBuilder cfgManagerBuilder = new BoardConfigurationManagerBuilder()
                .addConfigActiveObjectsFromFile("config/board-tdp.json")
                .addSettingActiveObject(RANK_CUSTOM_FIELD_ID, "10000");
        BoardConfigurationManager cfgManager = cfgManagerBuilder.build();

        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-tdp.json");
        original.protect();

        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, "TST");
        Assert.assertNotNull(boardConfig);
        ModelNode serialized = boardConfig.serializeModelNodeForConfig();
        Assert.assertEquals(original, serialized);
    }

    @Test(expected=JirbanValidationException.class)
    public void testDuplicateStates() throws IOException {
        BoardConfigurationManagerBuilder cfgManagerBuilder = new BoardConfigurationManagerBuilder()
                .addConfigActiveObjectsFromFile("config/board-duplicates.json")
                .addSettingActiveObject(RANK_CUSTOM_FIELD_ID, "10000");
        BoardConfigurationManager cfgManager = cfgManagerBuilder.build();

        cfgManager.getBoardConfigForBoardDisplay(null, "TST");
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
    public void testLoadConfigurationWithDone() throws IOException {
        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-tdp.json");
        original.protect();

        //All states as done is allowed although it makes absolutely no sense in real life
        loadAndValidateConfiguration(original, new DoneModifier(true, true, true, true));

        //A few at the end is ok and normal
        //A few in the beginning is ok and normal
        loadAndValidateConfiguration(original, new DoneModifier(false, false, false, true));
        loadAndValidateConfiguration(original, new DoneModifier(false, false, true, true));

        //Gaps are bad, and it must be the last states
        loadBadConfiguration(original, new DoneModifier(false, true, false, true));
        loadBadConfiguration(original, new DoneModifier(false, false, true, false));
    }

    @Test
    public void testLoadConfigurationWithBacklogAndHeaders() throws IOException {
        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-tdp.json");
        original.protect();

        //A state can have at the most of one of [header, backlog, done]
        //ok
        loadAndValidateConfiguration(original,
                new BacklogModifier(true, true, false, false),
                new StateHeaderModifier(null, null, "A", "B"));

        loadAndValidateConfiguration(original,
                new BacklogModifier(true, false, false, false),
                new StateHeaderModifier(null, null, "A", "B"));

        loadAndValidateConfiguration(original,
                new BacklogModifier(true, true, false, false),
                new StateHeaderModifier(null, null, "A", null),
                new DoneModifier(false, false, false, true));


        //not ok
        loadBadConfiguration(original,
                new BacklogModifier(true, true, false, false),
                new StateHeaderModifier(null, "A", "A", "B"));
        loadBadConfiguration(original,
                new BacklogModifier(true, false, false, false),
                new StateHeaderModifier("A", "A", "A", "B"));
        loadBadConfiguration(original,
                new DoneModifier(false, false, true, true),
                new StateHeaderModifier("A", "A", "A", "B"));
        loadBadConfiguration(original,
                new BacklogModifier(true, true, true, false),
                new DoneModifier(false, false, true, true));
    }

    @Test
    public void testLoadConfigurationWithCustomFields() throws IOException {
        BoardConfigurationManagerBuilder cfgManagerBuilder = new BoardConfigurationManagerBuilder()
                .addConfigActiveObjectsFromFile("config/board-custom.json")
                .setCustomFieldManager(CustomFieldManagerBuilder.loadFromResource("config/board-custom.json"))
                .addSettingActiveObject(RANK_CUSTOM_FIELD_ID, "10000");
        BoardConfigurationManager cfgManager = cfgManagerBuilder.build();

        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-custom.json");
        original.protect();

        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, "TST");
        Assert.assertNotNull(boardConfig);
        ModelNode serialized = boardConfig.serializeModelNodeForConfig();
        Assert.assertEquals(original, serialized);
    }

    @Test
    public void testLoadConfigurationWithParallelTasks() throws IOException {
        BoardConfigurationManagerBuilder cfgManagerBuilder = new BoardConfigurationManagerBuilder()
                .addConfigActiveObjectsFromFile("config/board-parallel-tasks.json")
                .setCustomFieldManager(CustomFieldManagerBuilder.loadFromResource("config/board-parallel-tasks.json"))
                .addSettingActiveObject(RANK_CUSTOM_FIELD_ID, "10000");
        BoardConfigurationManager cfgManager = cfgManagerBuilder.build();

        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-parallel-tasks.json");
        original.protect();

        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, "TST");
        Assert.assertNotNull(boardConfig);
        ModelNode serialized = boardConfig.serializeModelNodeForConfig();
        Assert.assertEquals(original, serialized);
    }


    @Test
    public void testLoadConfigurationWithWip() throws IOException {
        BoardConfigurationManagerBuilder cfgManagerBuilder = new BoardConfigurationManagerBuilder()
                .addConfigActiveObjectsFromFile("config/board-wip.json")
                .addSettingActiveObject(RANK_CUSTOM_FIELD_ID, "10000");
        BoardConfigurationManager cfgManager = cfgManagerBuilder.build();

        ModelNode original = BoardConfigurationManagerBuilder.loadConfig("config/board-wip.json");
        original.protect();

        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, "TST");
        Assert.assertNotNull(boardConfig);
        ModelNode serialized = boardConfig.serializeModelNodeForConfig();
        Assert.assertEquals(original, serialized);


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
        BoardConfigurationManager cfgManager = cfgManagerBuilder.addConfigActiveObject(copy.get(CODE).asString(), copy).build();
        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, "TST");
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

    private static class DoneModifier implements StateModifier {
        private final boolean[] done;

        public DoneModifier(boolean... done) {
            this.done = done;
        }

        @Override
        public void modify(int index, ModelNode state) {
            if (done[index]) {
                state.get(DONE).set(done[index]);
            }
        }
    }
}
