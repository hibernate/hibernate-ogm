/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.id.impl;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.impl.StringType;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.type.LongType;
import org.hibernate.type.Type;

/**
 * A table-based id generator. Inspired by ORM's {@link TableGenerator}. Refer to its JavaDoc for some design
 * considerations.
 * <table>
 * <caption>Configuration parameters</caption>
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
 * <td>The value indicating which segment is used by this generator; refers to values in the
 * {@link #SEGMENT_COLUMN_PARAM} column</td>
 * </tr>
 * <tr>
 * <td>{@link #INITIAL_PARAM}</td>
 * <td>{@link #DEFAULT_INITIAL_VALUE}</td>
 * <td>The initial value to be stored for the given segment</td>
 * </tr>
 * <tr>
 * <td>{@link #INCREMENT_PARAM}</td>
 * <td>{@link #DEFAULT_INCREMENT_SIZE}</td>
 * <td>The increment size for the underlying segment; see the discussion on {@link org.hibernate.id.enhanced.Optimizer}
 * for more details.</td>
 * </tr>
 * <tr>
 * <td>{@link #OPT_PARAM}</td>
 * <td><i>depends on defined increment size</i></td>
 * <td>Allows explicit definition of which optimization strategy to use</td>
 * </tr>
 * </table>
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 * @author Gunnar Morling
 */
public class OgmTableGenerator extends OgmGeneratorBase implements Configurable {

	public static final String CONFIG_PREFER_SEGMENT_PER_ENTITY = TableGenerator.CONFIG_PREFER_SEGMENT_PER_ENTITY;

	public static final String TABLE_PARAM = TableGenerator.TABLE_PARAM;
	public static final String DEF_TABLE = TableGenerator.DEF_TABLE;

	public static final String VALUE_COLUMN_PARAM = TableGenerator.VALUE_COLUMN_PARAM;
	public static final String DEF_VALUE_COLUMN = TableGenerator.DEF_VALUE_COLUMN;

	public static final String SEGMENT_COLUMN_PARAM = TableGenerator.SEGMENT_COLUMN_PARAM;
	public static final String DEF_SEGMENT_COLUMN = TableGenerator.DEF_SEGMENT_COLUMN;

	public static final String SEGMENT_VALUE_PARAM = TableGenerator.SEGMENT_VALUE_PARAM;
	public static final String DEF_SEGMENT_VALUE = TableGenerator.DEF_SEGMENT_VALUE;

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private Type identifierType;
	private volatile GridType identifierValueGridType;

	private String tableName;

	private String segmentColumnName;
	private String segmentValue;

	private String valueColumnName;

	private final GridType segmentGridType = StringType.INSTANCE;

	private IdSourceKeyMetadata generatorKeyMetadata;

	@Override
	public IdSourceKeyMetadata getGeneratorKeyMetadata() {
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

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		super.configure( type, params, serviceRegistry );
		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		Dialect dialect = jdbcEnvironment.getDialect();

		identifierType = type;
		tableName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				determineGeneratorTableName( params, dialect ),
				dialect
		);

		segmentColumnName = determineSegmentColumnName( params, dialect );
		valueColumnName = determineValueColumnName( params, dialect );
		segmentValue = determineSegmentValue( params );

		generatorKeyMetadata = DefaultIdSourceKeyMetadata.forTable( tableName, segmentColumnName, valueColumnName );
	}

	/**
	 * NOTE: Copied from TableGenerator
	 * <p>
	 * Determine the table name to use for the generator values.
	 * <p>
	 * Called during {@link #configure configuration}.
	 *
	 * @see #getTableName()
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 * @return The table name to use.
	 */
	protected QualifiedName determineGeneratorTableName(Properties params, Dialect dialect) {
		String schemaName = params.getProperty( PersistentIdentifierGenerator.SCHEMA );
		if ( schemaName != null ) {
			log.schemaOptionNotSupportedForTableGenerator( schemaName );
		}

		String catalogName = params.getProperty( PersistentIdentifierGenerator.CATALOG );
		if ( catalogName != null ) {
			log.catalogOptionNotSupportedForTableGenerator( catalogName );
		}

		return QualifiedNameParser.INSTANCE.parse(
				ConfigurationHelper.getString( TABLE_PARAM, params, DEF_TABLE ),
				null,
				null
		);
	}

	/**
	 * Determine the name of the column used to indicate the segment for each
	 * row.  This column acts as the primary key.
	 * <p>
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
		ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params.get( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER );
		String name = ConfigurationHelper.getString( SEGMENT_COLUMN_PARAM, params, DEF_SEGMENT_COLUMN );
		return normalizer.toDatabaseIdentifierText( name );
	}

	/**
	 * Determine the name of the column in which we will store the generator persistent value.
	 * <p>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 *
	 * @return The name of the value column
	 */
	protected String determineValueColumnName(Properties params, Dialect dialect) {
		ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params.get( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER );
		String name = ConfigurationHelper.getString( VALUE_COLUMN_PARAM, params, DEF_VALUE_COLUMN );
		return normalizer.toDatabaseIdentifierText( name );
	}

	/**
	 * Determine the segment value corresponding to this generator instance.
	 * <p>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 *
	 * @return The name of the value column
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
		String defaultToUse = preferSegmentPerEntity ? params.getProperty( PersistentIdentifierGenerator.TABLE ) : DEF_SEGMENT_VALUE;
		log.infof( "explicit segment value for id generator [%1$s.%2$s] suggested; using default [%3$s]", tableName, segmentColumnName, defaultToUse );
		return defaultToUse;
	}

	@Override
	protected IdSourceKey getGeneratorKey(SharedSessionContractImplementor session) {
		defineGridTypes( session );

		final String segmentName = (String) nullSafeSet(
				segmentGridType, segmentValue, segmentColumnName, session
		);

		return IdSourceKey.forTable( generatorKeyMetadata, segmentName );
	}

	private Object nullSafeSet(GridType type, Object value, String columnName, SharedSessionContractImplementor session) {
		Tuple tuple = new Tuple();
		type.nullSafeSet( tuple, value, new String[] { columnName }, session );
		return tuple.get( columnName );
	}

	private void defineGridTypes(SharedSessionContractImplementor session) {
		if ( identifierValueGridType == null ) {
			ServiceRegistryImplementor registry = session.getFactory().getServiceRegistry();
			identifierValueGridType = registry.getService( TypeTranslator.class ).getType( LongType.INSTANCE );
		}
	}
}
