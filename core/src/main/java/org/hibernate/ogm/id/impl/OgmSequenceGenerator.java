/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.id.impl;

import java.util.Properties;

import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedNameParser;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.ogm.dialect.impl.GridDialects;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.model.impl.DefaultIdSourceKeyMetadata;
import org.hibernate.ogm.model.key.spi.IdSourceKey;
import org.hibernate.ogm.model.key.spi.IdSourceKeyMetadata;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * A JPA sequence-based identifier generator (inspired by {@link SequenceStyleGenerator}.
 * <p>
 * This identifier generator is also used for JPA auto identifier generation. {@link OgmTableGenerator} is used as
 * fall-back, if the current datastore does not support sequences.
 * <table>
 * <caption>Configuration parameters</caption>
 * <tr>
 * <td><b>NAME</b></td>
 * <td><b>DESCRIPTION</b></td>
 * </tr>
 * <tr>
 * <td>{@link org.hibernate.id.enhanced.SequenceStyleGenerator#SEQUENCE_PARAM}</td>
 * <td>The name of the sequence to use store/retrieve sequence values</td>
 * </tr>
 * <tr>
 * <td>{@link org.hibernate.id.enhanced.SequenceStyleGenerator#INITIAL_PARAM}</td>
 * <td>The initial value of the sequence</td>
 * </tr>
 * <tr>
 * <td>{@link org.hibernate.id.enhanced.SequenceStyleGenerator#INCREMENT_PARAM}</td>
 * <td>The increment of the sequence</td>
 * </tr>
 * </table>
 *
 * @author Nabeel Ali Memon &lt;nabeel@nabeelalimemon.com&gt;
 * @author Steve Ebersole
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Gunnar Morling
 */
public class OgmSequenceGenerator extends OgmGeneratorBase implements ExportableProducer {

	private static final Log log = LoggerFactory.make( MethodHandles.lookup() );

	private Type type;
	private Properties params;

	private QualifiedName logicalQualifiedSequenceName;
	private String sequenceName;
	private IdSourceKeyMetadata generatorKeyMetadata;

	private IdSourceKeyAndKeyMetadataProvider delegate;

	public OgmSequenceGenerator() {
	}

	@Override
	public void configure(Type type, Properties params, ServiceRegistry serviceRegistry) throws MappingException {
		super.configure( type, params, serviceRegistry );
		JdbcEnvironment jdbcEnvironment = serviceRegistry.getService( JdbcEnvironment.class );

		this.type = type;
		this.params = params;
		logicalQualifiedSequenceName = determineSequenceName( params, jdbcEnvironment );
		sequenceName = jdbcEnvironment.getQualifiedObjectNameFormatter().format(
				logicalQualifiedSequenceName,
				jdbcEnvironment.getDialect()
		);
		generatorKeyMetadata = DefaultIdSourceKeyMetadata.forSequence( sequenceName );
		delegate = getDelegate( serviceRegistry );
	}

	@Override
	public IdSourceKeyMetadata getGeneratorKeyMetadata() {
		return delegate.getGeneratorKeyMetadata();
	}

	@Override
	protected IdSourceKey getGeneratorKey(SharedSessionContractImplementor session) {
		return delegate.getGeneratorKey( session );
	}

	/**
	 * NOTE: Copied from SequenceStyleGenerator
	 *
	 * Determine the name of the sequence (or table if this resolves to a physical table)
	 * to use.
	 * <p>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param jdbcEnv
	 * @return The sequence name
	 */
	protected QualifiedName determineSequenceName(Properties params, JdbcEnvironment jdbcEnv) {
		final String sequencePerEntitySuffix = ConfigurationHelper.getString( SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, params, SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX );
		// JPA_ENTITY_NAME value honors <class ... entity-name="..."> (HBM) and @Entity#name (JPA) overrides.
		final String defaultSequenceName = ConfigurationHelper.getBoolean( SequenceStyleGenerator.CONFIG_PREFER_SEQUENCE_PER_ENTITY, params, false )
				? params.getProperty( JPA_ENTITY_NAME ) + sequencePerEntitySuffix
				: SequenceStyleGenerator.DEF_SEQUENCE_NAME;

		final String sequenceName = ConfigurationHelper.getString( SequenceStyleGenerator.SEQUENCE_PARAM, params, defaultSequenceName );
		if ( sequenceName.contains( "." ) ) {
			return QualifiedNameParser.INSTANCE.parse( sequenceName );
		}
		else {
			final String schemaName = params.getProperty( PersistentIdentifierGenerator.SCHEMA );
			if ( schemaName != null ) {
				log.schemaOptionNotSupportedForSequenceGenerator( schemaName );
			}

			final String catalogName = params.getProperty( PersistentIdentifierGenerator.CATALOG );
			if ( catalogName != null ) {
				log.catalogOptionNotSupportedForSequenceGenerator( catalogName );
			}

			return new QualifiedNameParser.NameParts(
					null,
					null,
					jdbcEnv.getIdentifierHelper().toIdentifier( sequenceName )
			);
		}
	}

	private IdSourceKeyAndKeyMetadataProvider getDelegate(ServiceRegistry serviceRegistry) {
		GridDialect gridDialect = super.getGridDialect();

		if ( gridDialect.supportsSequences() ) {
			return new SequenceKeyAndMetadataProvider( generatorKeyMetadata );
		}
		else {
			log.dialectDoesNotSupportSequences(
					GridDialects.getWrappedDialect( gridDialect ),
					(String) params.get( SequenceStyleGenerator.JPA_ENTITY_NAME )
			);

			OgmTableGenerator tableGenerator = new OgmTableGenerator();
			Properties newParams = new Properties();
			newParams.putAll( params );
			newParams.put( OgmTableGenerator.SEGMENT_VALUE_PARAM, sequenceName );
			tableGenerator.configure( type, newParams, serviceRegistry );

			return new TableKeyAndMetadataProvider( tableGenerator );
		}
	}

	/**
	 * Provides a uniform way for handling the actual sequence case and the case of delegation to the table generator.
	 */
	private interface IdSourceKeyAndKeyMetadataProvider {
		IdSourceKeyMetadata getGeneratorKeyMetadata();
		IdSourceKey getGeneratorKey(SharedSessionContractImplementor session);
	}

	private static class TableKeyAndMetadataProvider implements IdSourceKeyAndKeyMetadataProvider {

		private final OgmTableGenerator delegate;

		public TableKeyAndMetadataProvider(OgmTableGenerator delegate) {
			this.delegate = delegate;
		}

		@Override
		public IdSourceKeyMetadata getGeneratorKeyMetadata() {
			return delegate.getGeneratorKeyMetadata();
		}

		@Override
		public IdSourceKey getGeneratorKey(SharedSessionContractImplementor session) {
			return delegate.getGeneratorKey( session );
		}
	}

	private static class SequenceKeyAndMetadataProvider implements IdSourceKeyAndKeyMetadataProvider {

		private final IdSourceKey idSourceKey;

		private SequenceKeyAndMetadataProvider(IdSourceKeyMetadata idSourceKeyMetadata) {
			idSourceKey = IdSourceKey.forSequence( idSourceKeyMetadata );
		}

		@Override
		public IdSourceKeyMetadata getGeneratorKeyMetadata() {
			return idSourceKey.getMetadata();
		}

		@Override
		public IdSourceKey getGeneratorKey(SharedSessionContractImplementor session) {
			return idSourceKey;
		}
	}

	@Override
	public void registerExportables(Database database) {
		final Namespace namespace = database.locateNamespace(
				logicalQualifiedSequenceName.getCatalogName(),
				logicalQualifiedSequenceName.getSchemaName()
		);
		Sequence sequence = namespace.locateSequence( logicalQualifiedSequenceName.getObjectName() );
		if ( sequence != null ) {
			sequence.validate( getInitialValue(), getIncrementSize() );
		}
		else {
			sequence = namespace.createSequence( logicalQualifiedSequenceName.getObjectName(), getInitialValue(), getIncrementSize() );
		}
	}
}
