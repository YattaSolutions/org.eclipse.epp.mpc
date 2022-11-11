/*******************************************************************************
 * Copyright (c) 2011, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *     Yatta Solutions - bug 432803: public API
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.ui.wizard;

import static org.junit.Assert.*;

import java.net.URL;

import org.eclipse.epp.internal.mpc.ui.CatalogRegistry;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.ui.CatalogDescriptor;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler;
import org.eclipse.epp.mpc.ui.MarketplaceUrlHandler.SolutionInstallationInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test {@link MarketplaceUrlHandler}
 *
 * @author Benjamin Muskalla
 */
public class MarketplaceUrlHandlerTest {

	private CatalogDescriptor eclipseMarketplace;

	@Before
	public void installMockMarketplace() throws Exception {
		URL url = new URL("https://marketplace.eclipse.org");
		eclipseMarketplace = new CatalogDescriptor(url, "Eclipse Marketplace");
		CatalogRegistry.getInstance().register(eclipseMarketplace);
	}

	@After
	public void uninstallMockMarketplace() throws Exception {
		CatalogRegistry.getInstance().unregister(eclipseMarketplace);
	}

	@Test
	public void testGetInstallationInfoNoValidUrl() throws Exception {
		SolutionInstallationInfo info = MarketplaceUrlHandler.createSolutionInstallInfo(null);
		assertNull(info);
		info = MarketplaceUrlHandler.createSolutionInstallInfo("");
		assertNull(info);
		info = MarketplaceUrlHandler.createSolutionInstallInfo("http://www.eclipse.org");
		assertNull(info);
		String missingInstallId = "https://marketplace.eclipse.org/mpc/install?mpc_install=";
		info = MarketplaceUrlHandler.createSolutionInstallInfo(missingInstallId);
	}

	@Test
	public void testGetInstallationInfo() throws Exception {
		String url = "https://marketplace.eclipse.org/mpc/install?x=19&y=17&mpc_state=&mpc_install=953";
		SolutionInstallationInfo info = MarketplaceUrlHandler.createSolutionInstallInfo(url);
		assertEquals("https://marketplace.eclipse.org", info.getCatalogDescriptor().getUrl().toExternalForm());
		assertEquals("953", info.getInstallId());
		assertNull(info.getState());
	}

	@Test
	public void testGetInstallationInfoWithState() throws Exception {
		String statefulUrl = "https://marketplace.eclipse.org/mpc/install?mpc_install=953";
		SolutionInstallationInfo info = MarketplaceUrlHandler.createSolutionInstallInfo(statefulUrl);
		assertEquals("https://marketplace.eclipse.org", info.getCatalogDescriptor().getUrl().toExternalForm());
		assertEquals("953", info.getInstallId());
		assertNull(info.getState());
	}

	@Test
	public void testPotentialUrls() throws Exception {
		assertFalse(MarketplaceUrlHandler.isPotentialSolution(null));
		assertFalse(MarketplaceUrlHandler.isPotentialSolution(""));
		String url = "https://marketplace.eclipse.org/mpc/install?x=19&y=17&mpc_state=&mpc_install=953";
		assertTrue(MarketplaceUrlHandler.isPotentialSolution(url));

		url = "https://marketplace.eclipse.org";
		assertFalse(MarketplaceUrlHandler.isPotentialSolution(url));
	}

	@Test
	public void testNodeUrls() throws Exception {
		final INode[] testNode = new INode[1];
		MarketplaceUrlHandler handler = new MarketplaceUrlHandler() {
			@Override
			protected boolean handleNode(CatalogDescriptor descriptor, String url, INode node) {
				testNode[0] = node;
				return true;
			}
		};

		testNode[0] = null;
		String url = "https://marketplace.eclipse.org/content/test";
		assertTrue(handler.handleUri(url));
		assertNotNull(testNode[0]);
		assertEquals(url, testNode[0].getUrl());

		testNode[0] = null;
		String nodeId = "12345";
		url = "https://marketplace.eclipse.org/node/" + nodeId;
		assertTrue(handler.handleUri(url));
		assertNotNull(testNode[0]);
		assertEquals(nodeId, testNode[0].getId());
	}

	@Test
	public void testHttpsVariants() {
		MarketplaceUrlHandler handler = new MarketplaceUrlHandler() {
			@Override
			protected boolean handleFeatured(CatalogDescriptor descriptor, String url, String category, String market) {
				assertEquals(eclipseMarketplace, descriptor);
				assertEquals("featured", url);
				return true;
			}
		};

		String url = "https://marketplace.eclipse.org/featured";
		assertTrue(handler.handleUri(url));

		url = "https://marketplace.eclipse.org/featured";
		assertTrue(handler.handleUri(url));
	}
}
