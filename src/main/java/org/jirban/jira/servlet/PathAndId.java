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
package org.jirban.jira.servlet;

/**
 * @author Kabir Khan
 */
class PathAndId {
    private final String method;
    private final String pathInfo;
    private final String path;
    private final Integer id;
    private  PathAndId next;

    private PathAndId(String method, String pathInfo, String path, Integer id, PathAndId next) {
        this.method = method;
        this.pathInfo = pathInfo;
        this.path = path;
        this.id = id;
        this.next = next;
    }

    Integer getId() {
        return id;
    }

    PathAndId getNext() {
        return next;
    }

    boolean isPath(String path) {
        return this.path.equals(path);
    }

    void validateId(boolean id) {
        if (id && this.id == null) {
            throw new InvalidPathFormatException(method, pathInfo, " does not have an id");
        }
        if (!id && this.id != null) {
            throw new InvalidPathFormatException(method, pathInfo, " should not have an id");
        }
    }

    static PathAndId parse(String method, String pathInfo) {
        if (pathInfo.length() <= 1) {
            throw new InvalidPathFormatException(method, pathInfo, " doesn't contain anything useful");
        }
        String[] elements = pathInfo.substring(1).split("/");
        if (elements.length > 4) {
            //Don't do more than two levels of objects for now
            throw new InvalidPathFormatException(method, pathInfo, " is too long");
        }

        PathAndId next = null;
        try {
            if (elements.length > 2) {
                Integer id = elements.length > 3 ? Integer.valueOf(elements[3]) : null;
                next = new PathAndId(method, pathInfo, elements[2], id, null);
                return new PathAndId(method, pathInfo, elements[0], Integer.valueOf(elements[1]), next);
            } else if (elements.length == 2) {
                return new PathAndId(method, pathInfo, elements[0], Integer.valueOf(elements[1]), null);
            } else {
                return new PathAndId(method, pathInfo, elements[0], null, null);
            }
        } catch (NumberFormatException e) {
            throw new InvalidPathFormatException(method, pathInfo, "could not create a number from the id part");
        }
    }

    static class InvalidPathFormatException extends RuntimeException {
        public InvalidPathFormatException(String method, String pathInfo, String message) {
            super(method + " " + pathInfo + " " + message);
        }
    }
}
