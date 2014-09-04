/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.type.descriptor.impl;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for the <tt>grid</tt> side of a value mapping.
 *
 * @author Emmanuel Bernard
 */
public interface GridTypeDescriptor extends Serializable {
	<X> GridValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor);

	<X> GridValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor);
}
