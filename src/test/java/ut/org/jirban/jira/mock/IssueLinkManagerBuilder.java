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
package ut.org.jirban.jira.mock;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.atlassian.jira.issue.link.IssueLinkManager;

/**
 * @author Kabir Khan
 */
public class IssueLinkManagerBuilder {
    private final IssueLinkManager issueLinkManager = mock(IssueLinkManager.class);

    private List<String> outwardLinks = new ArrayList<>();
    private List<String> inwardLinks = new ArrayList<>();

    public IssueLinkManager build() {
        //TODO configure some links
        when(issueLinkManager.getInwardLinks(anyLong())).thenReturn(Collections.emptyList());
        when(issueLinkManager.getOutwardLinks(anyLong())).thenReturn(Collections.emptyList());
        return issueLinkManager;
    }
}
