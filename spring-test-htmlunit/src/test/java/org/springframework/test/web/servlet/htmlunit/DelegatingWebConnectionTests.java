/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.springframework.test.web.servlet.htmlunit;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.matchers.UrlRegexRequestMatcher;
import org.springframework.test.web.servlet.htmlunit.matchers.WebRequestMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URL;
import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection.DelegateWebConnection;

/**
 * @author Rob Winch
 */
@RunWith(MockitoJUnitRunner.class)
public class DelegatingWebConnectionTests {
	private DelegatingWebConnection webConnection;

	@Mock
	private WebRequestMatcher matcher1;
	@Mock
	private WebRequestMatcher matcher2;
	@Mock
	private WebConnection defaultConnection;
	@Mock
	private WebConnection connection1;
	@Mock
	private WebConnection connection2;

	private WebRequest request;
	private WebResponse expectedResponse;

	@Before
	public void setUp() throws Exception {
		request = new WebRequest(new URL("http://localhost/"));
		WebResponseData data = new WebResponseData("".getBytes("UTF-8"),200, "", Collections.<NameValuePair>emptyList());
		expectedResponse = new WebResponse(data, request, 100L);
		webConnection = new DelegatingWebConnection(defaultConnection, new DelegateWebConnection(matcher1,connection1), new DelegateWebConnection(matcher2,connection2));
	}

	@Test
	public void getResponseDefault() throws Exception {
		when(defaultConnection.getResponse(request)).thenReturn(expectedResponse);

		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verify(matcher2).matches(request);
		verifyNoMoreInteractions(connection1,connection2);
		verify(defaultConnection).getResponse(request);
	}

	@Test
	public void getResponseAllMatches() throws Exception {
		when(matcher1.matches(request)).thenReturn(true);
		when(matcher2.matches(request)).thenReturn(true);
		when(connection1.getResponse(request)).thenReturn(expectedResponse);

		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verifyNoMoreInteractions(matcher2,connection2,defaultConnection);
		verify(connection1).getResponse(request);
	}

	@Test
	public void getResponseSecondMatches() throws Exception {
		when(matcher2.matches(request)).thenReturn(true);
		when(connection2.getResponse(request)).thenReturn(expectedResponse);

		WebResponse response = webConnection.getResponse(request);

		assertThat(response).isSameAs(expectedResponse);
		verify(matcher1).matches(request);
		verify(matcher2).matches(request);
		verifyNoMoreInteractions(connection1,defaultConnection);
		verify(connection2).getResponse(request);
	}

	@Test
	public void classlevelJavadoc() throws Exception {
		WebClient webClient = new WebClient();

		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(TestController.class).build();
		MockMvcWebConnection mockConnection = new MockMvcWebConnection(mockMvc);

		WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
		WebConnection httpConnection = new HttpWebConnection(webClient);
		WebConnection webConnection = new DelegatingWebConnection(mockConnection, new DelegateWebConnection(cdnMatcher, httpConnection));

		webClient.setWebConnection(webConnection);

		Page page = webClient.getPage("http://code.jquery.com/jquery-1.11.0.min.js");
		assertThat(page.getWebResponse().getStatusCode()).isEqualTo(200);
		assertThat(page.getWebResponse().getContentAsString()).isNotEmpty();
	}

	@Controller
	static class TestController {}
}