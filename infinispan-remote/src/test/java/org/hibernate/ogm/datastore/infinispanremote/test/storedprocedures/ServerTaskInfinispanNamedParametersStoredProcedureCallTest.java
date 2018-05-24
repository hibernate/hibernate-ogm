/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.storedprocedures;

import org.hibernate.ogm.backendtck.storedprocedures.NamedParametersStoredProcedureCallTest;
import org.hibernate.ogm.datastore.infinispanremote.utils.InfinispanRemoteJpaServerRunner;
import org.hibernate.ogm.utils.TestForIssue;

import org.junit.runner.RunWith;

/**
 * Testing call of stored procedures.
 * <p>This test based on 3 deployables (simple-value-procedure.jar, result-set-procedure.jar, exceptional-procedure.jar).
 * <p>They're representatives of following classes accordingly {@link SimpleValueProcedure}, {@link ResultSetProcedure} and {@link ExceptionalProcedure}.
 *
 * @author The Viet Nguyen &amp;ntviet18@gmail.com&amp;
 */
@TestForIssue(jiraKey = { "OGM-1431" })
@RunWith(InfinispanRemoteJpaServerRunner.class)
public class ServerTaskInfinispanNamedParametersStoredProcedureCallTest extends NamedParametersStoredProcedureCallTest {

	@Override
	public void testExceptionWhenUsingNotRegisteredParameter() {
		// this dialect delegate validation to users
	}
}
