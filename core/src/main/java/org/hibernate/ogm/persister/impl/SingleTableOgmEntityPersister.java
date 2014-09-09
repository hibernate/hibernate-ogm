/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.persister.impl;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;

/**
 * Use single table strategy.
 *
 * @see javax.persistence.InheritanceType#SINGLE_TABLE
 * @author Emmanuel Bernard
 */
public class SingleTableOgmEntityPersister extends OgmEntityPersister {

	public SingleTableOgmEntityPersister(
			final PersistentClass persistentClass,
			final EntityRegionAccessStrategy cacheAccessStrategy,
			final NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			final SessionFactoryImplementor factory,
			final Mapping mapping) throws HibernateException {
		super( persistentClass, cacheAccessStrategy, naturalIdRegionAccessStrategy, factory, mapping, resolveDiscriminator( persistentClass, factory ) );
	}

	private static EntityDiscriminator resolveDiscriminator(final PersistentClass persistentClass, final SessionFactoryImplementor factory) {
		if ( persistentClass.isPolymorphic() ) {
			Value discrimValue = persistentClass.getDiscriminator();
			Selectable selectable = (Selectable) discrimValue.getColumnIterator().next();
			if ( discrimValue.hasFormula() ) {
				throw new UnsupportedOperationException( "OGM doesn't support discriminator formulas" );
			}
			else {
				return new ColumnBasedDiscriminator( persistentClass, factory, (Column) selectable );
			}
		}
		else {
			return NotNeededDiscriminator.INSTANCE;
		}
	}

	@Override
	protected String filterFragment(String alias, Set<String> treatAsDeclarations) {
		return null;
	}

}
