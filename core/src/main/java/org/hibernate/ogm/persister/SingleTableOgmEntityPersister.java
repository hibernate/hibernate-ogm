/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.ogm.persister;

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
