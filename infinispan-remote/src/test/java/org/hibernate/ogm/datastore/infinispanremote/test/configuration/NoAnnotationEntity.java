/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.datastore.infinispanremote.test.configuration;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * This class not overrides a cache configuration
 *
 * @author Fabio Massimo Ercoli
 */
@Entity
public class NoAnnotationEntity {

	@Id
	private String id;

}
