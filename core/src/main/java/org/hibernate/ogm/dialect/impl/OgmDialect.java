/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.dialect.impl;

import javax.persistence.GenerationType;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.ogm.dialect.identity.spi.IdentityColumnAwareGridDialect;
import org.hibernate.ogm.dialect.spi.GridDialect;
import org.hibernate.ogm.id.impl.OgmIdentityGenerator;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;

/**
 * A pseudo {@link Dialect} implementation which exposes the current {@link GridDialect}.
 *
 * @author Emmanuel Bernard
 * @author Gunnar Morling
 */
public class OgmDialect extends Dialect {

	private final GridDialect gridDialect;

	public OgmDialect(GridDialect gridDialect) {
		this.gridDialect = gridDialect;
	}

	/**
	 * Returns the current {@link GridDialect}.
	 * <p>
	 * Intended for usage in code interacting with ORM SPIs which only provide access to the {@link Dialect} but not the
	 * service registry. Other code should obtain the grid dialect from the service registry.
	 *
	 * @return the current grid dialect.
	 */
	public GridDialect getGridDialect() {
		return gridDialect;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overridden in OGM in order to make things work when {@code USE_NEW_ID_GENERATOR_MAPPINGS} is set to
	 * {@code false}. If so, and if additionally the generation type is {@link GenerationType#AUTO}, the ORM engine
	 * will invoke this method to obtain the "native" identifier generator. Depending on the store's capabilities,
	 * OGM's identity, sequence or table generator will be returned.
	 *
	 * @see AvailableSettings#USE_NEW_ID_GENERATOR_MAPPINGS
	 */
	@Override
	public Class<? extends IdentifierGenerator> getNativeIdentifierGeneratorClass() {
		if ( GridDialects.hasFacet( gridDialect, IdentityColumnAwareGridDialect.class ) ) {
			return OgmIdentityGenerator.class;
		}
		else if ( gridDialect.supportsSequences() ) {
			return OgmSequenceGenerator.class;
		}
		else {
			return OgmTableGenerator.class;
		}
	}
}
