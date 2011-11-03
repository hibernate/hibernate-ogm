/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ogm.id.impl;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.hibernate.ogm.datastore.impl.DatastoreServices;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MappingException;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.id.IntegralDataTypeHolder;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.AccessCallback;
import org.hibernate.id.enhanced.Optimizer;
import org.hibernate.id.enhanced.OptimizerFactory;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.jdbc.AbstractReturningWork;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.impl.EmptyTupleSnapshot;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.RowKey;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.StringType;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

/**
 * An enhanced version of table-based id generation.
 * <p/>
 * Unlike the simplistic legacy one (which, btw, was only ever intended for subclassing
 * support) we "segment" the table into multiple values.  Thus a single table can
 * actually serve as the persistent storage for multiple independent generators.  One
 * approach would be to segment the values by the name of the entity for which we are
 * performing generation, which would mean that we would have a row in the generator
 * table for each entity name.  Or any configuration really; the setup is very flexible.
 * <p/>
 * In this respect it is very similar to the legacy
 * {@link org.hibernate.id.MultipleHiLoPerTableGenerator} in terms of the
 * underlying storage structure (namely a single table capable of holding
 * multiple generator values).  The differentiator is, as with
 * {@link org.hibernate.id.enhanced.SequenceStyleGenerator} as well, the externalized notion
 * of an optimizer.
 * <p/>
 * <b>NOTE</b> that by default we use a single row for all generators (based
 * on {@link #DEF_SEGMENT_VALUE}).  The configuration parameter
 * {@link #CONFIG_PREFER_SEGMENT_PER_ENTITY} can be used to change that to
 * instead default to using a row for each entity name.
 * <p/>
 * Configuration parameters:
 * <table>
 * <tr>
 * <td><b>NAME</b></td>
 * <td><b>DEFAULT</b></td>
 * <td><b>DESCRIPTION</b></td>
 * </tr>
 * <tr>
 * <td>{@link #TABLE_PARAM}</td>
 * <td>{@link #DEF_TABLE}</td>
 * <td>The name of the table to use to store/retrieve values</td>
 * </tr>
 * <tr>
 * <td>{@link #VALUE_COLUMN_PARAM}</td>
 * <td>{@link #DEF_VALUE_COLUMN}</td>
 * <td>The name of column which holds the sequence value for the given segment</td>
 * </tr>
 * <tr>
 * <td>{@link #SEGMENT_COLUMN_PARAM}</td>
 * <td>{@link #DEF_SEGMENT_COLUMN}</td>
 * <td>The name of the column which holds the segment key</td>
 * </tr>
 * <tr>
 * <td>{@link #SEGMENT_VALUE_PARAM}</td>
 * <td>{@link #DEF_SEGMENT_VALUE}</td>
 * <td>The value indicating which segment is used by this generator; refers to values in the {@link #SEGMENT_COLUMN_PARAM} column</td>
 * </tr>
 * <tr>
 * <td>{@link #SEGMENT_LENGTH_PARAM}</td>
 * <td>{@link #DEF_SEGMENT_LENGTH}</td>
 * <td>The data length of the {@link #SEGMENT_COLUMN_PARAM} column; used for schema creation</td>
 * </tr>
 * <tr>
 * <td>{@link #INITIAL_PARAM}</td>
 * <td>{@link #DEFAULT_INITIAL_VALUE}</td>
 * <td>The initial value to be stored for the given segment</td>
 * </tr>
 * <tr>
 * <td>{@link #INCREMENT_PARAM}</td>
 * <td>{@link #DEFAULT_INCREMENT_SIZE}</td>
 * <td>The increment size for the underlying segment; see the discussion on {@link org.hibernate.id.enhanced.Optimizer} for more details.</td>
 * </tr>
 * <tr>
 * <td>{@link #OPT_PARAM}</td>
 * <td><i>depends on defined increment size</i></td>
 * <td>Allows explicit definition of which optimization strategy to use</td>
 * </tr>
 * </table>
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class OgmTableGenerator implements PersistentIdentifierGenerator, Configurable {

	private static final Log log = LoggerFactory.make();

	public static final String CONFIG_PREFER_SEGMENT_PER_ENTITY = "prefer_entity_table_as_segment_value";

	public static final String TABLE_PARAM = "table_name";
	public static final String DEF_TABLE = "hibernate_sequences";

	public static final String VALUE_COLUMN_PARAM = "value_column_name";
	public static final String DEF_VALUE_COLUMN = "next_val";

	public static final String SEGMENT_COLUMN_PARAM = "segment_column_name";
	public static final String DEF_SEGMENT_COLUMN = "sequence_name";

	public static final String SEGMENT_VALUE_PARAM = "segment_value";
	public static final String DEF_SEGMENT_VALUE = "default";

	public static final String SEGMENT_LENGTH_PARAM = "segment_value_length";
	public static final int DEF_SEGMENT_LENGTH = 255;

	public static final String INITIAL_PARAM = "initial_value";
	public static final int DEFAULT_INITIAL_VALUE = 1;

	public static final String INCREMENT_PARAM = "increment_size";
	public static final int DEFAULT_INCREMENT_SIZE = 1;

	public static final String OPT_PARAM = "optimizer";


	private Type identifierType;

	private String tableName;

	private String segmentColumnName;
	private String segmentValue;
	private int segmentValueLength;

	private String valueColumnName;
	private int initialValue;
	private int incrementSize;

	private String selectQuery;
	private String insertQuery;
	private String updateQuery;

	private Optimizer optimizer;
	private long accessCount = 0;
	private volatile GridType identifierValueGridType;
	private GridType segmentGridType = StringType.INSTANCE;
	private volatile GridDialect gridDialect;

	/**
	 * {@inheritDoc}
	 */
	public Object generatorKey() {
		return tableName;
	}

	/**
	 * Type mapping for the identifier.
	 *
	 * @return The identifier type mapping.
	 */
	public final Type getIdentifierType() {
		return identifierType;
	}

	/**
	 * The name of the table in which we store this generator's persistent state.
	 *
	 * @return The table name.
	 */
	public final String getTableName() {
		return tableName;
	}

	/**
	 * The name of the column in which we store the segment to which each row
	 * belongs.  The value here acts as PK.
	 *
	 * @return The segment column name
	 */
	public final String getSegmentColumnName() {
		return segmentColumnName;
	}

	/**
	 * The value in {@link #getSegmentColumnName segment column} which
	 * corresponding to this generator instance.  In other words this value
	 * indicates the row in which this generator instance will store values.
	 *
	 * @return The segment value for this generator instance.
	 */
	public final String getSegmentValue() {
		return segmentValue;
	}

	/**
	 * The size of the {@link #getSegmentColumnName segment column} in the
	 * underlying table.
	 * <p/>
	 * <b>NOTE</b> : should really have been called 'segmentColumnLength' or
	 * even better 'segmentColumnSize'
	 *
	 * @return the column size.
	 */
	public final int getSegmentValueLength() {
		return segmentValueLength;
	}

	/**
	 * The name of the column in which we store our persistent generator value.
	 *
	 * @return The name of the value column.
	 */
	public final String getValueColumnName() {
		return valueColumnName;
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

	/**
	 * The optimizer being used by this generator.
	 *
	 * @return Out optimizer.
	 */
	public final Optimizer getOptimizer() {
		return optimizer;
	}

	/**
	 * Getter for property 'tableAccessCount'.  Only really useful for unit test
	 * assertions.
	 *
	 * @return Value for property 'tableAccessCount'.
	 */
	public final long getTableAccessCount() {
		return accessCount;
	}

	/**
	 * {@inheritDoc}
	 */
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		identifierType = type;

		tableName = determineGeneratorTableName( params, dialect );
		segmentColumnName = determineSegmentColumnName( params, dialect );
		valueColumnName = determineValueColumnName( params, dialect );

		segmentValue = determineSegmentValue( params );

		segmentValueLength = determineSegmentColumnSize( params );
		initialValue = determineInitialValue( params );
		incrementSize = determineIncrementSize( params );

		//this.selectQuery = buildSelectQuery( dialect );
		//this.updateQuery = buildUpdateQuery();
		//this.insertQuery = buildInsertQuery();

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

	/**
	 * Determine the table name to use for the generator values.
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 *
	 * @return The table name to use.
	 *
	 * @see #getTableName()
	 */
	protected String determineGeneratorTableName(Properties params, Dialect dialect) {
		String name = ConfigurationHelper.getString( TABLE_PARAM, params, DEF_TABLE );
		boolean isGivenNameUnqualified = name.indexOf( '.' ) < 0;
		if ( isGivenNameUnqualified ) {
			ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );
			name = normalizer.normalizeIdentifierQuoting( name );
			// if the given name is un-qualified we may neen to qualify it
			String schemaName = normalizer.normalizeIdentifierQuoting( params.getProperty( SCHEMA ) );
			String catalogName = normalizer.normalizeIdentifierQuoting( params.getProperty( CATALOG ) );
			name = Table.qualify(
					dialect.quote( catalogName ),
					dialect.quote( schemaName ),
					dialect.quote( name )
			);
		}
		else {
			// if already qualified there is not much we can do in a portable manner so we pass it
			// through and assume the user has set up the name correctly.
		}
		return name;
	}

	/**
	 * Determine the name of the column used to indicate the segment for each
	 * row.  This column acts as the primary key.
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 *
	 * @return The name of the segment column
	 *
	 * @see #getSegmentColumnName()
	 */
	protected String determineSegmentColumnName(Properties params, Dialect dialect) {
		ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );
		String name = ConfigurationHelper.getString( SEGMENT_COLUMN_PARAM, params, DEF_SEGMENT_COLUMN );
		return dialect.quote( normalizer.normalizeIdentifierQuoting( name ) );
	}

	/**
	 * Determine the name of the column in which we will store the generator persistent value.
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 *
	 * @return The name of the value column
	 *
	 * @see #getValueColumnName()
	 */
	protected String determineValueColumnName(Properties params, Dialect dialect) {
		ObjectNameNormalizer normalizer = ( ObjectNameNormalizer ) params.get( IDENTIFIER_NORMALIZER );
		String name = ConfigurationHelper.getString( VALUE_COLUMN_PARAM, params, DEF_VALUE_COLUMN );
		return dialect.quote( normalizer.normalizeIdentifierQuoting( name ) );
	}

	/**
	 * Determine the segment value corresponding to this generator instance.
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 *
	 * @return The name of the value column
	 *
	 * @see #getSegmentValue()
	 */
	protected String determineSegmentValue(Properties params) {
		String segmentValue = params.getProperty(SEGMENT_VALUE_PARAM);
		if ( StringHelper.isEmpty( segmentValue ) ) {
			segmentValue = determineDefaultSegmentValue( params );
		}
		return segmentValue;
	}

	/**
	 * Used in the cases where {@link #determineSegmentValue} is unable to
	 * determine the value to use.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 *
	 * @return The default segment value to use.
	 */
	protected String determineDefaultSegmentValue(Properties params) {
		boolean preferSegmentPerEntity = ConfigurationHelper.getBoolean( CONFIG_PREFER_SEGMENT_PER_ENTITY, params, false );
		String defaultToUse = preferSegmentPerEntity ? params.getProperty( TABLE ) : DEF_SEGMENT_VALUE;
		log.info( "explicit segment value for id generator [" + tableName + '.' + segmentColumnName + "] suggested; using default [" + defaultToUse + "]" );
		return defaultToUse;
	}

	/**
	 * Determine the size of the {@link #getSegmentColumnName segment column}
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 *
	 * @return The size of the segment column
	 *
	 * @see #getSegmentValueLength()
	 */
	protected int determineSegmentColumnSize(Properties params) {
		return ConfigurationHelper.getInt( SEGMENT_LENGTH_PARAM, params, DEF_SEGMENT_LENGTH );
	}

	protected int determineInitialValue(Properties params) {
		return ConfigurationHelper.getInt( INITIAL_PARAM, params, DEFAULT_INITIAL_VALUE );
	}

	protected int determineIncrementSize(Properties params) {
		return ConfigurationHelper.getInt(INCREMENT_PARAM, params, DEFAULT_INCREMENT_SIZE);
	}

	//TODO remove build*Query
	protected String buildSelectQuery(Dialect dialect) {
		final String alias = "tbl";
		String query = "select " + StringHelper.qualify( alias, valueColumnName ) +
				" from " + tableName + ' ' + alias +
				" where " + StringHelper.qualify( alias, segmentColumnName ) + "=?";
		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );
		lockOptions.setAliasSpecificLockMode( alias, LockMode.PESSIMISTIC_WRITE );
		Map updateTargetColumnsMap = Collections.singletonMap( alias, new String[] { valueColumnName } );
		return dialect.applyLocksToSql( query, lockOptions, updateTargetColumnsMap );
	}

	protected String buildUpdateQuery() {
		return "update " + tableName +
				" set " + valueColumnName + "=? " +
				" where " + valueColumnName + "=? and " + segmentColumnName + "=?";
	}

	protected String buildInsertQuery() {
		return "insert into " + tableName + " (" + segmentColumnName + ", " + valueColumnName + ") " + " values (?,?)";
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized Serializable generate(final SessionImplementor session, Object obj) {
		return optimizer.generate(
				new AccessCallback() {
					public IntegralDataTypeHolder getNextValue() {
						return ( IntegralDataTypeHolder ) doWorkInIsolationTransaction( session );
					}
				}
		);
	}

	//copied and altered from TransactionHelper
	public Serializable doWorkInIsolationTransaction(final SessionImplementor session)
			throws HibernateException {
		class Work extends AbstractReturningWork<IntegralDataTypeHolder> {
			private SessionImplementor localSession = session;

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
		Serializable generatedValue = session.getTransactionCoordinator().getTransaction().createIsolationDelegate().delegateWork(work, workInTransaction);
		return generatedValue;
	}

	public IntegralDataTypeHolder doWorkInCurrentTransactionIfAny(SessionImplementor session) {
		defineGridTypes( session );
		final Object segmentColumnValue = nullSafeSet(
				segmentGridType, segmentValue, segmentColumnName, session
		);
		RowKey key = new RowKey(
				tableName,
				new String[] { segmentColumnName },
				new Object[] { segmentColumnValue }
		);

		GridDialect dialect = getDialect(session);
		IntegralDataTypeHolder value = IdentifierGeneratorHelper.getIntegralDataTypeHolder( identifierType.getReturnedClass() );
		dialect.nextValue(key, value, optimizer.applyIncrementSizeToSourceValues() ? incrementSize : 1, initialValue);

		accessCount++;

		return value;
	}

	private GridDialect getDialect(SessionImplementor session) {
		if (gridDialect == null) {
			gridDialect = session.getFactory().getServiceRegistry().getService(DatastoreServices.class).getGridDialect();
		}
		return gridDialect;
	}

	private Object nullSafeSet(GridType type, Object value, String columnName, SessionImplementor session) {
		Tuple tuple = new Tuple( EmptyTupleSnapshot.SINGLETON );
		type.nullSafeSet( tuple, value, new String[] { columnName }, session );
		return tuple.get( columnName );
	}

	private void defineGridTypes(SessionImplementor session) {
		if ( identifierValueGridType == null ) {
			ServiceRegistryImplementor registry = session.getFactory().getServiceRegistry();
			identifierValueGridType = registry.getService(TypeTranslator.class).getType(new LongType());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return new String[] { };
	}

	/**
	 * {@inheritDoc}
	 */
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return new String[] { };
	}
}
