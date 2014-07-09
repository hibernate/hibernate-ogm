/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.massindex.model;

import org.hibernate.ogm.backendtck.id.NewsID;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * This is a simple implementation used by some tests; it's not supposed to be an example on how to implement a
 * {@link TwoWayStringBridge}.
 *
 * @author Davide D'Alto &lt;davide@hibernate.org&gt;
 */
public class NewsIdFieldBridge implements TwoWayStringBridge {

	private static final String SEP = "::::";

	@Override
	public String objectToString(Object object) {
		NewsID newsId = (NewsID) object;
		return newsId.getTitle() + SEP + newsId.getAuthor();
	}

	@Override
	public Object stringToObject(String stringValue) {
		String[] split = stringValue.split( SEP );
		return new NewsID( split[0], split[1] );
	}

}
