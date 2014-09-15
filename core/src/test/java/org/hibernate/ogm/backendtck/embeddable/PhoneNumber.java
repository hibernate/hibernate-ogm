/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.ogm.backendtck.embeddable;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;

import java.util.List;

/**
 * @author Davide D'Alto
 */
@Embeddable
public class PhoneNumber {

	private String main;

	@ElementCollection
	private List<String> alternatives;

	protected PhoneNumber() {
	}

	public PhoneNumber(String name, List<String> list) {
		this.main = name;
		this.alternatives = list;
	}

	public String getMain() {
		return main;
	}

	public void setMain(String name) {
		this.main = name;
	}

	public List<String> getAlternatives() {
		return alternatives;
	}

	public void setAlternatives(List<String> list) {
		this.alternatives = list;
	}
}
