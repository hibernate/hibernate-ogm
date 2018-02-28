/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.storedprocedures;

import java.util.Properties;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.utils.TestForIssue;
import org.hibernate.ogm.utils.jpa.GetterPersistenceUnitInfo;

/**
 * @author Sergey Chernolyas &amp;sergey_chernolyas@gmail.com&amp;
 */
@TestForIssue(jiraKey = { "OGM-359" })
public class PositionalParametersStoredProcedureCallTest extends org.hibernate.ogm.backendtck.storedprocedures.PositionalParametersStoredProcedureCallTest {

	@Override
	protected void configure(GetterPersistenceUnitInfo info) {
		Properties properties = info.getProperties();
		properties.setProperty( OgmProperties.GRID_DIALECT, MockStoredProcedureDialect.class.getName() );
	}
}
