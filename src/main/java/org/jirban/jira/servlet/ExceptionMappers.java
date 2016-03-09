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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanPermissionException;
import org.jirban.jira.JirbanValidationException;
import org.netbeans.lib.cvsclient.commandLine.command.status;

/**
 * Jax-rs exception mappers making sure that any errors are in the format expected by the client.
 *
 * @author Kabir Khan
 */
public class ExceptionMappers {
    @Provider
    public static class JirbanPermissionExceptionMapper implements ExceptionMapper<JirbanPermissionException> {
        @Override
        public Response toResponse(JirbanPermissionException e) {
            return getErrorResponse(Response.Status.FORBIDDEN, e, "Permission violation");
        }
    }

    @Provider
    public static class JirbanValidationExceptionMapper implements ExceptionMapper<JirbanValidationException> {
        @Override
        public Response toResponse(JirbanValidationException e) {
            return getErrorResponse(Response.Status.BAD_REQUEST, e, "Validation error");
        }
    }

    @Provider
    public static class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
        @Override
        public Response toResponse(WebApplicationException e) {

            return getErrorResponse(
                    Response.Status.fromStatusCode(e.getResponse().getStatus()),
                    e,
                    null);
        }
    }

    @Provider
    public static class RootExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception e) {
            return getErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, e, "Internal server error");
        }
    }

    static Response getErrorResponse(Response.Status statusCode, Exception e, String prefix) {
        ModelNode modelNode = new ModelNode();
        //Set these fields the same as what the default jira mapper seems to do
        String msg = e.getMessage() == null ? statusCode.getReasonPhrase() : e.getMessage();
        if (prefix != null) {
            msg = prefix + ": " + msg;
        }
        modelNode.get("message").set(msg);
        modelNode.get("status-code").set(statusCode.getStatusCode());
        return Response.status(statusCode).entity(modelNode.toJSONString(true)).build();
    }
}
