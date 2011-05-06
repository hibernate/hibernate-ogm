package org.hibernate.ogm.examples.gettingstarted.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.TableGenerator;

@Entity
public class Dog {
   @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "dog")
   @TableGenerator(
      name = "dog",
      table = "sequences",
      pkColumnName = "key",
      pkColumnValue = "dog",
      valueColumnName = "seed"
   )
   public Long getId() { return id; }
   public void setId(Long id) { this.id = id; }
   private Long id;

   public String getName() { return name; }
   public void setName(String name) { this.name = name; }
   private String name;

   @ManyToOne
   public Breed getBreed() { return breed; }
   public void setBreed(Breed breed) { this.breed = breed; }
   private Breed breed;
}