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
package com.magnet.yak.functional;

import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.protocol.DevTags;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.PublisherType;
import com.magnet.mmx.protocol.UserTags;

public class TagTests {
	private static final String TOPIC = "coocoocoo";
	private static final Logger LOGGER = LoggerFactory.getLogger(TagTests.class);
	public void testTopicTags(MMXClient client1, MMXClient client2) throws Exception {
		MMXTopicOptions o = new MMXTopicOptions();
		o.setPublisherType(PublisherType.anyone);
		o.setMaxItems(50000);
		MMXGlobalTopic topic = new MMXGlobalTopic(TOPIC);
		try {
			client1.getPubSubManager().createTopic(topic, o);
		} catch (Exception e) {
			LOGGER.error("testTopicTags : caught exception creating topic", e);
		}
		client1.getPubSubManager().addTags(topic, Arrays.asList("tag1", "tag2", "tag3"));
		TopicAction.TopicTags tags = client1.getPubSubManager().getAllTags(topic);		
		LOGGER.trace("call getAllTags = {}",tags.getTags());
		client1.getPubSubManager().setAllTags(topic, new ArrayList<String>());
		tags = client1.getPubSubManager().getAllTags(topic);
		LOGGER.trace("call getAllTags = {}",tags.getTags());
		client1.getPubSubManager().addTags(topic, Arrays.asList("tag4", "tag5", "tag6"));
		client1.getPubSubManager().removeTags(topic, Arrays.asList("tag6"));
		tags = client1.getPubSubManager().getAllTags(topic);
		LOGGER.trace("call getAllTags = {}",tags.getTags());
	}
	
	public void testUserTags(MMXClient client) throws Exception {
		LOGGER.trace("testUserTags : {}", client);
		client.getAccountManager().addTags(Arrays.asList("tag1", "tag2", "tag3"));
		UserTags tags = client.getAccountManager().getAllTags();
		LOGGER.trace("testUserTags : tags = {}", tags.getTags());
		client.getAccountManager().setAllTags(new ArrayList<String>());
		tags = client.getAccountManager().getAllTags();
		LOGGER.trace("testUserTags : tags = {}", tags.getTags());
		client.getAccountManager().addTags(Arrays.asList("tag4", "tag5", "tag6"));
		tags = client.getAccountManager().getAllTags();
		LOGGER.trace("testUserTags : tags = {}", tags.getTags());
		client.getAccountManager().removeTags(Arrays.asList("tag6"));
		tags = client.getAccountManager().getAllTags();
		LOGGER.trace("testUserTags : tags = {}", tags.getTags());
	}
	
	public void testDeviceTags(MMXClient client) throws Exception {
		LOGGER.trace("testDeviceTags : {}", client);
		client.getDeviceManager().addTags(Arrays.asList("devtag1", "devtag2", "devtag3"));
		DevTags tags = client.getDeviceManager().getAllTags();
		LOGGER.trace("testDeviceTags : tags = {}", tags.getTags());
		client.getAccountManager().setAllTags(new ArrayList<String>());
		tags = client.getDeviceManager().getAllTags();
		LOGGER.trace("testDeviceTags : tags = {}", tags.getTags());
		client.getAccountManager().addTags(Arrays.asList("devtag4", "devtag5", "devtag6"));
		tags = client.getDeviceManager().getAllTags();
		LOGGER.trace("testDeviceTags : tags = {}", tags.getTags());
		client.getAccountManager().removeTags(Arrays.asList("devtag6"));
		tags = client.getDeviceManager().getAllTags();
		LOGGER.trace("testDeviceTags : tags = {}", tags.getTags());
		
	}
}
