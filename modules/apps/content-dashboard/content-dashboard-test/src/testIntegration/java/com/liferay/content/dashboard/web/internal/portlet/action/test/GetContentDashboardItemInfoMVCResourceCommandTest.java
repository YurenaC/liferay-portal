/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.content.dashboard.web.internal.portlet.action.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.asset.entry.rel.service.AssetEntryAssetCategoryRelLocalService;
import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.model.AssetCategoryConstants;
import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetTag;
import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.model.AssetVocabularyConstants;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.asset.test.util.AssetTestUtil;
import com.liferay.content.dashboard.item.ContentDashboardItem;
import com.liferay.content.dashboard.item.ContentDashboardItemFactory;
import com.liferay.content.dashboard.item.action.ContentDashboardItemAction;
import com.liferay.content.dashboard.item.type.ContentDashboardItemSubtype;
import com.liferay.content.dashboard.item.type.ContentDashboardItemSubtypeFactory;
import com.liferay.content.dashboard.web.internal.item.ContentDashboardItemFactoryRegistry;
import com.liferay.content.dashboard.web.internal.portlet.action.GetContentDashboardItemInfoMVCResourceCommand;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppLocalServiceUtil;
import com.liferay.info.item.InfoItemReference;
import com.liferay.journal.constants.JournalArticleConstants;
import com.liferay.journal.constants.JournalFolderConstants;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.test.util.JournalTestUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.json.JSONFactoryImpl;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.test.portlet.MockLiferayPortletRenderRequest;
import com.liferay.portal.kernel.test.portlet.MockLiferayPortletRenderResponse;
import com.liferay.portal.kernel.test.portlet.MockLiferayResourceRequest;
import com.liferay.portal.kernel.test.portlet.MockLiferayResourceResponse;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.language.LanguageImpl;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.liferay.portal.util.PortalImpl;
import com.liferay.portlet.asset.model.impl.AssetVocabularyImpl;
import com.liferay.portlet.asset.service.impl.AssetVocabularyLocalServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * @author Yurena Cabrera
 */
