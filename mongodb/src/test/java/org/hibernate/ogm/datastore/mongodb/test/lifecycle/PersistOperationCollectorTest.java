/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.test.lifecycle;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Map;

import org.bson.types.ObjectId;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.ogm.OgmSession;
import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.compensation.ErrorHandler;
import org.hibernate.ogm.compensation.ErrorHandlingStrategy;
import org.hibernate.ogm.utils.BytemanHelper;
import org.hibernate.ogm.utils.BytemanHelperStateCleanup;
import org.hibernate.ogm.utils.OgmTestCase;
import org.hibernate.ogm.utils.TestForIssue;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitConfig;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for using object ids with MongoDB.
 *
 * @author Gunnar Morling
 *
 */
@BMUnitConfig(verbose = true, debug = true)
public class PersistOperationCollectorTest extends OgmTestCase {

	@Rule
	public BytemanHelperStateCleanup bytemanState = new BytemanHelperStateCleanup();

	@Test
	@TestForIssue(jiraKey = "OGM-1103")
	public void noConcurrentModificationExceptionWhenUsingOperationCollector() {
		OgmSession session = openSession();
		Transaction tx = session.beginTransaction();

		// given
		BarKeeper brian = new BarKeeper( new ObjectId(), "Brian" );

		// when
		session.persist( brian );
		session.flush();

		brian.setName( "Bruce" );
		tx.commit();
		session.clear();
		tx = session.beginTransaction();

		BarKeeper brianLoaded = session.load( BarKeeper.class, brian.getId() );

		// then
		assertThat( brianLoaded.getId() ).isEqualTo( brian.getId() );
		assertThat( brianLoaded.getName() ).isEqualTo( "Bruce" );

		tx.commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "OGM-1114")
	@BMRule(
			targetClass = "EventContextManager",
			targetMethod = "onEventBegin(EventSource)",
			helper = "org.hibernate.ogm.utils.BytemanHelper",
			action = "countInvocation()",
			name = "countEventBegin"
	)
	public void persistListenerEnabledEvenIfNotJpa() {
		OgmSession session = openSession();

		// given
		BarKeeper brian = new BarKeeper( new ObjectId(), "Brian" );

		// when
		session.persist( brian );

		assertThat( BytemanHelper.getAndResetInvocationCount() ).isEqualTo( 1 );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { BarKeeper.class };
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put( AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, false );
		settings.put( OgmProperties.ERROR_HANDLER, MyErrorHandler.class );
	}

	public static class MyErrorHandler implements ErrorHandler {

		@Override
		public ErrorHandlingStrategy onFailedGridDialectOperation(FailedGridDialectOperationContext context) {
			return ErrorHandlingStrategy.ABORT;
		}

		@Override
		public void onRollback(RollbackContext context) {
		}
	}
}
