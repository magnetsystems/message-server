/*   Copyright (c) 2016 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.protocol.TemplateDataModel;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.MMXPushConfigService;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplate;
import com.magnet.mmx.server.plugin.mmxmgmt.push.config.model.MMXTemplateType;
import org.apache.commons.lang3.StringUtils;

import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by mmicevic on 4/6/16.
 *
 */
@Path("/integration/templates")
public class TemplateResource {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTemplate(TemplateDataModel.TemplateRequest request) {

        RestMethod<TemplateDataModel.TemplateRequest, TemplateDataModel.TemplateResponse> method = new RestMethod<TemplateDataModel.TemplateRequest, TemplateDataModel.TemplateResponse>() {
            @Override
            public TemplateDataModel.TemplateResponse execute(TemplateDataModel.TemplateRequest request) throws MMXException {
                //convert request
                MMXTemplate t = convertRequest(request);

                //do job
                t = MMXPushConfigService.getInstance().createTemplate(t);

                //convert and return response
                return convertResponse(t);
            }
        };
        return method.doMethod(request);

    }
    @GET
    @Path("/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrieveTemplateById(@PathParam("templateId") Integer templateId) {

        RestMethod<Integer, TemplateDataModel.TemplateResponse> method = new RestMethod<Integer, TemplateDataModel.TemplateResponse>() {
            @Override
            public TemplateDataModel.TemplateResponse execute(Integer templateId) throws MMXException {

                //convert request
                //do job
                MMXTemplate t = MMXPushConfigService.getInstance().getTemplate(templateId);

                //convert and return response
                return convertResponse(t);
            }
        };
        return method.doMethod(templateId);
    }
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retrieveAllTemplatesForApp(@QueryParam("appId") String appId) {

        RestMethod<String, Collection<TemplateDataModel.TemplateResponse>> method = new RestMethod<String,Collection<TemplateDataModel.TemplateResponse>>() {
            @Override
            public Collection<TemplateDataModel.TemplateResponse> execute(String appId) throws MMXException {

                //convert request
                //do job
                Collection<MMXTemplate> t = MMXPushConfigService.getInstance().getAllTemplates(appId);

                //convert and return response
                return convertResponse(t);
            }
        };
        return method.doMethod(appId);
    }
    @PUT
    @Path("/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTemplate(@PathParam("templateId") final Integer templateId, TemplateRequest request) {

        RestMethod<TemplateDataModel.TemplateRequest, TemplateDataModel.TemplateResponse> method = new RestMethod<TemplateDataModel.TemplateRequest, TemplateDataModel.TemplateResponse>() {
            @Override
            public TemplateDataModel.TemplateResponse execute(TemplateDataModel.TemplateRequest request) throws MMXException {
                //convert request
                MMXTemplate t = convertRequest(request);
                t.setTemplateId(templateId);

                //do job
                t = MMXPushConfigService.getInstance().updateTemplate(t);

                //convert and return response
                return convertResponse(t);
            }
        };
        return method.doMethod(request);
    }
    @DELETE
    @Path("/{templateId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteTemplate(@PathParam("templateId") Integer templateId) {

        RestMethod<Integer, RestMethod.SimpleMessage> method = new RestMethod<Integer,RestMethod.SimpleMessage>() {
            @Override
            public SimpleMessage execute(Integer templateId) throws MMXException {
                //convert request

                //do job
                MMXPushConfigService.getInstance().deleteTemplate(templateId);

                //convert and return response
                return new RestMethod.SimpleMessage("deleted");
            }
        };
        return method.doMethod(templateId);
    }

    private static class MockTemplateLoader implements TemplateLoader {
      @Override
      public void closeTemplateSource(Object templateSrc) throws IOException {
      }

      @Override
      public Object findTemplateSource(String name) throws IOException {
        try {
          int templateId = Integer.parseInt(name);
          MMXTemplate template = MMXPushConfigService.getInstance().getTemplate(templateId);
          return template.getTemplate();
        } catch (Throwable e) {
          return null;
        }
      }

      @Override
      public long getLastModified(Object templateSrc) {
        return 0;
      }

      @Override
      public Reader getReader(Object templateSrc, String encoding) throws IOException {
        return new StringReader((String) templateSrc);
      }
    }

    @POST
    @Path("/validation/{templateId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validateTemplate(@PathParam("templateId") final int templateId,
                                    TemplateDataModel.ValidationRequest request) {
      RestMethod<TemplateDataModel.ValidationRequest, TemplateDataModel.ValidationResponse> method =
          new RestMethod<TemplateDataModel.ValidationRequest, TemplateDataModel.ValidationResponse>() {
        @Override
        public TemplateDataModel.ValidationResponse execute(TemplateDataModel.ValidationRequest request) throws MMXException {
          TemplateDataModel.ValidationResponse response = new TemplateDataModel.ValidationResponse();

          HashMap<String, Object> context = new HashMap<String, Object>();
          context.put("application", request.getApplication());
          context.put("channel", request.getChannel());
          context.put("config", request.getConfig());
          context.put("msg", request.getMsg());

          try {
            Configuration fmConfig = new Configuration(Configuration.VERSION_2_3_24);
            fmConfig.setLocalizedLookup(false);
            fmConfig.setTemplateLoader(new MockTemplateLoader());
            fmConfig.setDefaultEncoding("UTF-8");
            fmConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            fmConfig.setLogTemplateExceptions(false);

            Template template = fmConfig.getTemplate(String.valueOf(templateId));
            StringWriter out = new StringWriter();
            template.process(context, out);
            String props = out.getBuffer().toString();
            response.setSuccessResult(props);
          } catch (Throwable e) {
            response.setErrorResult(e.getMessage());
          }
          return response;
        }
      };

      return method.doMethod(request);
    }

    private static MMXTemplate convertRequest(TemplateDataModel.TemplateRequest request) {

        MMXTemplate t = new MMXTemplate();
        t.setAppId(StringUtils.isBlank(request.getAppId()) ? null : request.getAppId());
        t.setTemplateName(StringUtils.isBlank(request.getTemplateName()) ? null : request.getTemplateName());
        t.setTemplate(StringUtils.isBlank(request.getTemplate()) ? null : request.getTemplate());
        t.setTemplateType(MMXTemplateType.PUSH);
        return t;
    }
    private static TemplateDataModel.TemplateResponse convertResponse(MMXTemplate response) {

        TemplateDataModel.TemplateResponse t = new TemplateDataModel.TemplateResponse();
        t.setTemplateId(response.getTemplateId());
        t.setAppId(response.getAppId());
        t.setTemplateName(response.getTemplateName());
        t.setTemplate(response.getTemplate());
        t.setTemplateType(response.getTemplateType().name());
        return t;
    }
    private static Collection<TemplateDataModel.TemplateResponse> convertResponse(Collection<MMXTemplate> response) {

        if (response == null) {
            return null;
        }
        List<TemplateDataModel.TemplateResponse> list = new ArrayList<>();
        for (MMXTemplate t : response) {
            list.add(convertResponse(t));
        }
        return list;
    }
}
