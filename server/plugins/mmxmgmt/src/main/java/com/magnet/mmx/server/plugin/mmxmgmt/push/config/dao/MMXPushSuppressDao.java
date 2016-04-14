package com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao;

import com.magnet.mmx.server.plugin.mmxmgmt.push.config.dao.model.MMXPushSuppressDo;

import java.util.Collection;

/**
 * Created by mmicevic on 3/31/16.
 *
 */
public interface MMXPushSuppressDao {

    public MMXPushSuppressDo createSuppress(MMXPushSuppressDo suppress);
    public MMXPushSuppressDo getSuppress(int suppressId);
    public Collection<MMXPushSuppressDo> getSuppress(String appId, String userId);
    public void deleteSuppress(MMXPushSuppressDo suppress);
}
