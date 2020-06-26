/*
 *  Copyright 2020 EPAM Systems
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

package com.epam.reportportal.service.analytics;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.analytics.item.AnalyticsItem;
import com.epam.reportportal.test.TestUtils;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import io.reactivex.Maybe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AnalyticsServiceTest {

	private static class TestAnalyticsService extends AnalyticsService {
		private GoogleAnalytics googleAnalytics;

		public TestAnalyticsService(ListenerParameters listenerParameters, Maybe<String> launchIdMaybe) {
			super(listenerParameters, launchIdMaybe);
		}

		public void setGoogleAnalytics(GoogleAnalytics googleAnalytics) {
			this.googleAnalytics = googleAnalytics;
		}

		@Override
		protected GoogleAnalytics getGoogleAnalytics() {
			return googleAnalytics;
		}
	}

	@Mock
	private GoogleAnalytics googleAnalytics;

	private TestAnalyticsService service;
	private ListenerParameters parameters;

	@BeforeEach
	public void setup() {
		parameters = TestUtils.standardParameters();
		service = new TestAnalyticsService(parameters, CommonUtils.createMaybe("launchId"));
		service.setGoogleAnalytics(googleAnalytics);
	}

	@Test
	public void googleAnalyticsEventTest() {
		StartLaunchRQ launchRq = TestUtils.standardLaunchRequest(parameters);
		launchRq.setAttributes(Collections.singleton(new ItemAttributesRQ("agent", "agent-java-testng|test-version-1", true)));

		service.addStartLaunchEvent(launchRq);
		service.send();
		service.close();

		ArgumentCaptor<AnalyticsItem> argumentCaptor = ArgumentCaptor.forClass(AnalyticsItem.class);
		verify(googleAnalytics, times(1)).send(argumentCaptor.capture());

		AnalyticsItem value = argumentCaptor.getValue();

		Map<String, String> params = value.getParams();

		String type = params.get("t");
		String eventAction = params.get("ea");
		String eventCategory = params.get("ec");
		String eventLabel = params.get("el");

		assertThat(type, equalTo("event"));
		assertThat(eventAction, equalTo("Start launch"));
		assertThat(eventCategory, equalTo("${name}|${version}"));
		assertThat(eventLabel, equalTo("agent-java-testng|test-version-1"));
	}

}
