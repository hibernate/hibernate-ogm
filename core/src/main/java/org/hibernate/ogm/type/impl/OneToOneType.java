/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.impl;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.ogm.model.spi.Tuple;
import org.hibernate.ogm.type.spi.GridType;
import org.hibernate.ogm.type.spi.TypeTranslator;

/**
 * @author Emmanuel Bernard
 */
public class OneToOneType extends GridTypeDelegatingToCoreType implements GridType {
	private final TypeTranslator typeTranslator;
	private final org.hibernate.type.OneToOneType delegate;

	public OneToOneType(org.hibernate.type.OneToOneType type, TypeTranslator typeTranslator) {
		super( type );
		this.delegate = type;
		this.typeTranslator = typeTranslator;
	}

	@Override
	public Object nullSafeGet(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return resolve( hydrate( rs, names, session, owner ), session, owner );
	}

	@Override
	public Object nullSafeGet(Tuple rs, String name, SessionImplementor session, Object owner)
			throws HibernateException {
		return nullSafeGet( rs, new String[] {name}, session, owner );
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, boolean[] settable, SessionImplementor session)
			throws HibernateException {
		//Nothing to do
	}

	@Override
	public void nullSafeSet(Tuple resultset, Object value, String[] names, SessionImplementor session)
			throws HibernateException {
		//nothing to do
	}

	@Override
	public Object hydrate(Tuple rs, String[] names, SessionImplementor session, Object owner)
			throws HibernateException {
		return session.getContextEntityIdentifier( owner );
	}
}
