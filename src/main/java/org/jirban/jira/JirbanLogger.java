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
package org.jirban.jira;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Kabir Khan
 */
public class JirbanLogger {
    private final Logger logger = LoggerFactory.getLogger("org.jirban.jira");

    public static final JirbanLogger LOGGER = new JirbanLogger();

    private JirbanLogger() {
    }

    public void trace(String msg, Object...params) {
        logger.trace(msg, params);
    }

    public void debug(String msg, Object...params) {
        logger.debug(msg, params);
    }

    public void info(String msg, Object...params) {
        logger.info(msg, params);
    }

    public void warn(String msg, Object...params) {
        logger.warn(msg, params);
    }

    public void error(String msg, Object... params) {
        logger.error(msg, params);
    }
}
