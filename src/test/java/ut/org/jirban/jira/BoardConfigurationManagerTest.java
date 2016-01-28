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

import java.io.IOException;

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
        BoardConfigurationManager cfgManager= cfgManagerBuilder.build();

        BoardConfig boardConfig = cfgManager.getBoardConfigForBoardDisplay(null, 0);
        Assert.assertNotNull(boardConfig);
    }
}
