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
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.mapping.Table;
import org.hibernate.ogm.dialect.GridDialect;
import org.hibernate.ogm.grid.IdGeneratorKey;
import org.hibernate.ogm.grid.IdGeneratorKeyMetadata;
import org.hibernate.ogm.util.impl.Log;
import org.hibernate.ogm.util.impl.LoggerFactory;
import org.hibernate.type.Type;

/**
 * A JPA sequence-based identifier generator.
 * <p>
 * This identifier generator is also used for JPA auto identifier generation. {@link OgmTableGenerator} is used as
 * fall-back, if the current datastore does not support sequences.
 * <p>
 * Configuration parameters:
 * <table>
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
 * @author Nabeel Ali Memon <nabeel@nabeelalimemon.com>
 * @author Gunnar Morling
 */
public class OgmSequenceGenerator extends OgmGeneratorBase {

	private static final Log log = LoggerFactory.make();

	private Type type;
	private Dialect dialect;
	private Properties params;

	private String sequenceName;
	private IdGeneratorKeyMetadata generatorKeyMetadata;

	private volatile Executor delegate;

	public OgmSequenceGenerator() {
	}

	@Override
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		super.configure( type, params, dialect );

		this.type = type;
		this.dialect = dialect;
		this.params = params;
		sequenceName = determineSequenceName( params, dialect );
		generatorKeyMetadata = IdGeneratorKeyMetadata.forSequence( sequenceName );
	}

	@Override
	public IdGeneratorKeyMetadata getGeneratorKeyMetadata() {
		return delegate == null ? generatorKeyMetadata : delegate.getGeneratorKeyMetadata();
	}

	@Override
	protected IdGeneratorKey getGeneratorKey(SessionImplementor session) {
		return getDelegate( session ).getGeneratorKey( session );
	}

	/**
	 * NOTE: Copied from SequenceStyleGenerator
	 *
	 * Determine the name of the sequence (or table if this resolves to a physical table)
	 * to use.
	 * <p/>
	 * Called during {@link #configure configuration}.
	 *
	 * @param params The params supplied in the generator config (plus some standard useful extras).
	 * @param dialect The dialect in effect
	 * @return The sequence name
	 */
	private String determineSequenceName(Properties params, Dialect dialect) {
		final String sequencePerEntitySuffix = ConfigurationHelper.getString( SequenceStyleGenerator.CONFIG_SEQUENCE_PER_ENTITY_SUFFIX, params, SequenceStyleGenerator.DEF_SEQUENCE_SUFFIX );
		// JPA_ENTITY_NAME value honors <class ... entity-name="..."> (HBM) and @Entity#name (JPA) overrides.
		String sequenceName = ConfigurationHelper.getBoolean( SequenceStyleGenerator.CONFIG_PREFER_SEQUENCE_PER_ENTITY, params, false )
				? params.getProperty( JPA_ENTITY_NAME ) + sequencePerEntitySuffix
				: SequenceStyleGenerator.DEF_SEQUENCE_NAME;
		final ObjectNameNormalizer normalizer = (ObjectNameNormalizer) params.get( PersistentIdentifierGenerator.IDENTIFIER_NORMALIZER );
		sequenceName = ConfigurationHelper.getString( SequenceStyleGenerator.SEQUENCE_PARAM, params, sequenceName );
		if ( sequenceName.indexOf( '.' ) < 0 ) {
			sequenceName = normalizer.normalizeIdentifierQuoting( sequenceName );
			final String schemaName = params.getProperty( PersistentIdentifierGenerator.SCHEMA );
			final String catalogName = params.getProperty( PersistentIdentifierGenerator.CATALOG );
			sequenceName = Table.qualify(
					dialect.quote( catalogName ),
					dialect.quote( schemaName ),
					dialect.quote( sequenceName )
			);
		}
		// if already qualified there is not much we can do in a portable manner so we pass it
		// through and assume the user has set up the name correctly.

		return sequenceName;
	}

	private Executor getDelegate(SessionImplementor session) {
		if ( delegate == null ) {
			GridDialect gridDialect = getDialect( session );

			if ( gridDialect.supportsSequences() ) {
				delegate = new SequenceExecutor( generatorKeyMetadata );
			}
			else {
				log.dialectDoesNotSupportSequences( gridDialect.getClass().getName() );

				OgmTableGenerator tableGenerator = new OgmTableGenerator();
				Properties newParams = new Properties();
				newParams.putAll( params );
				newParams.put( OgmTableGenerator.SEGMENT_VALUE_PARAM, sequenceName );
				tableGenerator.configure( type, newParams, dialect );

				delegate = new TableExecutor( tableGenerator );
			}
		}

		return delegate;
	}

	/**
	 * Provides a uniform way for handling the actual sequence case and the case of delegation to the table generator.
	 */
	private interface Executor {
		IdGeneratorKeyMetadata getGeneratorKeyMetadata();
		IdGeneratorKey getGeneratorKey(SessionImplementor session);
	}

	private static class TableExecutor implements Executor {

		private final OgmTableGenerator delegate;

		public TableExecutor(OgmTableGenerator delegate) {
			this.delegate = delegate;
		}

		@Override
		public IdGeneratorKeyMetadata getGeneratorKeyMetadata() {
			return delegate.getGeneratorKeyMetadata();
		}

		@Override
		public IdGeneratorKey getGeneratorKey(SessionImplementor session) {
			return delegate.getGeneratorKey( session );
		}
	}

	private static class SequenceExecutor implements Executor {

		private final IdGeneratorKey generatorKey;

		private SequenceExecutor(IdGeneratorKeyMetadata generatorKeyMetadata) {
			generatorKey = IdGeneratorKey.forSequence( generatorKeyMetadata );
		}

		@Override
		public IdGeneratorKeyMetadata getGeneratorKeyMetadata() {
			return generatorKey.getMetadata();
		}

		@Override
		public IdGeneratorKey getGeneratorKey(SessionImplementor session) {
			return generatorKey;
		}
	}
}
