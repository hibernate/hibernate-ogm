package org.hibernate.ogm.test.integration.spring;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class Person {

	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@OneToMany
	private List<Person> children;

	@OneToOne
	private Person spouse;

	@OneToMany
	private List<Person> parents;

	public Person(Long id, String name, List<Person> children, Person spouse,
			List<Person> parents) {
		this.id = id;
		this.name = name;
		this.children = children;
		this.spouse = spouse;
		this.parents = parents;
	}

	public Person() {

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Person> getChildren() {
		return children;
	}

	public void setChildren(List<Person> children) {
		this.children = children;
	}

	public Person getSpouse() {
		return spouse;
	}

	public void setSpouse(Person spouse) {
		this.spouse = spouse;
	}

	public List<Person> getParents() {
		return parents;
	}

	public void setParents(List<Person> parents) {
		this.parents = parents;
	}

	public static ArrayList<Person> getFamily() {
		ArrayList<Person> family = new ArrayList();

		Person childTom = new Person();
		childTom.setName( "Tom" );

		Person childLouis = new Person();
		childLouis.setName( "Louis" );

		Person childAlex = new Person();
		childAlex.setName( "Alex" );

		Person mary = new Person();
		mary.setName( "Mary" );

		mary.setSpouse( childTom );
		childTom.setSpouse( mary );

		ArrayList<Person> children = new ArrayList();
		children.add( childLouis );
		children.add( childTom );
		children.add( childAlex );

		Person husband = new Person();
		husband.setName( "Nick" );

		Person wife = new Person();
		wife.setName( "Kate" );

		husband.setSpouse( wife );
		wife.setSpouse( husband );

		husband.setChildren( children );
		wife.setChildren( children );

		ArrayList<Person> parents = new ArrayList();
		parents.add( husband );
		parents.add( wife );
		childTom.setParents( parents );
		childLouis.setParents( parents );
		childAlex.setParents( parents );

		family.add( husband );
		family.add( wife );
		family.add( childAlex );
		family.add( childLouis );
		family.add( childTom );
		family.add( mary );

		return family;
	}
}
