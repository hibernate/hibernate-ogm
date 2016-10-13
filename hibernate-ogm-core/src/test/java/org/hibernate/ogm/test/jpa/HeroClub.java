package org.hibernate.ogm.test.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.ogm.test.simpleentity.Hero;

/**
 * @author Jonathan Wood
 */
@Entity
public class HeroClub {

	@Id
	private String name;

	@OneToMany
	private List<Hero> members;

	public String getName() {
		return name; 
		}
	
	public void setName(String name) { 
		this.name = name; 
	}

	public HeroClub() {
		this.members = new ArrayList<Hero>();
	}

	public List<Hero> getMembers() {
		return members;
	}

	public void setMembers(List<Hero> members) {
		this.members = members;
	}
	
}
