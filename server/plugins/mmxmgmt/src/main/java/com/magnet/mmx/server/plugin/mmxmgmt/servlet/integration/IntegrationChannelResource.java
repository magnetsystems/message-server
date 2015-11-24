/*   Copyright (c) 2015 Magnet Systems, Inc.
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

import com.google.common.base.Strings;
import com.magnet.mmx.protocol.*;
import com.magnet.mmx.server.api.v1.RestUtils;
import com.magnet.mmx.server.api.v1.protocol.ChannelCreateInfo;
import com.magnet.mmx.server.plugin.mmxmgmt.MMXException;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorCode;
import com.magnet.mmx.server.plugin.mmxmgmt.api.ErrorResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.SendMessageResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.api.SentMessageId;
import com.magnet.mmx.server.plugin.mmxmgmt.api.push.Count;
import com.magnet.mmx.server.plugin.mmxmgmt.handler.MMXChannelManager;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSender;
import com.magnet.mmx.server.plugin.mmxmgmt.message.MessageSenderImpl;
import com.magnet.mmx.server.plugin.mmxmgmt.message.TopicPostResult;
import com.magnet.mmx.server.plugin.mmxmgmt.servlet.TopicPostResponse;
import com.magnet.mmx.server.plugin.mmxmgmt.topic.TopicPostMessageRequest;
import com.magnet.mmx.server.plugin.mmxmgmt.util.JIDUtil;
import com.magnet.mmx.server.plugin.mmxmgmt.util.MMXServerConstants;
import com.magnet.mmx.util.ChannelHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Channel admin REST API using auth token.  Accessing this resource must be
 * restricted to MMS or admin user.
 */
