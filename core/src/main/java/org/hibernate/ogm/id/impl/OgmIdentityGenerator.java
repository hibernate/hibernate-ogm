/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.id.impl;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.IdentifierGeneratorHelper;

/**
 * An identifier generator which obtains ids assigned by the store implicitly during insertion.
 *
 * @author Gunnar Morling
 */
public class OgmIdentityGenerator implements IdentifierGenerator {

	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		// By returning this marker, the logic in ORM is advised to invoke the insert() method of the persister which
		// does not expect an id to be present prior to insertion
		return IdentifierGeneratorHelper.POST_INSERT_INDICATOR;
	}
}
