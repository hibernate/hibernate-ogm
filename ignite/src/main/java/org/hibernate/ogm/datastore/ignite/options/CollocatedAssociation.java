/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.ignite.options;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.ogm.datastore.ignite.options.impl.CollocatedAssociationConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Specifies that objects in collections are loaded from parent objects's node only.
 *
 * @author Victor Kadachigov
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@MappingOption(CollocatedAssociationConverter.class)
public @interface CollocatedAssociation {

	boolean value() default true;
}
