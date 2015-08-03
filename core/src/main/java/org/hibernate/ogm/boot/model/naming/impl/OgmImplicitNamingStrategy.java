/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.boot.model.naming.impl;

import java.util.regex.Pattern;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.source.spi.AttributePath;

/**
 * An {@link ImplicitNamingStrategy} which exposes component property names via their full path (e.g. "newsId.author")
 * rather than only the actual property name (e.g. "author").
 *
 * @author Gunnar Morling
 */
public class OgmImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {

	/**
	 * A pattern common to all property names used in element collections.
	 */
	private static final Pattern ELEMENT_COLLECTION_NAME_PATTERN = Pattern.compile( "collection&&element\\." );

	@Override
	protected String transformAttributePath(AttributePath attributePath) {
		// for element collections just use the simple name
		String[] parts = ELEMENT_COLLECTION_NAME_PATTERN.split( attributePath.getFullPath() );

		// for element collections just use the simple name
		if ( parts.length == 2 ) {
			return parts[1];
		}
		else {
			return attributePath.getFullPath();
		}
	}
}