@Path("/integration/channels")
public class IntegrationChannelResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationChannelResource.class);
    static final String DESCRIPTION_KEY = "description";
    static final String TAGS_KEY = "tag";
    static final String ID_KEY = "id";
    static final String CHANNEL_NAME = "channelName";
    static final String MMX_INTERNAL_SALT = "Z00N13!!";
    private static  char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    private static int HASH_MIN_KEY_SIZE = 8;


    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/create")
    public Response createChannel(@Context HttpHeaders headers,
                                      CreateChannelRequest channelInfo) {

        ErrorResponse errorResponse = null;
        CreateChannelResponse chatChannelResponse = null;

        if (channelInfo == null) {
            chatChannelResponse = new CreateChannelResponse(ErrorCode.ILLEGAL_ARGUMENT.getCode(),"Channel information not set");
            return RestUtils.getOKJAXRSResp(chatChannelResponse);
        }

        if (!ChannelHelper.validateApplicationChannelName(channelInfo.getChannelName())) {
            chatChannelResponse = new CreateChannelResponse(ErrorCode.ILLEGAL_ARGUMENT.getCode(),
                    MMXChannelManager.StatusCode.INVALID_CHANNEL_NAME.getMessage());
            return RestUtils.getOKJAXRSResp(chatChannelResponse);
        }

        if (!Strings.isNullOrEmpty(channelInfo.getDescription())
                && channelInfo.getDescription().length() > MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN) {

            chatChannelResponse = new CreateChannelResponse(ErrorCode.ILLEGAL_ARGUMENT.getCode(),
                    "channel description too long, max length = " +
                            MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN);

            return RestUtils.getOKJAXRSResp(chatChannelResponse);
        }

        // Attempt to create or Fetch Channel
        String channelName = channelInfo.getChannelName();

        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        channelInfo.setChannelName(channelName);
        channelInfo.setPrivateChannel(channelInfo.isPrivateChannel());
        JID from = RestUtils.createJID(channelInfo.getUserId(),
                channelInfo.getMmxAppId(),
                channelInfo.getDeviceId());
        try {
            ChannelAction.CreateRequest rqt = toCreateRequest(channelInfo);
            channelManager.createChannel(from, channelInfo.getMmxAppId(), rqt);

        } catch (MMXException e) {
            if (e.getCode() == StatusCode.CONFLICT) {
                errorResponse = new ErrorResponse(ErrorCode.TOPIC_EXISTS,
                        "Channel already exists");
            } else if (e.getCode() == StatusCode.FORBIDDEN) {
                errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e
                        .getMessage());
            } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e
                        .getMessage());
            } else {
                errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e
                        .getMessage());
            }

            chatChannelResponse = new CreateChannelResponse(errorResponse.getCode(),
                    errorResponse.getMessage());
             return RestUtils.getOKJAXRSResp(chatChannelResponse);

        } catch (Throwable e) {

            chatChannelResponse = new CreateChannelResponse(ErrorCode.UNKNOWN_ERROR.getCode(),
                    e.getMessage());
            return RestUtils.getOKJAXRSResp(chatChannelResponse);


        }

        // auto subscribe recipients to this channel
        Map<String,ChannelAction.SubscribeResponse> subResponseMap =  new HashMap<String,ChannelAction.SubscribeResponse>();

        try {
            MMXChannelId channelId = nameToId(channelName,channelInfo.getUserId());
            //ChannelInfo foundChannel =  channelManager.getChannel(channelInfo.getMmxAppId(),channelId );
            //errorResponse = new ErrorResponse(ErrorCode.NO_ERROR, "Send Message to Chat Success");

            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            ChannelAction.SubscribeResponse resp = null;

            for (String subscriber : channelInfo.getSubscribers()) {
                JID sub = new JID(JIDUtil.makeNode(subscriber, channelInfo.getMmxAppId()),
                        from.getDomain(), null);
                resp = channelManager.subscribeChannel(sub, channelInfo.getMmxAppId(), rqt,
                        Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
                subResponseMap.put(subscriber,resp);

            }

        } catch (MMXException e) {
            LOGGER.error("Exception during createChannel request", e);
            chatChannelResponse = new CreateChannelResponse(ErrorCode.UNKNOWN_ERROR.getCode(),
                    e.getMessage());
            chatChannelResponse.setSubscribeResponse(subResponseMap);

            return RestUtils.getCreatedJAXRSResp(chatChannelResponse);

        }


        chatChannelResponse = new CreateChannelResponse(ErrorCode.NO_ERROR.getCode(),
                "Channel created");
        chatChannelResponse.setSubscribeResponse(subResponseMap);

        return RestUtils.getCreatedJAXRSResp(chatChannelResponse);



    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/chat/publish/")
    public Response publishToChannel(@Context HttpHeaders headers,
                                  PublishMessageToChatRequest channelInfo) {

        ErrorResponse errorResponse = null;

        if (channelInfo == null) {
            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                    "Channel information not set");
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }

//        if (!ChannelHelper.validateApplicationChannelName(channelInfo.getChannelName())) {
//            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
//                    MMXChannelManager.StatusCode.INVALID_CHANNEL_NAME.getMessage());
//            return RestUtils.getBadReqJAXRSResp(errorResponse);
//        }

        if (!Strings.isNullOrEmpty(channelInfo.getDescription())
                && channelInfo.getDescription().length() > MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN) {
            errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT,
                    "channel description too long, max length = " +
                            MMXServerConstants.MAX_TOPIC_DESCRIPTION_LEN);
            return RestUtils.getBadReqJAXRSResp(errorResponse);
        }

        // Attempt to create or Fetch Channel
        String channelHash = generateHash(channelInfo);
        String channelName = constructChannelName(channelInfo.getChannelName(), channelHash);
        MMXChannelManager channelManager = MMXChannelManager.getInstance();
        channelInfo.setChannelName(channelName);
        channelInfo.setPrivateChannel(true);
        JID from = RestUtils.createJID(channelInfo.getUserId(), channelInfo.getMmxAppId(), channelInfo.getDeviceId());
        try {
            ChannelAction.CreateRequest rqt = toCreateRequest(channelInfo);
            channelManager.createChannel(from, channelInfo.getMmxAppId(), rqt);

        } catch (MMXException e) {
            if (e.getCode() != StatusCode.CONFLICT) {
                if (e.getCode() == StatusCode.FORBIDDEN) {
                    errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e
                            .getMessage());
                } else if (e.getCode() == StatusCode.BAD_REQUEST) {
                    errorResponse = new ErrorResponse(ErrorCode.ILLEGAL_ARGUMENT, e
                            .getMessage());
                } else {
                    errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e
                            .getMessage());
                    return RestUtils.getInternalErrorJAXRSResp(errorResponse);
                }
            }

        } catch (Throwable e) {
            LOGGER.error("Throwable during publishToChatChannel request", e);
            errorResponse = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
            return RestUtils.getInternalErrorJAXRSResp(errorResponse);
        }

        List <SentMessageId> sentList =  new ArrayList<SentMessageId>();
        // auto subscribe recipients to this channel
        try {
            MMXChannelId channelId = nameToId(channelName,channelInfo.getUserId());
            ChannelInfo foundChannel =  channelManager.getChannel(channelInfo.getMmxAppId(),channelId );
            errorResponse = new ErrorResponse(ErrorCode.NO_ERROR, "Send Message to Chat Success");

            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
                    channelId.getEscUserId(), channelId.getName(), null);
            ChannelAction.SubscribeResponse resp = null;

            for (String subscriber : channelInfo.getRecipients()) {
                JID sub = new JID(JIDUtil.makeNode(subscriber, channelInfo.getMmxAppId()),
                        from.getDomain(), null);
                resp = channelManager.subscribeChannel(sub, channelInfo.getMmxAppId(), rqt,
                        Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
                sentList.add(new SentMessageId(subscriber,"",null));

            }

        } catch (MMXException e) {
            // e.printStackTrace();
            //ignore this exception?
        }

        // send Message
        try {
            MessageSender sender = new MessageSenderImpl();

            TopicPostMessageRequest tpm = new TopicPostMessageRequest();
            tpm.setContent(channelInfo.getContent());
            tpm.setContentType(channelInfo.getContentType());
            tpm.setMessageType(channelInfo.getMessageType());

            TopicPostResult result = sender.postMessage(channelInfo.getUserId(), channelInfo.getUserId() + "#" + channelName,
                    channelInfo.getMmxAppId(), tpm);

            TopicPostResponse sendResponse = new TopicPostResponse();
            if (result.isError()) {
                LOGGER.info("Problem posting message:" + result.getErrorMessage());
                sendResponse.setMessage(result.getErrorMessage());
                sendResponse.setStatus(result.getStatus());
                throw new WebApplicationException(
                        Response
                                .status(
                                        result.getErrorCode() == ErrorCode.TOPIC_PUBLISH_FORBIDDEN
                                                .getCode() ?
                                                Response.Status.FORBIDDEN : Response.Status.BAD_REQUEST)
                                .entity(sendResponse)
                                .build());
            } else {
                sendResponse.setMessageId(result.getMessageId());
                sendResponse.setMessage("Successfully published message");
                sendResponse.setStatus(result.getStatus());
                for(SentMessageId messageId :sentList) {
                    messageId.setMessageId(result.getMessageId());
                }
            }

            SendMessageResponse messageResponse = new SendMessageResponse();
            messageResponse.setCount(new Count(channelInfo.getRecipients().length,sentList.size(),0));
            messageResponse.setSentList(sentList);
            return RestUtils.getOKJAXRSResp(messageResponse);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Throwable t) {
            LOGGER.warn("Throwable during processing request", t);
            ErrorResponse error = new ErrorResponse(ErrorCode.UNKNOWN_ERROR,
                    "Error processing request");
            return RestUtils.getInternalErrorJAXRSResp(error);
        }

    }




