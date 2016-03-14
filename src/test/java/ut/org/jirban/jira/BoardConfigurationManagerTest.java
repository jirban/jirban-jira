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

import static org.jirban.jira.impl.Constants.HEADER;
import static org.jirban.jira.impl.Constants.STATES;

import java.io.IOException;
import java.util.List;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanValidationException;
import org.jirban.jira.api.BoardConfigurationManager;
import org.jirban.jira.impl.BoardConfigurationManagerBuilder;
import org.jirban.jira.impl.Constants;
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
        testHeaderConfigurations(original, new String[]{"A", "A", "A", "A"});

        //Same header for pairs should work
        testHeaderConfigurations(original, new String[]{"A", "A", "B", "B"});

        //Gaps are ok
        testHeaderConfigurations(original, new String[]{"A", "A", null, "B"});
        testHeaderConfigurations(original, new String[]{"A", null, null, "B"});
        testHeaderConfigurations(original, new String[]{"A", null, null, "B"});
        testHeaderConfigurations(original, new String[]{null, null, null, "B"});
        testHeaderConfigurations(original, new String[]{null, null, "A", "B"});

        //No headers are ok
        testHeaderConfigurations(original, new String[]{null, null, null, null});

        //Test bad configs, we cannot reuse a header if it was not the last one
        testBadHeaderConfigurations(original, new String[]{"A", "B", "A", null});
        testBadHeaderConfigurations(original, new String[]{"A", null, "A", null});
        testBadHeaderConfigurations(original, new String[]{null, "B", "A", "B"});

    }

    private void testBadHeaderConfigurations(ModelNode original, String[] stateHeaders) throws IOException {
        try {
            testHeaderConfigurations(original, stateHeaders);
            Assert.fail("Expected failure");
        } catch (JirbanValidationException expected) {
        }
    }

    private void testHeaderConfigurations(ModelNode original, String[] stateHeaders) throws IOException {
        BoardConfigurationManagerBuilder cfgManagerBuilder = new BoardConfigurationManagerBuilder();
        ModelNode copy = cloneAndAddStateHeaders(original, stateHeaders);
        BoardConfigurationManager cfgManager = cfgManagerBuilder.addConfigActiveObject(copy).build();
        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, 0);
        Assert.assertNotNull(boardConfig);
    }

    private ModelNode cloneAndAddStateHeaders(ModelNode original, String[] stateHeaders) {
        ModelNode copy = original.clone();
        List<ModelNode> states = copy.get(STATES).asList();
        for (int i = 0 ; i < stateHeaders.length ; i++) {
            if (stateHeaders[i] != null) {
                states.get(i).get(HEADER).set(stateHeaders[i]);
            }
        }
        return copy;
    }
}
