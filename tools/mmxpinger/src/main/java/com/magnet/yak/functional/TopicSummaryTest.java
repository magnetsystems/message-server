/* Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.List;

import org.apache.commons.lang3.RandomUtils;

import com.magnet.mmx.client.MMXClient;
import com.magnet.mmx.client.common.MMXGlobalTopic;
import com.magnet.mmx.client.common.MMXPayload;
import com.magnet.mmx.protocol.MMXTopic;
import com.magnet.mmx.protocol.MMXTopicOptions;
import com.magnet.mmx.protocol.TopicAction;
import com.magnet.mmx.protocol.TopicAction.PublisherType;

public class TopicSummaryTest<TopicOptions> {
	public void publishItems(List<MMXClient> clients) throws Exception {
		for(MMXClient client : clients) {
			MMXGlobalTopic topic = new MMXGlobalTopic("Hello" + RandomUtils.nextInt(0, 100000));
			MMXTopicOptions options = new MMXTopicOptions();
			MMXTopic createdTopic = client.getPubSubManager().createTopic(topic, options);
			MMXPayload payload = new MMXPayload("asdasdasdasdasdas");
			client.getPubSubManager().publish(createdTopic, payload);
		}
	}
}
