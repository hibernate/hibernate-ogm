/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.ogm.datastore.orientdb.type.spi;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.ogm.datastore.orientdb.type.descriptor.java.ORidBagTypeDescriptor;
import org.hibernate.ogm.type.descriptor.impl.PassThroughGridTypeDescriptor;
import org.hibernate.ogm.type.impl.AbstractGenericBasicType;

import com.orientechnologies.orient.core.db.record.ridbag.ORidBag;

/**
 * The class is grid type for OrientDB's type '{@link ORidBag}'
 *
 * @author Sergey Chernolyas &lt;sergey.chernolyas@gmail.com&gt;
 */

public class ORidBagGridType extends AbstractGenericBasicType<ORidBag> {

	/**
	 * instance of the class
	 */
	public static final ORidBagGridType INSTANCE = new ORidBagGridType();

	/**
	 * The default constructor
	 */
	public ORidBagGridType() {
		super( PassThroughGridTypeDescriptor.INSTANCE, ORidBagTypeDescriptor.INSTANCE );
	}

	@Override
	public int getColumnSpan(Mapping mapping) throws MappingException {
		return 1;
	}

	@Override
	public String getName() {
		return "ORidBag";
	}
}
