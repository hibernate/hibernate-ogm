/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispan.test.dialect.impl.counter;

import org.hibernate.ogm.backendtck.id.SequenceNextValueGenerationTest;
import org.hibernate.ogm.datastore.infinispan.InfinispanProperties;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;

/**
 * Repeat {@link SequenceNextValueGenerationTest} tests
 * with a configuration does not defining transport
 *
 * @author Fabio Massimo Ercoli
 */
@TestForIssue(jiraKey = "OGM-1376")
public class TransportNotDefinedSequenceGenerationTest extends SequenceNextValueGenerationTest {

	protected void configure(GetterPersistenceUnitInfo info) {
		super.configure( info );
		info.getProperties().setProperty( InfinispanProperties.CONFIGURATION_RESOURCE_NAME, "infinispan-local.xml" );
	}
}
