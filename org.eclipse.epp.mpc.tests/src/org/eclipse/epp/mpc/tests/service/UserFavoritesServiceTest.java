/*******************************************************************************
 * Copyright (c) 2010, 2018 The Eclipse Foundation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.epp.mpc.tests.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.epp.internal.mpc.core.ServiceLocator;
import org.eclipse.epp.internal.mpc.core.service.DefaultMarketplaceService;
import org.eclipse.epp.mpc.core.model.IFavoriteList;
import org.eclipse.epp.mpc.core.model.INode;
import org.eclipse.epp.mpc.core.service.ICatalogService;
import org.eclipse.epp.mpc.core.service.IMarketplaceStorageService;
import org.eclipse.epp.mpc.core.service.IUserFavoritesService;
import org.eclipse.epp.mpc.core.service.QueryHelper;
import org.eclipse.epp.mpc.core.service.ServiceHelper;
import org.eclipse.epp.mpc.tests.Categories.RemoteTests;
import org.eclipse.epp.mpc.tests.util.TestProperties;
import org.eclipse.userstorage.IStorageService;
import org.eclipse.userstorage.internal.StorageService;
import org.eclipse.userstorage.internal.StorageServiceRegistry;
import org.eclipse.userstorage.spi.AbstractCredentialsProvider;
import org.eclipse.userstorage.spi.Credentials;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@org.junit.experimental.categories.Category(RemoteTests.class)
public class UserFavoritesServiceTest {

	private static final URI USERSTORAGE_SERVICE_URI = URI.create("https://api.eclipse.org/");

	protected IMarketplaceStorageService marketplaceStorageService;

	protected IUserFavoritesService favoritesService;

	@BeforeClass
	public static void initServices() throws CoreException {
		assureService();
		ICatalogService catalogService = ServiceHelper.getMarketplaceServiceLocator().getCatalogService();
		catalogService.listCatalogs(new NullProgressMonitor());
	}

	@Before
	public void setUp() throws Exception {
		marketplaceStorageService = ServiceHelper.getMarketplaceServiceLocator().getDefaultStorageService();
		assertNotNull(marketplaceStorageService);
		assertEquals(USERSTORAGE_SERVICE_URI, marketplaceStorageService.getServiceUri());
		marketplaceStorageService.getStorage().setCredentialsProvider(new AbstractCredentialsProvider() {

			@Override
			public Credentials provideCredentials(IStorageService service, boolean reauthentication) {
				String marketplaceUser;
				String marketplacePass;
				try {
					marketplaceUser = TestProperties.getTestProperty("mpc.storage.user");
					marketplacePass = TestProperties.getTestProperty("mpc.storage.pass");
				} catch (IOException e) {
					throw new AssertionError("Cannot load test properties", e);
				}
				Assume.assumeNotNull(marketplaceUser, marketplacePass);
				return new Credentials(marketplaceUser, marketplacePass);
			}
		});
		favoritesService = ServiceHelper.getMarketplaceServiceLocator().getDefaultFavoritesService();
		//assertNotNull(favoritesService);
		if (favoritesService == null) {
			((ServiceLocator) ServiceHelper.getMarketplaceServiceLocator()).registerFavoritesService(DefaultMarketplaceService.DEFAULT_SERVICE_LOCATION, marketplaceStorageService.getServiceUri().toString(), null);
			favoritesService = ServiceHelper.getMarketplaceServiceLocator().getDefaultFavoritesService();
		}
		assertSame(marketplaceStorageService, favoritesService.getStorageService());
	}

	private static void assureService() {
		StorageService service = StorageServiceRegistry.INSTANCE.getService(USERSTORAGE_SERVICE_URI);
		if (service == null) {
			StorageServiceRegistry.INSTANCE.addService("Staging", USERSTORAGE_SERVICE_URI);
			service = StorageServiceRegistry.INSTANCE.getService(USERSTORAGE_SERVICE_URI);
		}
		assertNotNull(service);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testToggleFavorite() throws Exception {
		INode favNode = QueryHelper.nodeById("12345");
		favoritesService.setFavorite(favNode, true, null);
		List<INode> favorites = favoritesService.getFavorites(null);
		assertNotNull(QueryHelper.findById(favorites, favNode));

		favoritesService.setFavorite(favNode, false, null);
		favorites = favoritesService.getFavorites(null);
		assertNull(QueryHelper.findById(favorites, favNode));
	}

	@Test
	public void testGetFavoriteIds() throws Exception {
		testGetFavoriteIds("creckord");
	}

	private void testGetFavoriteIds(String userId) throws IOException {
		URI favoritesUri = URI.create("https://marketplace.eclipse.org/user/" + userId + "/favorites");
		List<String> favoriteIds = favoritesService.getFavoriteIds(favoritesUri, null);
		assertNotNull(favoriteIds);
		assertFalse("Got empty favorites for " + favoritesUri, favoriteIds.isEmpty());
	}

	@Test
	public void testGetRandomFavoriteLists() throws Exception {
		List<IFavoriteList> favoriteLists = favoritesService.getRandomFavoriteLists(null);
		assertNotNull(favoriteLists);
		assertFalse(favoriteLists.isEmpty());
		assertFalse(favoriteLists.contains(null));
		for (IFavoriteList favoriteList : favoriteLists) {
			assertNotNull(favoriteList.getId());
			assertNotNull(favoriteList.getUrl());
			assertEquals(favoriteList.getId().trim(), favoriteList.getId());
			assertEquals(favoriteList.getUrl().trim(), favoriteList.getUrl());
		}
		IFavoriteList favoriteList = favoriteLists.get(0);
		testGetFavoriteIds(favoriteList.getId());
	}
}
