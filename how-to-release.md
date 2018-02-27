# Releasing Hibernate OGM

## Requirements

Make sure you have:

1. **JDK 8** for the build (the created artifacts are compatible with Java 7)

2. **Maven**

3. all the permissions required to perform the release process:

  - [CI](http://ci.hibernate.org/)
  - [Nexus](https://repository.jboss.org/nexus/index.html): you can try to login on the Nexus web interface

## Release process

To prepare and release a new version of Hibernate OGM follow these steps (executed against the branch you intend to release):

### Preparation

Verify:

1. the project status on [Jenkins](http://ci.hibernate.org/view/OGM/)

2. the latest version of Hibernate ORM is used

3. there are no outstanding issues in JIRA

4. tests and artifacts:

   ```
       mvn clean install -s settings-example.xml
   ```

5. the distribution package as built by Maven (_distribution/target/hibernate-ogm-[version]-dist_).

   They should contain the appropriate dependencies, without duplicates. The creation of these directories is driven by the assembly plugin (see _distribution/src/main/assembly/dist.xml_) which is very specific and might break with the inclusion of new dependencies.

   Especially check the jar files in the subdirectories:
   - optional
   - required
   - provided

### Release

1. [Release the version on JIRA](https://hibernate.atlassian.net/plugins/servlet/project-config/OGM/versions)

2. Do **NOT** update _changelog.txt_ in project root: it will be automatically updated

3. Verify _readme.txt_:
   - content is up to date
   - links are not broken
   - don't change the version number, it will be automatically updated

4. Commit any outstanding changes

5. Go to CI and execute the [release job](http://ci.hibernate.org/view/OGM/job/hibernate-ogm-release/).
   - **Be careful when filling the form with the build parameters.**

6. Log in to [Nexus](https://repository.jboss.org/nexus):
   - check all artifacts you expect are there
   - close and release the repository. See [more details about using the staging repository](https://community.jboss.org/wiki/MavenDeployingARelease)
   - if there is a problem, drop the staging repo, fix the problem and re-deploy

### Publish

1. The CI job automatically pushes the distribution to SourceForge and publishes the documentation to docs.jboss.org.

2. Update the [community pages](http://community.jboss.org/en/hibernate/ogm).
   In particular, update the [migration notes](https://community.jboss.org/wiki/HibernateOGMMigrationNotes).
   When doing the latter, create an API change report by running `mvn clirr:clirr -pl core`.
   The report created at _core/target/site/clirr-report.html_ provides an overview of all changed API/SPI types.
   After you have created the change log, don't forget to update the _comparisonVersion_ in the configuration of the Maven Clirr plug-in in _pom.xml_.

### Announce

1. Blog about the release on [in.relation.to](http://in.relation.to/), make sure to use the tags **Hibernate OGM**, **Hibernate** and **news** for the blog entry.
   This way the blog will be featured on the [web-site](http://www.hibernate.org/ogm) and on the JBoss blog federation.

2. Update [hibernate.org](http://hibernate.org/) by adding a new release file to _data/projects/ogm/releases_
   and by updating the roadmap in _ogm/roadmap.adoc_
   If you don't want to display an older release, set the property _displayed_ to false in the corresponding .yml file.
   - If it is a new major release, add a _data/projects/ogm/releases/series.yml_ file and an _ogm/releases/<version>/index.adoc_ file
   - Add a new release file to _data/projects/ogm/releases_
   - Remember to add a one line summary using the property _summary_
   - Depending on which series you want to have displayed, make sure to adjust the displayed flag of the series.yml file of the old series
   - Deploy to production

3. Check the getting started guide on the website. It is mostly copied from the reference documentation with a few twists.
   Likewise, check the roadmap, move the new released to the previous section and adjust the roadmap as needed.
   When ready, deploy everything on production.

   Check:
   - http://www.hibernate.org/ogm/download
   - http://www.hibernate.org/ogm/documentation
   - http://www.hibernate.org/ogm/roadmap
 
4. Update the sticky post in the forum: https://discourse.hibernate.org/t/latest-versions-5-1-0-final-stable-5-2-0-alpha1/25

5. Send email to _hibernate-dev_ and _hibernate-announce_.
   A quick sum up paragraph in the email is necessary before pointing to the blog entry.

6. Twitter
