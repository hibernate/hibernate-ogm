/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.id.impl;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.datastore.spi.Tuple;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.IdGeneratorKeyMetadata;
import org.hibernate.ogm.type.GridType;
import org.hibernate.ogm.type.StringType;
import org.hibernate.ogm.type.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.service.spi.ServiceRegistryImplementor;
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
public class OgmTableGenerator extends OgmGeneratorBase implements PersistentIdentifierGenerator, Configurable {

	public static final String CONFIG_PREFER_SEGMENT_PER_ENTITY = "prefer_entity_table_as_segment_value";

	public static final String TABLE_PARAM = TableGenerator.TABLE_PARAM;
	public static final String DEF_TABLE = TableGenerator.DEF_TABLE;

	public static final String VALUE_COLUMN_PARAM = TableGenerator.VALUE_COLUMN_PARAM;
	public static final String DEF_VALUE_COLUMN = TableGenerator.DEF_VALUE_COLUMN;

	public static final String SEGMENT_COLUMN_PARAM = TableGenerator.SEGMENT_COLUMN_PARAM;
	public static final String DEF_SEGMENT_COLUMN = TableGenerator.DEF_SEGMENT_COLUMN;

	public static final String SEGMENT_VALUE_PARAM = TableGenerator.SEGMENT_VALUE_PARAM;
	public static final String DEF_SEGMENT_VALUE = TableGenerator.DEF_SEGMENT_VALUE;

	private static final Log log = LoggerFactory.make();

	private Type identifierType;
	private volatile GridType identifierValueGridType;

	private String tableName;

	private String segmentColumnName;
	private String segmentValue;

	private String valueColumnName;

	private final GridType segmentGridType = StringType.INSTANCE;

	private IdGeneratorKeyMetadata generatorKeyMetadata;

	@Override
	public IdGeneratorKeyMetadata generatorKey() {
		return generatorKeyMetadata;
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

	@Override
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		super.configure( type, params, dialect );

		identifierType = type;
		tableName = determineGeneratorTableName( params, dialect );
		segmentColumnName = determineSegmentColumnName( params, dialect );
		valueColumnName = determineValueColumnName( params, dialect );
		segmentValue = determineSegmentValue( params );

		generatorKeyMetadata = IdGeneratorKeyMetadata.forTable( tableName, segmentColumnName, valueColumnName );
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
			ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params.get( IDENTIFIER_NORMALIZER );
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
		ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params.get( IDENTIFIER_NORMALIZER );
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
		ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params.get( IDENTIFIER_NORMALIZER );
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
		String segmentValue = params.getProperty( SEGMENT_VALUE_PARAM );
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

	@Override
	protected IdGeneratorKey getGeneratorKey(SessionImplementor session) {
		defineGridTypes( session );

		final String segmentName = (String) nullSafeSet(
				segmentGridType, segmentValue, segmentColumnName, session
		);

		return IdGeneratorKey.forTable( generatorKeyMetadata, segmentName );
	}

	private Object nullSafeSet(GridType type, Object value, String columnName, SessionImplementor session) {
		Tuple tuple = new Tuple();
		type.nullSafeSet( tuple, value, new String[] { columnName }, session );
		return tuple.get( columnName );
	}

	private void defineGridTypes(SessionImplementor session) {
		if ( identifierValueGridType == null ) {
			ServiceRegistryImplementor registry = session.getFactory().getServiceRegistry();
			identifierValueGridType = registry.getService( TypeTranslator.class ).getType( new LongType() );
		}
	}
}
