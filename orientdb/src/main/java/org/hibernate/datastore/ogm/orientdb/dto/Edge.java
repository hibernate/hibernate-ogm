/*
* Hibernate OGM, Domain model persistence for NoSQL datastores
* 
* License: GNU Lesser General Public License (LGPL), version 2.1 or later
* See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
*/

package org.hibernate.datastore.ogm.orientdb.dto;

import com.orientechnologies.orient.core.record.impl.ODocument;

/**
 * @author Sergey Chernolyas <sergey.chernolyas@gmail.com>
 */

public class Edge {

	private ODocument in;
	private ODocument out;

	public ODocument getIn() {
		return in;
	}

	public void setIn(ODocument in) {
		this.in = in;
	}

	public ODocument getOut() {
		return out;
	}

	public void setOut(ODocument out) {
		this.out = out;
	}
}
