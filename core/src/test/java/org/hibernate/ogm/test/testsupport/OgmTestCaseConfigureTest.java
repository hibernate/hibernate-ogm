/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.test.testsupport;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.compensation.ErrorHandlingStrategy;
import org.hibernate.ogm.compensation.impl.InvocationCollectingGridDialect;
import org.hibernate.ogm.dialect.impl.GridDialectLogger;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.junit.Test;

/**
 * @author Guillaume Smet
 */
public class OgmTestCaseConfigureTest extends OgmTestCase {

	@Override
	protected void configure(Map<String, Object> cfg) {
		cfg.put( OgmProperties.ERROR_HANDLER, MyErrorHandler.class );
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1113")
	public void testConfigureWorksProperly() {
		GridDialect gridDialect = getSessionFactory().getServiceRegistry().getService( GridDialect.class );

		assertThat( ( (GridDialectLogger) gridDialect ).getGridDialect() ).isInstanceOf( InvocationCollectingGridDialect.class );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ Poem.class };
	}

	public static class MyErrorHandler implements ErrorHandler {

		@Override
		public ErrorHandlingStrategy onFailedGridDialectOperation(FailedGridDialectOperationContext context) {
			return ErrorHandlingStrategy.ABORT;
		}

		@Override
		public void onRollback(RollbackContext context) {
			// No-op
		}
	}

}
