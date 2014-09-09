/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;

/**
 * Register OGM strategies for identifier generations
 *
 * @author Davide D'Alto
 */
public class OgmMutableIdentifierGeneratorFactory extends DefaultIdentifierGeneratorFactory implements MutableIdentifierGeneratorFactory {

	public OgmMutableIdentifierGeneratorFactory() {
		register( org.hibernate.id.enhanced.TableGenerator.class.getName(), OgmTableGenerator.class );
		register( org.hibernate.id.enhanced.SequenceStyleGenerator.class.getName(), OgmSequenceGenerator.class );
		// TODO We are using OgmTableGenerator as a fall-back until we have a better solution (see OGM-436/OGM-550)
		register( "identity", OgmTableGenerator.class );
	}

}
