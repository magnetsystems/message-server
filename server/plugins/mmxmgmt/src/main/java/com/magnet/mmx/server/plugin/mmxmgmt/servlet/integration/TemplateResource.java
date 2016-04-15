package com.magnet.mmx.server.plugin.mmxmgmt.servlet.integration;

import com.magnet.mmx.server.common.data.AppEntity;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAO;
import com.magnet.mmx.server.plugin.mmxmgmt.db.AppDAOImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.db.OpenFireDBConnectionProvider;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubWakeupProvider.FmMmxTemplateLoader;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubWakeupProvider.FmPushConfig;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubWakeupProvider.MsgData;
import com.magnet.mmx.server.plugin.mmxmgmt.pubsub.PubSubWakeupProvider.NameDesc;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mmicevic on 4/6/16.
 *
 */
@Path("/integration/templates")
public class TemplateResource {

    @POST
//    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTemplate(TemplateRequest request) {

        RestMethod<TemplateRequest, TemplateResponse> method = new RestMethod<TemplateRequest, TemplateResponse>() {
            @Override
            public TemplateResponse execute(TemplateRequest request) throws MMXException {
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
    public Response retrieveTemplateById(@PathParam("templateId") int templateId) {

        RestMethod<Integer, TemplateResponse> method = new RestMethod<Integer, TemplateResponse>() {
            @Override
            public TemplateResponse execute(Integer templateId) throws MMXException {

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

        RestMethod<String, Collection<TemplateResponse>> method = new RestMethod<String,Collection<TemplateResponse>>() {
            @Override
            public Collection<TemplateResponse> execute(String appId) throws MMXException {

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
    public Response updateTemplate(@PathParam("templateId") final int templateId, TemplateRequest request) {

        RestMethod<TemplateRequest, TemplateResponse> method = new RestMethod<TemplateRequest, TemplateResponse>() {
            @Override
            public TemplateResponse execute(TemplateRequest request) throws MMXException {
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
    public Response deleteTemplate(@PathParam("templateId") int templateId) {

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
    @Path("/verify/{templateId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response verifyTemplate(@PathParam("templateId") final int templateId,
                                    TemplateVerifyRequest request) {
      RestMethod<TemplateVerifyRequest, TemplateVerifyResponse> method =
          new RestMethod<TemplateVerifyRequest, TemplateVerifyResponse>() {
        @Override
        public TemplateVerifyResponse execute(TemplateVerifyRequest request) throws MMXException {
          TemplateVerifyResponse response = new TemplateVerifyResponse();

          HashMap<String, Object> context = new HashMap<String, Object>();
          context.put("application", request.application);
          context.put("channel", request.channel);
          context.put("config", request.config);

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

    private static MMXTemplate convertRequest(TemplateRequest request) {

        MMXTemplate t = new MMXTemplate();
        t.setAppId(StringUtils.isBlank(request.appId) ? null : request.appId);
        t.setTemplateName(StringUtils.isBlank(request.templateName) ? null : request.templateName);
        t.setTemplate(StringUtils.isBlank(request.template) ? null : request.template);
        t.setTemplateType(MMXTemplateType.PUSH);
        return t;
    }
    private static TemplateResponse convertResponse(MMXTemplate response) {

        TemplateResponse t = new TemplateResponse();
        t.templateId = response.getTemplateId();
        t.appId = response.getAppId();
        t.templateName = response.getTemplateName();
        t.template = response.getTemplate();
        t.templateType = response.getTemplateType().name();
        return t;
    }
    private static Collection<TemplateResponse> convertResponse(Collection<MMXTemplate> response) {

        if (response == null) {
            return null;
        }
        List<TemplateResponse> list = new ArrayList<>();
        for (MMXTemplate t : response) {
            list.add(convertResponse(t));
        }
        return list;
    }

    // REQUEST / RESPONSE
    public static class TemplateRequest {

        String appId;
        String templateName;
        String template;

        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
        }
        public String getTemplateName() {
            return templateName;
        }
        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }
        public String getTemplate() {
            return template;
        }
        public void setTemplate(String template) {
            this.template = template;
        }
    }

    public static class TemplateResponse {

        int templateId;
        String appId;
        String templateType;
        String templateName;
        String template;

        public int getTemplateId() {
            return templateId;
        }
        public void setTemplateId(int templateId) {
            this.templateId = templateId;
        }
        public String getAppId() {
            return appId;
        }
        public void setAppId(String appId) {
            this.appId = appId;
        }
        public String getTemplateType() {
            return templateType;
        }
        public void setTemplateType(String templateType) {
            this.templateType = templateType;
        }
        public String getTemplateName() {
            return templateName;
        }
        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }
        public String getTemplate() {
            return template;
        }
        public void setTemplate(String template) {
            this.template = template;
        }
    }

    public static class TemplateVerifyRequest {

      public static class MockPushConfig {
        private final boolean mSilentPush;
        private final Map<String, String> mMeta;

        public MockPushConfig(boolean silentPush, Map<String, String> meta) {
          mSilentPush = silentPush;
          mMeta = meta;
        }

        public boolean isSilentPush() {
          return mSilentPush;
        }

        public Map<String, String> getMeta() {
          return mMeta;
        }
      }

      public MockPushConfig config;
      public NameDesc application;
      public NameDesc channel;
      public MsgData msg;
    }

    public static class TemplateVerifyResponse {
      private boolean success;
      private String result;

      public void setSuccessResult(String result) {
        this.success = true;
        this.result = result;
      }

      public void setErrorResult(String result) {
        this.success = false;
        this.result = result;
      }

      public boolean isSuccess() {
        return this.success;
      }

      public String getResult() {
        return this.result;
      }
    }
}
