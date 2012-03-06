package org.hibernate.ogm.test.associations.embedded.entities;

import javax.persistence.Embeddable;

@Embeddable
public class EmbeddedSecondLvl {

	private String secondLvlValue;

	public String getSecondLvlValue() {
		return secondLvlValue;
	}

	public void setSecondLvlValue(String secondLvlValue) {
		this.secondLvlValue = secondLvlValue;
	}

	public EmbeddedSecondLvl(String secondLvlValue) {
		super();
		this.secondLvlValue = secondLvlValue;
	}

	public EmbeddedSecondLvl() {
		super();
	}
}
