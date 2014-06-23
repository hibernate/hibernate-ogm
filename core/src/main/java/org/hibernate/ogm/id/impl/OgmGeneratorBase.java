/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.id.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.IdGeneratorKeyMetadata;
import org.hibernate.type.Type;

/**
 * Base class for sequence and table based id generators in Hibernate OGM.
 * @author Gunnar Morling
 *
 */
public abstract class OgmGeneratorBase implements PersistentIdentifierGenerator, Configurable {

	public static final String OPT_PARAM = TableGenerator.OPT_PARAM;

	public static final String INITIAL_PARAM = TableGenerator.INITIAL_PARAM;
	public static final int DEFAULT_INITIAL_VALUE = TableGenerator.DEFAULT_INITIAL_VALUE;

	public static final String INCREMENT_PARAM = TableGenerator.INCREMENT_PARAM;
	public static final int DEFAULT_INCREMENT_SIZE = TableGenerator.DEFAULT_INCREMENT_SIZE;

	private Type identifierType;
	private Optimizer optimizer;

	private int initialValue;
	private int incrementSize;

	/**
	 * Needs to be volatile as it is set lazily after the initialization phase. Ideally, we'd get passed the grid
	 * dialect to configure().
	 */
	private volatile GridDialect gridDialect;

	@Override
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		identifierType = type;
		incrementSize = determineIncrementSize( params );
		initialValue = determineInitialValue( params );

		// if the increment size is greater than one, we prefer pooled optimization; but we
		// need to see if the user prefers POOL or POOL_LO...
		String defaultPooledOptimizerStrategy = ConfigurationHelper.getBoolean(
				Environment.PREFER_POOLED_VALUES_LO, params, false
		)
				? OptimizerFactory.POOL_LO
				: OptimizerFactory.POOL;
		final String defaultOptimizerStrategy = incrementSize <= 1 ? OptimizerFactory.NONE : defaultPooledOptimizerStrategy;
		final String optimizationStrategy = ConfigurationHelper.getString( OPT_PARAM, params, defaultOptimizerStrategy );
		optimizer = OptimizerFactory.buildOptimizer(
				optimizationStrategy,
				identifierType.getReturnedClass(),
				incrementSize,
				ConfigurationHelper.getInt( INITIAL_PARAM, params, -1 )
		);
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[0];
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return new String[0];
	}

	/**
	 * The initial value to use when we find no previous state in the
	 * generator table corresponding to our sequence.
	 *
	 * @return The initial value to use.
	 */
	public final int getInitialValue() {
		return initialValue;
	}

	/**
	 * The amount of increment to use.  The exact implications of this
	 * depends on the {@link #getOptimizer() optimizer} being used.
	 *
	 * @return The increment amount.
	 */
	public final int getIncrementSize() {
		return incrementSize;
	}

	@Override
	public abstract IdGeneratorKeyMetadata generatorKey();

	protected abstract IdGeneratorKey getGeneratorKey(SessionImplementor session);

	protected GridDialect getDialect(SessionImplementor session) {
		if (gridDialect == null) {
			gridDialect = session.getFactory().getServiceRegistry().getService( GridDialect.class );
		}

		return gridDialect;
	}

	@Override
	public synchronized Serializable generate(final SessionImplementor session, Object obj) {
		return optimizer.generate(
				new AccessCallback() {
					@Override
					public IntegralDataTypeHolder getNextValue() {
						return (IntegralDataTypeHolder) doWorkInIsolationTransaction( session );
					}

					@Override
					public String getTenantIdentifier() {
						return session.getTenantIdentifier();
					}
				}
		);
	}

	//copied and altered from TransactionHelper
	private Serializable doWorkInIsolationTransaction(final SessionImplementor session)
			throws HibernateException {
		class Work extends AbstractReturningWork<IntegralDataTypeHolder> {
			private final SessionImplementor localSession = session;

			@Override
			public IntegralDataTypeHolder execute(Connection connection) throws SQLException {
				try {
					return doWorkInCurrentTransactionIfAny( localSession );
				}
				catch ( RuntimeException sqle ) {
					throw new HibernateException( "Could not get or update next value", sqle );
				}
			}
		}
		//we want to work out of transaction
		boolean workInTransaction = false;
		Work work = new Work();
		Serializable generatedValue = session.getTransactionCoordinator().getTransaction().createIsolationDelegate().delegateWork( work, workInTransaction );
		return generatedValue;
	}

	private IntegralDataTypeHolder doWorkInCurrentTransactionIfAny(SessionImplementor session) {
		IdGeneratorKey key = getGeneratorKey( session );
		IntegralDataTypeHolder value = IdentifierGeneratorHelper.getIntegralDataTypeHolder( identifierType.getReturnedClass() );
		GridDialect gridDialect = getDialect( session );

		gridDialect.nextValue( key, value, optimizer.applyIncrementSizeToSourceValues() ? incrementSize : 1, initialValue );

		return value;
	}

	private int determineIncrementSize(Properties params) {
		return ConfigurationHelper.getInt( INCREMENT_PARAM, params, DEFAULT_INCREMENT_SIZE );
	}

	private int determineInitialValue(Properties params) {
		return ConfigurationHelper.getInt( INITIAL_PARAM, params, DEFAULT_INITIAL_VALUE );
	}
}
