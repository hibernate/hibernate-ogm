/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.mongodb.options;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.ogm.datastore.document.options.AssociationStorageType;
import org.hibernate.ogm.datastore.mongodb.options.impl.AssociationDocumentStorageConverter;
import org.hibernate.ogm.options.spi.MappingOption;

/**
 * Specifies whether association documents should be stored in a separate collection per association type or in one
 * global collection for all associations. Only applies if the association storage option is set to
 * {@link AssociationStorageType#ASSOCIATION_DOCUMENT}. When given for non-association properties, this setting is
 * ignored.
 *
 * @author Gunnar Morling
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@MappingOption(AssociationDocumentStorageConverter.class)
public @interface AssociationDocumentStorage {

	/**
	 * Whether association documents should be stored in a separate collection per association type or in one global
	 * collection for all associations
	 */
	AssociationDocumentType value();
}
