/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.utils.test;

import org.junit.Test;

import junit.framework.Assert;

/**
 * To be run in an IDE this test requires option -Dhibernate.service.allow_crawling=false
 * In fact the whole purpose is to verify the testsuite is run with this option enabled.
 *
 * We run the testsuite with ALLOW_CRAWLING disabled to make sure the project is using the
 * latest version (definition) of any Service defined by Hibernate ORM: since HHH-8619
 * ORM will attempt to translate legacy service requests remapping them to their new
 * version. By disabling this in our testsuite we make sure that at least at release time
 * of a version of OGM the services are up to date.
 *
 * @author Sanne Grinovero
 */
public class EnvironmentTest {

	//TODO replace with constant org.hibernate.service.internal.AbstractServiceRegistryImpl#ALLOW_CRAWLING
	private static final String ALLOW_CRAWLING = "hibernate.service.allow_crawling";

	@Test
	public void hibernateORMServiceCrawlingDisabled() {
		String property = System.getProperty( ALLOW_CRAWLING );
		Assert.assertEquals( "false", property );
	}

}
