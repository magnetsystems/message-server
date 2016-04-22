package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushSuppressDo;

import java.util.Collection;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXPushSuppressDao {

    public void suppress(MMXPushSuppressDo suppress);
    public void unSuppress(MMXPushSuppressDo suppress);
    public MMXPushSuppressDo getSuppress(Integer suppressId);
    public Collection<MMXPushSuppressDo> getSuppress(String appId, String userId);
    public Collection<MMXPushSuppressDo> getAllSuppress(String appId);
    public MMXPushSuppressDo getSuppress(String appId, String userId, String channelId);
    public void deleteSuppress(MMXPushSuppressDo suppress);
}
