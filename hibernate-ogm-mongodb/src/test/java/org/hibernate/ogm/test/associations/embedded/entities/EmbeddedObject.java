package org.hibernate.ogm.test.associations.embedded.entities;

import javax.persistence.Embeddable;
import javax.persistence.Embedded;

@Embeddable
public class EmbeddedObject {
	private String embValue;
	private int intValue;

	@Embedded
	private EmbeddedSecondLvl secondLvl;

	public EmbeddedSecondLvl getSecondLvl() {
		return secondLvl;
	}

	public void setSecondLvl(EmbeddedSecondLvl secondLvl) {
		this.secondLvl = secondLvl;
	}

	public int getIntValue() {
		return intValue;
	}

	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

	public EmbeddedObject() {
		super();
	}

	public EmbeddedObject(String embValue, int intValue, EmbeddedSecondLvl secondLvl) {
		this.embValue = embValue;
		this.intValue = intValue;
		this.secondLvl = secondLvl;
	}

	public String getEmbValue() {
		return embValue;
	}

	public void setEmbValue(String embValue) {
		this.embValue = embValue;
	}
}