//    /**
//     * Add subscribers to a public or private channel by the admin.  The
//     * <code>channelName</code> must be in the form of "user-id#channel-name"
//     * (e.g. "38473af95e116c0#myblogs") for private channel, or without a hash
//     * sign as "channel-name" (e.g. "company-announcement") for public channel.
//     * @param headers
//     * @param channelName Private or public channel name.
//     * @param subscribers A list of subscribers.
//     * @return
//     */
//    @PUT
//    @Path("{" + CHANNEL_NAME + "}/subscribers")
//    @Produces(MediaType.APPLICATION_JSON)
//    @Consumes(MediaType.APPLICATION_JSON)
//    public Response addSubscribers(@Context HttpHeaders headers,
//                                    @PathParam(CHANNEL_NAME) String channelName,
//                                    List<MMXid> subscribers) {
//        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
//        if (tokenInfo == null) {
//            return RestUtils.getUnauthJAXRSResp();
//        }
//
//        JID admin = RestUtils.createJID(tokenInfo);
//        String domain = admin.getDomain();
//        String appId = tokenInfo.getMmxAppId();
//        MMXChannelId channelId = nameToId(channelName);
//
//        MMXChannelManager channelManager = MMXChannelManager.getInstance();
//        try {
//            ChannelAction.SubscribeRequest rqt = new ChannelAction.SubscribeRequest(
//                    channelId.getEscUserId(), channelId.getName(), null);
//            ChannelAction.SubscribeResponse resp = null;
//            for (MMXid subscriber : subscribers) {
//              JID sub = new JID(JIDUtil.makeNode(subscriber.getUserId(), appId),
//                    domain, subscriber.getDeviceId());
//              resp = channelManager.subscribeChannel(sub, appId, rqt,
//                  Arrays.asList(MMXServerConstants.TOPIC_ROLE_PUBLIC));
//            }
//            return RestUtils.getOKJAXRSResp(resp);
//        } catch (MMXException e) {
//            Response.Status status;
//            ErrorResponse response;
//            if (e.getCode() == StatusCode.NOT_FOUND) {
//                status = Response.Status.NOT_FOUND;
//                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST,
//                        "Channel does not exist: " + channelName);
//            } else if (e.getCode() == StatusCode.FORBIDDEN) {
//                status = Response.Status.FORBIDDEN;
//                response = new ErrorResponse(ErrorCode.TOPIC_SUBSCRIBE_FORBIDDEN,
//                        "Channel cannot be subscribed: " + channelName);
//            } else if (e.getCode() == StatusCode.CONFLICT) {
//                status = Response.Status.CONFLICT;
//                response = new ErrorResponse(ErrorCode.TOPIC_ALREADY_SUBSCRIBED,
//                        "Channel is already subscribed: " + channelName);
//            } else {
//                status = Response.Status.INTERNAL_SERVER_ERROR;
//                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
//            }
//            return RestUtils.getJAXRSResp(status, response);
//        }
//    }
//
//    /**
//     * Delete subscribers from a public or private channel by the admin.  The
//     * <code>channelName</code> must be in the form of "user-id#channel-name"
//     * (e.g. "38473af95e116c0#myblogs") for private channel, or without a hash
//     * sign as "channel-name" (e.g. "company-announcement") for public channel.
//     * @param headers
//     * @param channelName Private or public channel name.
//     * @param subscribers A list of subscribers.
//     * @return
//     */
//    @DELETE
//    @Path("{" + CHANNEL_NAME + "}/subscribers")
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    public Response deleteSubscribers(@Context HttpHeaders headers,
//                                       @PathParam(CHANNEL_NAME) String channelName,
//                                       List<MMXid> subscribers) {
//        TokenInfo tokenInfo = RestUtils.getAuthTokenInfo(headers);
//        if (tokenInfo == null) {
//            return RestUtils.getUnauthJAXRSResp();
//        }
//
//        JID admin = RestUtils.createJID(tokenInfo);
//        String domain = admin.getDomain();
//        String appId = tokenInfo.getMmxAppId();
//        MMXChannelId channelId = nameToId(channelName);
//
//        MMXChannelManager channelManager = MMXChannelManager.getInstance();
//        try {
//            ChannelAction.UnsubscribeRequest rqt = new ChannelAction.UnsubscribeRequest(
//                    channelId.getEscUserId(), channelId.getName(), null);
//            MMXStatus resp = null;
//            for (MMXid subscriber : subscribers) {
//              JID sub = new JID(JIDUtil.makeNode(subscriber.getUserId(), appId),
//                    domain, subscriber.getDeviceId());
//              resp = channelManager.unsubscribeChannel(sub, appId, rqt);
//            }
//            return RestUtils.getOKJAXRSResp(resp);
//        } catch (MMXException e) {
//            Response.Status status;
//            ErrorResponse response;
//            if (e.getCode() == StatusCode.NOT_FOUND) {
//                status = Response.Status.NOT_FOUND;
//                response = new ErrorResponse(ErrorCode.TOPIC_NOT_EXIST,
//                        "Channel does not exist: " + channelName);
//            } else if (e.getCode() == StatusCode.GONE) {
//                status = Response.Status.GONE;
//                response = new ErrorResponse(ErrorCode.TOPIC_NOT_SUBSCRIBED,
//                        "Channel is not subscribed: " + channelName);
//            } else {
//                status = Response.Status.INTERNAL_SERVER_ERROR;
//                response = new ErrorResponse(ErrorCode.UNKNOWN_ERROR, e.getMessage());
//            }
//            return RestUtils.getJAXRSResp(status, response);
//        }
//    }

    // The hack to fix MOB-2516 that allows the console to display user channels as
    // userID#channelName. This method parses the global channel or user channel
    // properly.
    public static MMXChannelId nameToId(String channelName,String userId) {
        int index = channelName.indexOf(ChannelHelper.CHANNEL_SEPARATOR);
        if (index < 0) {
            return new MMXChannelId(userId,channelName);
        } else {
            return new MMXChannelId(channelName.substring(0, index), channelName
                    .substring(index + 1));
        }
    }

    // The hack to fix MOB-2516 to convert a user channel to userID#channelName
    public static String idToName(String userId, String channelName) {

        if (userId == null) {
            return channelName;
        } else {
            return userId + ChannelHelper.CHANNEL_SEPARATOR + channelName;
        }
    }


    private ChannelAction.CreateRequest toCreateRequest(ChannelCreateInfo createInfo) {
        MMXTopicOptions options = new MMXTopicOptions()
                .setDescription(createInfo.getDescription())
                .setMaxItems(createInfo.getMaxItems())
                .setSubscribeOnCreate(createInfo.isSubscribeOnCreate())
                .setSubscriptionEnabled(createInfo.isSubscriptionEnabled())
                .setPublisherType(createInfo.getPublishPermission());
        ChannelAction.CreateRequest rqt = new ChannelAction.CreateRequest(createInfo.getChannelName(),
                createInfo.isPrivateChannel(), options);
        return rqt;
    }



    private static String generateHash(PublishMessageToChatRequest request) {
        List<String> recipients = new ArrayList<String>();
        recipients.addAll(Arrays.asList(request.getRecipients()));
        recipients.add(request.getUserId());

        if(recipients!=null && recipients.size()>0) {
            //order recipients
            Collections.sort(recipients);
            String signature = StringUtils.join(recipients, ",");
            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
                md5.update((MMX_INTERNAL_SALT + signature).getBytes());
                byte byteData[] = md5.digest();

                String channelHash = encode2Hex(byteData);
                return channelHash;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
       }
       return  null;

    }


    private static String constructChannelName(String channelName,String hash) {
        //return "chat/"+ hash;
        return hash;
    }


    public static String encode2Hex(byte [] bytes) {

        //shouldnt be using this to hexify a huge binary array
        if(bytes==null || bytes.length<1 || bytes.length>(Integer.MAX_VALUE/2))
            return null;

        char [] hex = new char[bytes.length*2];
        for (int indx=0;indx<bytes.length;indx++) {
            byte lsb = (byte) (bytes[indx] & 0x0f);
            byte msb = (byte) ((bytes[indx] & 0xf0)>>4);
            hex [ indx * 2 ] = hexArray[msb];
            hex [ indx * 2 + 1] = hexArray[lsb];
        }
        return new String(hex);
    }


}