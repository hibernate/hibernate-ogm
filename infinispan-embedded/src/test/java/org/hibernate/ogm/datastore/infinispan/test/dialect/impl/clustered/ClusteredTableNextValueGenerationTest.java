/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl.clustered;

import org.hibernate.ogm.backendtck.id.TableNextValueGenerationTest;
import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;

import org.junit.Test;

/**
 * Test that the generation of sequences is thread safe.
 *
 * @see TableNextValueGenerationTest
 * @author Fabio Massimo Ercoli (C) 2017 Red Hat Inc.
 */
public class ClusteredTableNextValueGenerationTest extends TableNextValueGenerationTest {

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		info.getProperties().setProperty( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, "infinispan-dist.xml" );
	}

	/**
	 * This test is skipped for Infinispan dialect on base Class.
	 * Using clustered counters on top of Infinispan clustered configuration can be enabled.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void testIncrements_inClusteredMode() throws InterruptedException {
		super.testIncrements();
	}

}
