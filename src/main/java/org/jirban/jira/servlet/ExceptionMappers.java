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
package org.jirban.jira.servlet;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.jboss.dmr.ModelNode;
import org.jirban.jira.JirbanPermissionException;
import org.jirban.jira.JirbanValidationException;

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
