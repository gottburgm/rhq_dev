/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.enterprise.server.plugin.pc.content;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.Package;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.content.RepoManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jason Dobies
 */
public class ContentProviderManagerSyncRepoTest extends AbstractEJB3Test {

    private static final boolean TESTS_ENABLED = true;

    private TestContentServerPluginService pluginService;

    private TestContentProvider contentProvider1 = new TestContentProvider();
    private TestContentProvider contentProvider2 = new TestContentProvider();

    // The following variables need to be cleaned up at the end of the test

    private ContentSourceType contentSourceType;
    private PackageType packageType;
    private ResourceType resourceType;

    private List<ContentSource> repoContentSources = new ArrayList<ContentSource>();
    private Repo repoToSync;


    @BeforeMethod
    public void setupBeforeMethod() throws Exception {

        // Plugin service setup
        prepareScheduler();
        pluginService = new TestContentServerPluginService(this);

        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        ContentSourceManagerLocal contentManager = LookupUtil.getContentSourceManager();
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord = subjectManager.getOverlord();

        // Create a sample content source type that will be used in this test
        contentSourceType = new ContentSourceType("testType");
        entityManager.persist(contentSourceType);
        entityManager.flush();

        // A repo sync will query all providers for that repo, so add multiple providers
        ContentSource cs1 = new ContentSource("contentSource1", contentSourceType);
        ContentSource cs2 = new ContentSource("contentSource2", contentSourceType);

        cs1 = contentManager.simpleCreateContentSource(overlord, cs1);
        cs2 = contentManager.simpleCreateContentSource(overlord, cs2);

        pluginService.associateContentProvider(cs1, contentProvider1);
        pluginService.associateContentProvider(cs2, contentProvider2);

        repoContentSources.add(cs1);
        repoContentSources.add(cs2);

        // Create the package type packages will be created against
        resourceType = new ResourceType(TestContentProvider.RESOURCE_TYPE_NAME,
            TestContentProvider.RESOURCE_TYPE_PLUGIN_NAME, ResourceCategory.PLATFORM, null);
        entityManager.persist(resourceType);

        packageType = new PackageType(TestContentProvider.PACKAGE_TYPE_NAME, resourceType);
        entityManager.persist(packageType);

        // Create the repo to be syncced
        Repo repo = new Repo(TestContentProvider.REPO_WITH_PACKAGES);
        repo.addContentSource(cs1);
//        repo.addContentSource(cs2);  Disabled until we implement a second test content provider to return new stuff
        repoToSync = repoManager.createRepo(overlord, repo);

        tx.commit();
    }

    @AfterMethod
    public void tearDownAfterMethod() throws Exception {

        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        ContentSourceManagerLocal contentSourceManagerLocal = LookupUtil.getContentSourceManager();
        RepoManagerLocal repoManager = LookupUtil.getRepoManagerLocal();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject overlord = subjectManager.getOverlord();

        // Delete all package version <-> content source mappings
        for (ContentSource source : repoContentSources) {
            contentSourceManagerLocal.deleteContentSource(overlord, source.getId());
        }
        repoContentSources.clear();

        // Delete the repo
        repoManager.deleteRepo(overlord, repoToSync.getId());

        // Delete any packages that were created
        for (ContentProviderPackageDetails details : TestContentProvider.PACKAGES.values()) {
            String packageName = details.getContentProviderPackageDetailsKey().getName();

            Query query = entityManager.createNamedQuery(Package.QUERY_FIND_BY_NAME_PKG_TYPE_ID);
            query.setParameter("name", packageName);
            query.setParameter("packageTypeId", packageType.getId());

            try {
                Package p = (Package) query.getSingleResult();
                entityManager.remove(p);
            }
            catch (Exception e) {
                System.out.println("===================================");
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        // Delete the package type
        packageType = entityManager.find(PackageType.class, packageType.getId());
        entityManager.remove(packageType);

        resourceType = entityManager.find(ResourceType.class, resourceType.getId());
        entityManager.remove(resourceType);

        // Delete the content source type
        contentSourceType = entityManager.find(ContentSourceType.class, contentSourceType.getId());
        entityManager.remove(contentSourceType);

        tx.commit();

        // Cleanup providers between tests
        contentProvider1.reset();
        contentProvider2.reset();

        // Plugin service teardown
        unprepareServerPluginService();
        unprepareScheduler();
    }

    @Test(enabled = TESTS_ENABLED)
    public void synchronizeRepo() throws Exception {

        // Test
        // --------------------------------------------
        boolean completed = pluginService.getContentProviderManager().synchronizeRepo(repoToSync.getId());
        assert completed;

        // Verify
        // --------------------------------------------

        // Make sure the proper calls were made into the provider
        assert contentProvider1.getLogSynchronizePackagesRepos().size() == 1 :
            "Expected: 1, Found: " + contentProvider1.getLogSynchronizePackagesRepos().size();
        assert contentProvider1.getLogGetInputStreamLocations().size() == TestContentProvider.PACKAGES.size() :
            "Expected: " + TestContentProvider.PACKAGES.size() +
            ", Found: " + contentProvider1.getLogGetInputStreamLocations().size();

        // Make sure all of the packages were added
        TransactionManager tx = getTransactionManager();
        tx.begin();
        EntityManager entityManager = getEntityManager();

        Query query = entityManager.createNamedQuery(PackageVersion.QUERY_FIND_BY_REPO_ID);
        query.setParameter("repoId", repoToSync.getId());
        List<PackageVersion> repoPackages = query.getResultList();

        assert repoPackages.size() == TestContentProvider.PACKAGES.size() :
            "Expected: " + TestContentProvider.PACKAGES.size() + ", Found: " + repoPackages.size();
    }
}
