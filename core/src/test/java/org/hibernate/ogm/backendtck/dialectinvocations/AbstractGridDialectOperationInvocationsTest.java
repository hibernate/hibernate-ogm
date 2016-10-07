/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.dialectinvocations;

import java.util.List;
import java.util.Map;

import org.hibernate.ogm.cfg.OgmProperties;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.DuplicateInsertPreventionStrategy;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.DefaultEntityKeyMetadata;
import org.hibernate.ogm.utils.InvokedOperationsLoggingDialect;
import org.hibernate.ogm.utils.OgmTestCase;
import org.junit.Before;

/**
 * Base class for all the tests checking the operation invoked.
 *
 * @author Guillaume Smet
 */
public abstract class AbstractGridDialectOperationInvocationsTest extends OgmTestCase {

	@Before
	public void resetOperationsLog() {
		getOperationsLogger().reset();
	}

	@Override
	protected void configure(Map<String, Object> settings) {
		settings.put( OgmProperties.GRID_DIALECT, InvokedOperationsLoggingDialect.class );
	}

	private InvokedOperationsLoggingDialect getOperationsLogger() {
		InvokedOperationsLoggingDialect invocationLogger = GridDialects.getDelegateOrNull(
				getGridDialect(),
				InvokedOperationsLoggingDialect.class
		);

		return invocationLogger;
	}

	protected GridDialect getGridDialect() {
		return getSessionFactory().getServiceRegistry().getService( GridDialect.class );
	}

	protected List<String> getOperations() {
		return getOperationsLogger().getOperations();
	}

	protected boolean isDuplicateInsertPreventionStrategyNative(GridDialect gridDialect) {
		return DuplicateInsertPreventionStrategy.NATIVE
				.equals( gridDialect.getDuplicateInsertPreventionStrategy( new DefaultEntityKeyMetadata( "TableName", new String[]{ "id" } ) ) );
	}
}
