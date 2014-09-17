/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.jpa.impl;

import org.hibernate.id.MultipleHiLoPerTableGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.enhanced.TableGenerator;
import org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory;
import org.hibernate.id.factory.spi.MutableIdentifierGeneratorFactory;
import org.hibernate.ogm.id.impl.OgmIdentityGenerator;
import org.hibernate.ogm.id.impl.OgmSequenceGenerator;
import org.hibernate.ogm.id.impl.OgmTableGenerator;

/**
 * Register OGM strategies for identifier generations
 *
 * @author Davide D'Alto
 * @author Gunnar Morling
 */
public class OgmMutableIdentifierGeneratorFactory extends DefaultIdentifierGeneratorFactory implements MutableIdentifierGeneratorFactory {

	public OgmMutableIdentifierGeneratorFactory() {
		// override the generators when AvailableSettings#USE_NEW_ID_GENERATOR_MAPPINGS is false
		register( "seqhilo", OgmSequenceGenerator.class );
		register( MultipleHiLoPerTableGenerator.class.getName(), OgmTableGenerator.class );

		// override the generators when AvailableSettings#USE_NEW_ID_GENERATOR_MAPPINGS is true
		register( TableGenerator.class.getName(), OgmTableGenerator.class );
		register( SequenceStyleGenerator.class.getName(), OgmSequenceGenerator.class );

		register( "identity", OgmIdentityGenerator.class );
	}
}