@RunWith(Arquillian.class)
public class GetContentDashboardItemInfoMVCResourceCommandTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
		_company = _companyLocalService.getCompany(_group.getCompanyId());

		_serviceContext = ServiceContextTestUtil.getServiceContext(
			_group.getGroupId());
	}

	@Test
	public void testServeResourceMio() throws Exception {
		User user = TestPropsValues.getUser();
		user.setPortraitId(12345L);

		MockLiferayResourceRequest mockLiferayResourceRequest =
			new MockLiferayResourceRequest();

		ThemeDisplay themeDisplay = new ThemeDisplay();

		themeDisplay.setUser(TestPropsValues.getUser());
		themeDisplay.setCompany(_company);

		mockLiferayResourceRequest.setAttribute(
			WebKeys.THEME_DISPLAY, themeDisplay);

		MockLiferayResourceResponse mockLiferayResourceResponse =
			new MockLiferayResourceResponse();

		ContentDashboardItem contentDashboardItem =
			_createContentDashboardFileItem();

		_initGetContentDashboardItemInfoMVCResourceCommand(contentDashboardItem);

		InfoItemReference infoItemReference =
			contentDashboardItem.getInfoItemReference();

		mockLiferayResourceRequest.setParameter("className",
			infoItemReference.getClassName());
		mockLiferayResourceRequest.addParameter(
			"classPK", String.valueOf(infoItemReference.getClassPK()));


		_mvcResourceCommand.serveResource(
			mockLiferayResourceRequest, mockLiferayResourceResponse);

		ByteArrayOutputStream byteArrayOutputStream =
			(ByteArrayOutputStream)
				mockLiferayResourceResponse.getPortletOutputStream();

		JSONObject jsonObject = JSONFactoryUtil.createJSONObject(
			new String(byteArrayOutputStream.toByteArray()));

		JSONObject vocabulariesJSONObject = jsonObject.getJSONObject(
			"vocabularies");

//		List<AssetCategory> assetCategories =
//			contentDashboardItem.getAssetCategories();
//
//		for (AssetCategory assetCategory : assetCategories) {
//			JSONObject vocabularyDataJSONObject =
//				vocabulariesJSONObject.getJSONObject(
//					String.valueOf(assetCategory.getVocabularyId()));
//
//			JSONArray categoriesJSONArray =
//				vocabularyDataJSONObject.getJSONArray("categories");
//
//			Assert.assertEquals(1, categoriesJSONArray.length());
//
//			Assert.assertEquals(
//				assetCategory.getTitle(LocaleUtil.getSiteDefault()),
//				categoriesJSONArray.getString(0));
//		}
//
//		InfoItemReference infoItemReference =
//			contentDashboardItem.getInfoItemReference();
//
//		Assert.assertEquals(
//			infoItemReference.getClassName(),
//			jsonObject.getString("className"));
//		Assert.assertEquals(
//			infoItemReference.getClassPK(), jsonObject.getLong("classPK"), 0);
//
//		Assert.assertEquals(
//			contentDashboardItem.getDescription(LocaleUtil.US),
//			jsonObject.getString("description"));
//		Assert.assertNotNull(jsonObject.getString("fetchSharingButtonURL"));
//		Assert.assertNotNull(
//			jsonObject.getString("fetchSharingCollaboratorsURL"));
//
//		Assert.assertNotNull(jsonObject.getString("preview"));
//
//		JSONObject previewJSONObject = jsonObject.getJSONObject("preview");
//
//		Assert.assertEquals(
//			"http://www.preview.com/imageURL",
//			previewJSONObject.getString("imageURL"));
//		Assert.assertEquals(
//			"http://www.viewURL.url.com/viewURL",
//			previewJSONObject.getString("url"));
//
//		JSONArray tagsJSONArray = jsonObject.getJSONArray("tags");
//
//		Assert.assertEquals(
//			JSONUtil.putAll(
//				ListUtil.toArray(
//					contentDashboardItem.getAssetTags(), AssetTag.NAME_ACCESSOR)
//			).toString(),
//			tagsJSONArray.toString());
//
//		Assert.assertEquals(
//			contentDashboardItem.getTitle(LocaleUtil.US),
//			jsonObject.getString("title"));
//
//		ContentDashboardItemSubtype contentDashboardItemSubtype =
//			contentDashboardItem.getContentDashboardItemSubtype();
//
//		Assert.assertEquals(
//			contentDashboardItemSubtype.getLabel(LocaleUtil.US),
//			jsonObject.getString("subType"));
//
//		List<ContentDashboardItem.SpecificInformation<?>>
//			specificInformationList =
//			contentDashboardItem.getSpecificInformationList(LocaleUtil.US);
//
//		Assert.assertEquals(
//			String.valueOf(specificInformationList), 2,
//			specificInformationList.size());
//
//		JSONObject specificFieldsJSONObject = jsonObject.getJSONObject(
//			"specificFields");
//
//		for (ContentDashboardItem.SpecificInformation<?> specificInformation :
//			specificInformationList) {
//
//			JSONObject specificFieldJSONObject =
//				specificFieldsJSONObject.getJSONObject(
//					specificInformation.getKey());
//
//			Assert.assertEquals(
//				specificFieldJSONObject.getString("title"),
//				specificInformation.getKey());
//			Assert.assertEquals(
//				specificFieldJSONObject.getString("value"),
//				specificInformation.getValue());
//		}
//
//		JSONObject userJSONObject = jsonObject.getJSONObject("user");
//
//		Assert.assertEquals(
//			contentDashboardItem.getUserName(),
//			userJSONObject.getString("name"));
//		Assert.assertEquals(
//			contentDashboardItem.getUserId(), userJSONObject.getLong("userId"));
//		Assert.assertEquals("portraitURL", userJSONObject.getString("url"));
//
//		_assertContentDashboardItemLatestVersions(
//			contentDashboardItem, jsonObject);
	}

	private ContentDashboardItem _createContentDashboardFileItem()
		throws PortalException {
		FileEntry fileEntry = DLAppLocalServiceUtil.addFileEntry(
			"Site", TestPropsValues.getUserId(), _group.getGroupId(),
			DLFolderConstants.DEFAULT_PARENT_FOLDER_ID, "fileName.pdf",
			"application/pdf", new byte[0], new Date(150000), new Date(150000),
			_serviceContext);

		return _contentDashboardFileItemFactory.create(
			fileEntry.getPrimaryKey());
	}

	@Test
	public void testGetFileSpecificFields() throws Exception {
		ContentDashboardItem contentDashboardItem =
			_createContentDashboardFileItem();

		JSONObject jsonObject = ReflectionTestUtil.invoke(
			_mvcResourceCommand, "_getSpecificFieldsJSONObject",
			new Class<?>[] {ContentDashboardItem.class, Locale.class},
			contentDashboardItem, LocaleUtil.SPAIN);

		Assert.assertNotNull(jsonObject);
		Assert.assertNotNull(jsonObject.getString("extension"));
		Assert.assertNotNull(jsonObject.getString("size"));
		Assert.assertNotNull(jsonObject.getString("file-name"));
	}

	@Test
	public void testGetJournalArticleSpecificFields() throws Exception {
		JournalArticle journalArticle = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			JournalArticleConstants.CLASS_NAME_ID_DEFAULT, StringPool.BLANK,
			true,
			HashMapBuilder.put(
				LocaleUtil.SPAIN, RandomTestUtil.randomString()
			).put(
				LocaleUtil.US, RandomTestUtil.randomString()
			).build(),
			new HashMap<>(),
			HashMapBuilder.put(
				LocaleUtil.SPAIN, RandomTestUtil.randomString()
			).put(
				LocaleUtil.US, RandomTestUtil.randomString()
			).build(),
			null, LocaleUtil.getSiteDefault(), null, false, false,
			_serviceContext);

		ContentDashboardItem contentDashboardItem =
			_contentDashboardJournalItemFactory.create(
				journalArticle.getResourcePrimKey());

		JSONObject jsonObject = ReflectionTestUtil.invoke(
			_mvcResourceCommand, "_getSpecificFieldsJSONObject",
			new Class<?>[] {ContentDashboardItem.class, Locale.class},
			contentDashboardItem, LocaleUtil.SPAIN);

		Assert.assertNotNull(jsonObject);
		Assert.assertNotNull(jsonObject.getString("review-date"));
		Assert.assertNotNull(jsonObject.getString("display-date"));
		Assert.assertNotNull(jsonObject.getString("expiration-date"));
	}

	private void _initGetContentDashboardItemInfoMVCResourceCommand(ContentDashboardItem<?> contentDashboardItem)
		throws Exception {

		ReflectionTestUtil.setFieldValue(
			_mvcResourceCommand, "_jsonFactory",
			new JSONFactoryImpl());

		AssetVocabulary assetVocabulary = _addAssetVocabulary(
			AssetVocabularyConstants.VISIBILITY_TYPE_PUBLIC);

		AssetCategory assetCategory = _addAssetCategory(assetVocabulary);

		AssetEntry assetEntry = AssetTestUtil.addAssetEntry(
			_group.getGroupId());

		_assetEntryAssetCategoryRelLocalService.addAssetEntryAssetCategoryRel(
			assetEntry.getEntryId(), assetCategory.getCategoryId());

		ReflectionTestUtil.setFieldValue(
			_mvcResourceCommand, "_language",
			new LanguageImpl());
		ReflectionTestUtil.setFieldValue(
			_mvcResourceCommand, "_portal",
			new PortalImpl());

	}

	private AssetVocabulary _addAssetVocabulary(int visibilityTypePublic)
		throws Exception {

		return _assetVocabularyLocalService.addVocabulary(
			TestPropsValues.getUserId(), _group.getGroupId(), null,
			HashMapBuilder.put(
				LocaleUtil.US, RandomTestUtil.randomString()
			).build(),
			null, null, visibilityTypePublic, new ServiceContext());
	}

	private AssetCategory _addAssetCategory(AssetVocabulary assetVocabulary)
		throws Exception {

		return _assetCategoryLocalService.addCategory(
			null, TestPropsValues.getUserId(), _group.getGroupId(),
			AssetCategoryConstants.DEFAULT_PARENT_CATEGORY_ID,
			HashMapBuilder.put(
				LocaleUtil.US, RandomTestUtil.randomString()
			).build(),
			null, assetVocabulary.getVocabularyId(), null,
			new ServiceContext());
	}

	@Inject(
		filter = "component.name=com.liferay.content.dashboard.document.library.internal.item.FileEntryContentDashboardItemFactory"
	)
	private ContentDashboardItemFactory _contentDashboardFileItemFactory;

	@Inject(
		filter = "component.name=com.liferay.content.dashboard.journal.internal.item.JournalArticleContentDashboardItemFactory"
	)
	private ContentDashboardItemFactory _contentDashboardJournalItemFactory;

	@DeleteAfterTestRun
	private Group _group;

	@Inject(
		filter = "mvc.command.name=/content_dashboard/get_content_dashboard_item_info"
	)
	private MVCResourceCommand _mvcResourceCommand;

	private ServiceContext _serviceContext;

	private Company _company;

	@Inject
	private CompanyLocalService _companyLocalService;

	@Inject
	private AssetCategoryLocalService _assetCategoryLocalService;

	@Inject
	private AssetVocabularyLocalService _assetVocabularyLocalService;

	@Inject
	private AssetEntryAssetCategoryRelLocalService
		_assetEntryAssetCategoryRelLocalService;
}